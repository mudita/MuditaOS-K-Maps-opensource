package net.osmand.plus;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mudita.map.common.enums.LocationSource;
import com.mudita.maps.R;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.plus.helpers.LocationServiceHelper;
import net.osmand.plus.helpers.LocationServiceHelper.LocationCallback;
import net.osmand.plus.notifications.OsmandNotification;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.List;

public class NavigationService extends Service {

	public static class NavigationServiceBinder extends Binder {
	}

	public static final String DEEP_LINK_ACTION_OPEN_ROOT_SCREEN = "net.osmand.plus.navigation.car.OpenRootScreen";

	// global id don't conflict with others
	public static int USED_BY_NAVIGATION = 1;
	public static int USED_BY_GPX = 2;
	public static int USED_BY_CAR_APP = 4;
	public static final String USAGE_INTENT = "SERVICE_USED_BY";

	private final NavigationServiceBinder binder = new NavigationServiceBinder();

	private OsmandSettings settings;

	protected int usedBy;
	private OsmAndLocationProvider locationProvider;
	private LocationServiceHelper locationServiceHelper;
	private StateChangedListener<LocationSource> locationSourceListener;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public int getUsedBy() {
		return usedBy;
	}

	public boolean isUsed() {
		return usedBy != 0;
	}

	public void stopIfNeeded(Context ctx, int usageIntent) {
		if ((usedBy & usageIntent) > 0) {
			usedBy -= usageIntent;
		}
		if (usedBy == 0) {
			Intent serviceIntent = new Intent(ctx, NavigationService.class);
			ctx.stopService(serviceIntent);
		} else {
			OsmandApplication app = getApp();
			app.getNotificationHelper().updateTopNotification();
			app.getNotificationHelper().refreshNotifications();
		}
	}

	private OsmandApplication getApp() {
		return (OsmandApplication) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		OsmandApplication app = getApp();
		settings = app.getSettings();
		usedBy = intent.getIntExtra(USAGE_INTENT, 0);

		locationProvider = app.getLocationProvider();
		locationServiceHelper = app.createLocationServiceHelper();
		app.setNavigationService(this);

		Notification notification = app.getNotificationHelper().buildTopNotification();
		if (notification != null) {
			startForeground(OsmandNotification.TOP_NOTIFICATION_SERVICE_ID, notification);
			app.getNotificationHelper().refreshNotifications();
		} else {
			notification = app.getNotificationHelper().buildErrorNotification();
			startForeground(OsmandNotification.TOP_NOTIFICATION_SERVICE_ID, notification);
			stopSelf();
			return START_NOT_STICKY;
		}
		requestLocationUpdates();

		return START_REDELIVER_INTENT;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		addLocationSourceListener();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		OsmandApplication app = getApp();
		app.setNavigationService(null);
		usedBy = 0;
		removeLocationUpdates();
		removeLocationSourceListener();

		// remove notification
		stopForeground(Boolean.TRUE);
		app.getNotificationHelper().updateTopNotification();
		app.runInUIThread(() -> app.getNotificationHelper().refreshNotifications(), 500);
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		OsmandApplication app = getApp();
		app.getNotificationHelper().removeNotifications(false);
		if (app.getNavigationService() != null &&
				app.getSettings().DISABLE_RECORDING_ONCE_APP_KILLED.get()) {
			stopSelf();
		}
	}

	private void addLocationSourceListener() {
		OsmandApplication app = getApp();
		locationSourceListener = change -> {
			removeLocationUpdates();
			locationServiceHelper = app.createLocationServiceHelper();
			requestLocationUpdates();
		};
		app.getSettings().LOCATION_SOURCE.addListener(locationSourceListener);
	}

	private void removeLocationSourceListener() {
		getApp().getSettings().LOCATION_SOURCE.removeListener(locationSourceListener);
	}

	private void requestLocationUpdates() {
		try {
			locationServiceHelper.requestLocationUpdates(new LocationCallback() {
				@Override
				public void onLocationResult(@NonNull List<net.osmand.Location> locations) {
					if (!locations.isEmpty()) {
						Location location = locations.get(locations.size() - 1);
						if (!settings.MAP_ACTIVITY_ENABLED.get()) {
							locationProvider.setLocationFromService(location);
						}
					}
				}

				@Override
				public void onLocationAvailability(boolean locationAvailable) {
				}
			});
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show();
		}
	}

	private void removeLocationUpdates() {
		if (locationServiceHelper != null) {
			try {
				locationServiceHelper.removeLocationUpdates();
			} catch (SecurityException e) {
				// Location service permission not granted
			}
		}
	}
}
