package net.osmand.plus.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mudita.maps.R;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.mapmarkers.MarkersPlanRouteContext;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.GeneralRouter;
import net.osmand.util.MapUtils;

import java.util.List;

public class MapActions {

	public static final int START_TRACK_POINT_MY_LOCATION_RADIUS_METERS = 50 * 1000;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public MapActions(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public boolean hasUiContext() {
		return false;
	}

	public void setGPXRouteParams(@Nullable GPXFile result) {
		if (result == null) {
			app.getRoutingHelper().setGpxParams(null);
			settings.FOLLOW_THE_GPX_ROUTE.set(null);
		} else {
			GPXRouteParamsBuilder params = new GPXRouteParamsBuilder(result, settings);
			params.setCalculateOsmAndRouteParts(settings.GPX_ROUTE_CALC_OSMAND_PARTS.get());
			params.setCalculateOsmAndRoute(settings.GPX_ROUTE_CALC.get());
			params.setSelectedSegment(settings.GPX_SEGMENT_INDEX.get());
			params.setSelectedRoute(settings.GPX_ROUTE_INDEX.get());
			List<Location> ps = params.getPoints(settings.getContext());
			app.getRoutingHelper().setGpxParams(params);
			settings.FOLLOW_THE_GPX_ROUTE.set(result.path);
			if (!ps.isEmpty()) {
				Location startLoc = ps.get(0);
				Location finishLoc = ps.get(ps.size() - 1);
				Location location = app.getLocationProvider().getLastKnownLocation();
				TargetPointsHelper pointsHelper = app.getTargetPointsHelper();
				pointsHelper.clearAllIntermediatePoints(false);
				if (location == null || MapUtils.getDistance(location, startLoc) <= START_TRACK_POINT_MY_LOCATION_RADIUS_METERS) {
					pointsHelper.clearStartPoint(false);
				} else {
					pointsHelper.setStartPoint(new LatLon(startLoc.getLatitude(), startLoc.getLongitude()), false, null);
				}
				pointsHelper.navigateToPoint(new LatLon(finishLoc.getLatitude(), finishLoc.getLongitude()), false, -1);
			}
		}
	}

	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, ApplicationMode appMode, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu, int menuState) {
		TargetPointsHelper targets = app.getTargetPointsHelper();

		if (gpxFile != null && gpxFile.hasRtePt() && appMode == null) {
			GPXUtilities.WptPt routePoint = gpxFile.getRoutePoints().get(0);
			ApplicationMode routePointAppMode = ApplicationMode.valueOfStringKey(routePoint.getProfileType(), ApplicationMode.DEFAULT);
			if (routePointAppMode != ApplicationMode.DEFAULT) {
				appMode = routePointAppMode;
			}
		}
		ApplicationMode mode = appMode != null ? appMode : getRouteMode();
		app.getSettings().setApplicationMode(mode, false);
		app.getRoutingHelper().setAppMode(mode);
		initVoiceCommandPlayer(mode, showMenu);
		// save application mode controls
		settings.FOLLOW_THE_ROUTE.set(false);
		app.getRoutingHelper().setFollowingMode(false);
		app.getRoutingHelper().setRoutePlanningMode(true);
		// reset start point
		targets.setStartPoint(from, false, fromName);
		// then set gpx
		setGPXRouteParams(gpxFile);
		// then update start and destination point
		targets.updateRouteAndRefresh(true);

		app.getMapViewTrackingUtilities().switchRoutePlanningMode();
		app.getOsmandMap().refreshMap(true);

		if (targets.hasTooLongDistanceToNavigate()) {
			app.showToastMessage(R.string.route_is_too_long_v2);
		}
	}

	protected void initVoiceCommandPlayer(@NonNull ApplicationMode mode, boolean showMenu) {
		app.initVoiceCommandPlayer(app, mode, null, true, false, false, showMenu);
	}

	public ApplicationMode getRouteMode() {
		MarkersPlanRouteContext planRouteContext = app.getMapMarkersHelper().getPlanRouteContext();
		if (planRouteContext.isNavigationFromMarkers() && planRouteContext.getSnappedMode() != ApplicationMode.DEFAULT) {
			planRouteContext.setNavigationFromMarkers(false);
			return planRouteContext.getSnappedMode();
		}
		ApplicationMode mode = settings.DEFAULT_APPLICATION_MODE.get();
		ApplicationMode selected = settings.APPLICATION_MODE.get();
		if (selected != ApplicationMode.DEFAULT) {
			mode = selected;
		} else if (mode == ApplicationMode.DEFAULT) {
			for (ApplicationMode appMode : ApplicationMode.values(app)) {
				if (appMode != ApplicationMode.DEFAULT) {
					mode = appMode;
					break;
				}
			}
			if (settings.LAST_ROUTING_APPLICATION_MODE != null &&
					settings.LAST_ROUTING_APPLICATION_MODE != ApplicationMode.DEFAULT) {
				mode = settings.LAST_ROUTING_APPLICATION_MODE;
			}
		}
		return mode;
	}

	public void stopNavigationWithoutConfirm() {
		app.stopNavigation();
		List<ApplicationMode> modes = ApplicationMode.values(app);
		for (ApplicationMode mode : modes) {
			if (settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.getModeValue(mode)) {
				settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.setModeValue(mode, false);
				settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE, false).setModeValue(mode, false);
				settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK, false).setModeValue(mode, false);
			}
		}
	}
}
