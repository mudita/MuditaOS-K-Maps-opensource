package net.osmand.plus.dashboard;

import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.CONFIGURE_MAP;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.CYCLE_ROUTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.DASHBOARD;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.HIKING_ROUTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.LIST_MENU;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.MAPILLARY;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.MTB_ROUTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.NAUTICAL_DEPTH;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.OVERLAY_MAP;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.ROUTE_PREFERENCES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.TERRAIN;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.TRANSPORT_LINES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.TRAVEL_ROUTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.UNDERLAY_MAP;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.WEATHER;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.WEATHER_CONTOURS;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.WEATHER_LAYER;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.WIKIPEDIA;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.mudita.maps.R;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.RasterMapMenu;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.DownloadedRegionsLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DashboardOnMap implements ObservableScrollViewCallbacks, IRouteInformationListener {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(DashboardOnMap.class);
	private static final String TAG = "DashboardOnMap";

	public static boolean staticVisible;
	public static DashboardType staticVisibleType = DASHBOARD;
	public static final String SHOULD_SHOW = "should_show";

	private final MapActivity mapActivity;
	private ImageView actionButton;
	private View compassButton;

	private boolean visible;
	private DashboardVisibilityStack visibleTypes = new DashboardVisibilityStack();
	private ApplicationMode previousAppMode;
	private final List<WeakReference<DashBaseFragment>> fragList = new LinkedList<>();
	private LatLon mapViewLocation;
	private float heading;
	private boolean mapLinkedToLocation;
	private boolean inLocationUpdate;
	private View listBackgroundView;
	private View paddingView;
	private int mFlexibleSpaceImageHeight;
	private int mFlexibleBlurSpaceHeight;
	private boolean portrait;

	int baseColor;
	boolean nightMode;

	public enum DashboardType {
		CONFIGURE_MAP,
		LIST_MENU,
		ROUTE_PREFERENCES,
		DASHBOARD,
		OVERLAY_MAP,
		UNDERLAY_MAP,
		MAPILLARY,
		CONTOUR_LINES,
		OSM_NOTES,
		WIKIPEDIA,
		TERRAIN,
		CYCLE_ROUTES,
		HIKING_ROUTES,
		TRAVEL_ROUTES,
		TRANSPORT_LINES,
		WEATHER,
		WEATHER_LAYER,
		WEATHER_CONTOURS,
		NAUTICAL_DEPTH,
		MTB_ROUTES
	}

	private final Map<DashboardActionButtonType, DashboardActionButton> actionButtons = new HashMap<>();

	public enum DashboardActionButtonType {
		MY_LOCATION,
		NAVIGATE,
		ROUTE
	}

	private class DashboardActionButton {
		private Drawable icon;
		private String text;
		private View.OnClickListener onClickListener;
	}

	public DashboardOnMap(MapActivity ma) {
		this.mapActivity = ma;
	}

	private void updateListBackgroundHeight() {
		if (listBackgroundView != null) {
			View contentView = mapActivity.getWindow().getDecorView().findViewById(android.R.id.content);
			ViewTreeObserver vto = contentView.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					ViewTreeObserver obs = contentView.getViewTreeObserver();
					obs.removeOnGlobalLayoutListener(this);
					listBackgroundView.getLayoutParams().height = contentView.getHeight();
				}
			});
		}
	}

	private void setActionButton(DashboardType type) {
		DashboardActionButton button = null;

		if (type == DASHBOARD || type == LIST_MENU) {
			button = actionButtons.get(DashboardActionButtonType.MY_LOCATION);
		} else if (type == ROUTE_PREFERENCES) {
			button = actionButtons.get(DashboardActionButtonType.NAVIGATE);
		}

		if (button != null) {
			actionButton.setImageDrawable(button.icon);
			actionButton.setContentDescription(button.text);
			actionButton.setOnClickListener(button.onClickListener);
		}
	}

	private void hideActionButton() {
		actionButton.setVisibility(View.GONE);
		if (compassButton != null) {
			compassButton.setVisibility(View.GONE);
		}
	}

	public LatLon getMapViewLocation() {
		return mapViewLocation;
	}

	public float getHeading() {
		return heading;
	}

	public boolean isMapLinkedToLocation() {
		return mapLinkedToLocation;
	}

	protected OsmandApplication getMyApplication() {
		return mapActivity.getMyApplication();
	}

	public void hideDashboard() {
		setDashboardVisibility(false, visibleTypes.getCurrent());
	}

	public void hideDashboard(boolean animation) {
		setDashboardVisibility(false, visibleTypes.getCurrent(), animation);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type) {
		setDashboardVisibility(visible, type, null);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, int[] animationCoordinates) {
		boolean animate = !getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get();
		setDashboardVisibility(visible, type, animate, animationCoordinates);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, boolean animation) {
		setDashboardVisibility(visible, type, animation, null);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, boolean animation, int[] animationCoordinates) {
		boolean currentType = isCurrentType(type);
		if (visible == this.visible && currentType || !AndroidUtils.isActivityNotDestroyed(mapActivity)) {
			return;
		}
		mapActivity.getRoutingHelper().removeListener(this);
		this.visible = visible;
		updateVisibilityStack(type, visible);
		ApplicationMode currentAppMode = getMyApplication().getSettings().APPLICATION_MODE.get();
		boolean appModeChanged = currentAppMode != previousAppMode;

		boolean refresh = currentType && !appModeChanged;
		previousAppMode = currentAppMode;
		staticVisible = visible;
		staticVisibleType = type;

		if (visible) {
			mapViewLocation = mapActivity.getMapLocation();
			mapLinkedToLocation = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
			mapActivity.getMapViewTrackingUtilities().setDashboard(this);
			if (isActionButtonVisible()) {
				setActionButton(visibleTypes.getCurrent());
				actionButton.setVisibility(View.VISIBLE);
			} else {
				hideActionButton();

			}
			updateDownloadBtn();
			if (!isCurrentType(DASHBOARD, CONFIGURE_MAP, MAPILLARY, CYCLE_ROUTES, HIKING_ROUTES,
					TRAVEL_ROUTES, TRANSPORT_LINES, TERRAIN, WEATHER, WEATHER_LAYER, WEATHER_CONTOURS, NAUTICAL_DEPTH, MTB_ROUTES)) {
				if (refresh) {
					refreshContent(false);
				} else {
					updateListAdapter();
				}
				updateListBackgroundHeight();
			}
			mapActivity.findViewById(R.id.toolbar_back).setVisibility(isBackButtonVisible() ? View.VISIBLE : View.GONE);

			updateLocation(true, true, false);
			mapActivity.getRoutingHelper().addListener(this);
		} else {
			mapActivity.getMapViewTrackingUtilities().setDashboard(null);
			hideActionButton();
			for (WeakReference<DashBaseFragment> df : fragList) {
				if (df.get() != null) {
					df.get().onCloseDash();
				}
			}
		}
	}

	private void updateVisibilityStack(@NonNull DashboardType type, boolean visible) {
		if (visible) {
			visibleTypes.add(type);
		} else {
			visibleTypes.clear();
		}
	}

	private void updateListAdapter() {
		ContextMenuAdapter cm = null;
		if (isCurrentType(UNDERLAY_MAP)) {
			cm = RasterMapMenu.createListAdapter(mapActivity, OsmandRasterMapsPlugin.RasterMapType.UNDERLAY);
		} else if (isCurrentType(OVERLAY_MAP)) {
			cm = RasterMapMenu.createListAdapter(mapActivity, OsmandRasterMapsPlugin.RasterMapType.OVERLAY);
		}
		if (cm != null) {
			updateListAdapter(cm);
		}
	}

	public void updateListAdapter(ContextMenuAdapter cm) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = settings.getApplicationMode();

		ViewCreator viewCreator = new ViewCreator(mapActivity, nightMode);
		int profileColor = appMode.getProfileColor(nightMode);
		if (isCurrentType(WIKIPEDIA)) {
			viewCreator.setDefaultLayoutId(R.layout.dash_item_with_description_72dp);
			viewCreator.setCustomControlsColor(profileColor);
		} else if (isNoCurrentType(LIST_MENU)) {
			viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
			viewCreator.setCustomControlsColor(profileColor);
		}
	}

	public void refreshContent(boolean force) {
		if (force) {
			updateListAdapter();
		} else if (isCurrentType(ROUTE_PREFERENCES)) {
			updateListAdapter();
		}
	}

	private void updateDownloadBtn() {
		String filter = null;
		String txt = "";
		OsmandMapTileView mv = mapActivity.getMapView();
		if (!mapActivity.getMyApplication().isApplicationInitializing()) {
			if (mv.getZoom() < 11 && !mapActivity.getMyApplication().getResourceManager().containsBasemap()) {
				filter = "basemap";
				txt = mapActivity.getString(R.string.shared_string_download) + " "
						+ mapActivity.getString(R.string.base_world_map);
			} else {
				DownloadedRegionsLayer dl = mv.getLayerByClass(DownloadedRegionsLayer.class);
				if (dl != null) {
					StringBuilder btnName = new StringBuilder();
					filter = dl.getFilter(btnName);
					txt = btnName.toString();
				}
			}
		}
		scheduleDownloadButtonCheck();
	}

	private void scheduleDownloadButtonCheck() {
		mapActivity.getMyApplication().runInUIThread(() -> {
			if (isVisible()) {
				updateDownloadBtn();
			}
		}, 4000);
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isCurrentType(@NonNull DashboardType... types) {
		for (DashboardType type : types) {
			if (visibleTypes.getCurrent() == type) {
				return true;
			}
		}
		return false;
	}

	public boolean isNoCurrentType(@NonNull DashboardType... types) {
		return !isCurrentType(types);
	}

	void onDetach(DashBaseFragment dashBaseFragment) {
		Iterator<WeakReference<DashBaseFragment>> it = fragList.iterator();
		while (it.hasNext()) {
			WeakReference<DashBaseFragment> wr = it.next();
			if (wr.get() == dashBaseFragment) {
				it.remove();
			}
		}
	}

	public void onAppModeChanged() {
		if (isCurrentType(CONFIGURE_MAP)) {
			refreshContent(false);
		}
	}

	public void updateLocation(boolean centerChanged, boolean locationChanged,
	                           boolean compassChanged) {
		if (inLocationUpdate) {
			return;
		}
		inLocationUpdate = true;
		mapActivity.runOnUiThread(() -> {
			inLocationUpdate = false;
			for (WeakReference<DashBaseFragment> df : fragList) {
				if (df.get() instanceof DashLocationFragment) {
					((DashLocationFragment) df.get()).updateLocation(centerChanged, locationChanged, compassChanged);
				}
			}
		});

	}

	public void updateMyLocation(net.osmand.Location location) {
		updateLocation(false, true, false);
	}

	public void updateCompassValue(double heading) {
		this.heading = (float) heading;
		updateLocation(false, false, true);
	}

	public void onAttach(DashBaseFragment dashBaseFragment) {
		fragList.add(new WeakReference<>(dashBaseFragment));
	}

	public boolean onBackPressed() {
		if (isVisible()) {
			backPressed();
			return true;
		}
		return false;
	}

	private void backPressed() {
		DashboardType previous = visibleTypes.getPrevious();
		if (previous != null) {
			if (isCurrentType(MAPILLARY)) {
				hideKeyboard();
			}
			visibleTypes.pop(); // Remove current visible type.
			visibleTypes.pop(); // Also remove previous type. It will be add later.
			setDashboardVisibility(true, previous);
		} else {
			hideDashboard();
		}
	}

	private void hideKeyboard() {
		View currentFocus = mapActivity.getCurrentFocus();
		if (currentFocus != null) {
			InputMethodManager imm = (InputMethodManager) mapActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
			}
		}
	}

	@Override
	public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
		// Translate list background
		if (portrait) {
			if (listBackgroundView != null) {
				setTranslationY(listBackgroundView, Math.max(0, -scrollY + mFlexibleSpaceImageHeight));
			}
		}

		updateColorOfToolbar(scrollY);
		updateTopButton(scrollY);
	}

	private boolean isActionButtonVisible() {
		return isCurrentType(DASHBOARD, LIST_MENU, ROUTE_PREFERENCES);
	}

	private boolean isBackButtonVisible() {
		return isNoCurrentType(DASHBOARD, LIST_MENU);
	}

	private void updateTopButton(int scrollY) {
		if (actionButton != null && portrait && isActionButtonVisible()) {
			double scale = mapActivity.getResources().getDisplayMetrics().density;
			int originalPosition = mFlexibleSpaceImageHeight - (int) (80 * scale);
			int minTop = mFlexibleBlurSpaceHeight + (int) (5 * scale);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) actionButton.getLayoutParams();
			if (minTop > originalPosition - scrollY) {
				hideActionButton();
			} else {
				actionButton.setVisibility(View.VISIBLE);
				lp.topMargin = originalPosition - scrollY;
				((FrameLayout) actionButton.getParent()).updateViewLayout(actionButton, lp);
			}
		} else if (compassButton != null) {
			double scale = mapActivity.getResources().getDisplayMetrics().density;
			int originalPosition = mFlexibleSpaceImageHeight - (int) (64 * scale);
			int minTop = mFlexibleBlurSpaceHeight + (int) (5 * scale);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) compassButton.getLayoutParams();
			if (minTop > originalPosition - scrollY) {
				hideActionButton();
			} else {
				compassButton.setVisibility(View.VISIBLE);
				lp.topMargin = originalPosition - scrollY;
				((FrameLayout) compassButton.getParent()).updateViewLayout(compassButton, lp);
			}
		}
	}

	private void updateColorOfToolbar(int scrollY) {
		if (portrait) {
			float sh = mFlexibleSpaceImageHeight - mFlexibleBlurSpaceHeight;
			float t = sh == 0 ? 1 : (1 - Math.max(0, -scrollY + sh) / sh);
			t = Math.max(0, t);

			int alpha = (int) (t * 255);
			// in order to have proper fast scroll down
			int malpha = t == 1 ? 0 : alpha;
			setAlpha(paddingView, malpha, baseColor);
		}
	}

	private void setTranslationY(View v, int y) {
		v.setTranslationY(y);
	}

	@SuppressLint("NewApi")
	private void setAlpha(View v, int alpha, int color) {
		v.setBackgroundColor((alpha << 24) | color);
	}

	@Override
	public void onDownMotionEvent() {
	}

	@Override
	public void onUpOrCancelMotionEvent(ScrollState scrollState) {
	}

	<T extends DashBaseFragment> T getFragmentByClass(Class<T> class1) {
		for (WeakReference<DashBaseFragment> f : fragList) {
			DashBaseFragment b = f.get();
			if (b != null && !b.isDetached() && class1.isInstance(b)) {
				//noinspection unchecked
				return (T) b;
			}
		}
		return null;
	}

	void blacklistFragmentByTag(String tag) {
		hideFragmentByTag(tag);
		getMyApplication().getSettings().registerBooleanPreference(SHOULD_SHOW + tag, true)
				.makeGlobal().set(false);
	}

	void hideFragmentByTag(String tag) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		Fragment fragment = manager.findFragmentByTag(tag);
		if (fragment != null) {
			manager.beginTransaction()
					.hide(fragment)
					.commitAllowingStateLoss();
		}
	}

	void unblacklistFragmentClass(String tag) {
		unhideFragmentByTag(tag);
		getMyApplication().getSettings().registerBooleanPreference(SHOULD_SHOW + tag, true)
				.makeGlobal().set(true);
	}

	void unhideFragmentByTag(String tag) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		Fragment fragment = manager.findFragmentByTag(tag);
		if (fragment != null) {
			manager.beginTransaction()
					.show(fragment)
					.commitAllowingStateLoss();
		}
	}


	public static <T> void handleNumberOfRows(List<T> list, OsmandSettings settings,
	                                          String rowNumberTag) {
		int numberOfRows = settings.registerIntPreference(rowNumberTag, 3)
				.makeGlobal().get();
		if (list.size() > numberOfRows) {
			while (list.size() != numberOfRows) {
				list.remove(numberOfRows);
			}
		}
	}

	public static class DefaultShouldShow extends DashFragmentData.ShouldShowFunction {
		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			return settings.registerBooleanPreference(SHOULD_SHOW + tag, true).makeGlobal().get();
		}
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
	}

	@Override
	public void routeWasCancelled() {
	}

	@Override
	public void routeWasFinished() {
	}
}
