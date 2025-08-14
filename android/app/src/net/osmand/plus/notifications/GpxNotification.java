package net.osmand.plus.notifications;

import static net.osmand.plus.NavigationService.USED_BY_GPX;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;

import com.mudita.maps.R;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

public class GpxNotification extends OsmandNotification {

	public static final String OSMAND_SAVE_GPX_SERVICE_ACTION = "OSMAND_SAVE_GPX_SERVICE_ACTION";
	public static final String OSMAND_START_GPX_SERVICE_ACTION = "OSMAND_START_GPX_SERVICE_ACTION";
	public static final String OSMAND_STOP_GPX_SERVICE_ACTION = "OSMAND_STOP_GPX_SERVICE_ACTION";
	public static final String GROUP_NAME = "GPX";

	private boolean wasNoDataDismissed;
	private boolean lastBuiltNoData;

	public GpxNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
	}

	@Override
	public NotificationType getType() {
		return NotificationType.GPX;
	}

	@Override
	public int getPriority() {
		return NotificationCompat.PRIORITY_DEFAULT;
	}

	@Override
	public boolean isActive() {
		NavigationService service = app.getNavigationService();
		return isEnabled()
				&& service != null
				&& (service.getUsedBy() & USED_BY_GPX) != 0;
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public Intent getContentIntent() {
		return new Intent(app, MapActivity.class);
	}

	@Override
	public void onNotificationDismissed() {
		if (!wasNoDataDismissed) {
			wasNoDataDismissed = lastBuiltNoData;
		}
	}

	@Override
	public Builder buildNotification(boolean wearable) {
		if (!isEnabled()) {
			return null;
		}
		color = 0;
		icon = R.drawable.ic_notification_track;
		ongoing = true;
		lastBuiltNoData = false;

		if (app.getSavingTrackHelper().getTrkPoints() <= 0) {
			ongoing = false;
			lastBuiltNoData = true;
		}

		if ((wasNoDataDismissed || !app.getSettings().SHOW_TRIP_REC_NOTIFICATION.get()) && !ongoing) {
			return null;
		}

		Builder notificationBuilder = createBuilder(wearable);

		Intent saveIntent = new Intent(OSMAND_SAVE_GPX_SERVICE_ACTION);
		PendingIntent savePendingIntent = PendingIntent.getBroadcast(app, 0, saveIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		Intent startIntent = new Intent(OSMAND_START_GPX_SERVICE_ACTION);
		PendingIntent startPendingIntent = PendingIntent.getBroadcast(app, 0, startIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		if (app.getSavingTrackHelper().getTrkPoints() > 0) {
			notificationBuilder.addAction(R.drawable.ic_notification_rec_start,
					app.getString(R.string.shared_string_resume), startPendingIntent);
			notificationBuilder.addAction(R.drawable.ic_notification_save,
					app.getString(R.string.shared_string_save), savePendingIntent);
		} else {
			notificationBuilder.addAction(R.drawable.ic_notification_rec_start,
					app.getString(R.string.shared_string_record), startPendingIntent);
		}

		return notificationBuilder;
	}

	@Override
	public int getOsmandNotificationId() {
		return GPX_NOTIFICATION_SERVICE_ID;
	}

	@Override
	public int getOsmandWearableNotificationId() {
		return WEAR_GPX_NOTIFICATION_SERVICE_ID;
	}
}
