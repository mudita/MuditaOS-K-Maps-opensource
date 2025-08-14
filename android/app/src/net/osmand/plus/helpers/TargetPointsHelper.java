package net.osmand.plus.helpers;


import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.mudita.map.common.model.navigation.NavigationPoint;
import com.mudita.maps.R;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class TargetPointsHelper {

	private final OsmandApplication ctx;
	private OsmandSettings settings;
	private final RoutingHelper routingHelper;

	private final List<TargetPoint> intermediatePoints = new ArrayList<>();
	private TargetPoint pointToNavigate;
	private TargetPoint pointToStart;
	private TargetPoint pointToNavigateBackup;
	private TargetPoint pointToStartBackup;
	private TargetPoint myLocationToStart;
	private final List<StateChangedListener<Void>> listeners = new ArrayList<>();

	private AddressLookupRequest startPointRequest;
	private AddressLookupRequest targetPointRequest;

	public static class TargetPoint implements LocationPoint {
		public LatLon point;
		private final PointDescription pointDescription;
		public int index;
		public boolean intermediate;
		public boolean start;

		public TargetPoint(LatLon point, PointDescription name) {
			this.point = point;
			this.pointDescription = name;
		}

		public TargetPoint(LatLon point, PointDescription name, int index) {
			this.point = point;
			this.pointDescription = name;
			this.index = index;
			this.intermediate = true;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			TargetPoint targetPoint = (TargetPoint) o;

			if (start != targetPoint.start) return false;
			if (intermediate != targetPoint.intermediate) return false;
			if (index != targetPoint.index) return false;
			return point.equals(targetPoint.point);

		}

		@Override
		public int hashCode() {
			int result = point.hashCode();
			result = 31 * result + index;
			result = 31 * result + (start ? 10 : 20);
			result = 31 * result + (intermediate ? 100 : 200);
			return result;
		}

		@SuppressLint("StringFormatInvalid")
		public PointDescription getPointDescription(Context ctx) {
			if (!intermediate) {
				return new PointDescription(PointDescription.POINT_TYPE_TARGET, ctx.getString(R.string.destination_point, ""),
						getOnlyName());
			} else {
				return new PointDescription(PointDescription.POINT_TYPE_TARGET, (index + 1) + ". " + ctx.getString(R.string.intermediate_point, ""), 
						getOnlyName());
			}
		}
		
		public PointDescription getOriginalPointDescription() {
			return pointDescription;
		}
		
		public String getOnlyName() {
			return pointDescription == null ? "" : pointDescription.getName();
		}

		public boolean isSearchingAddress(Context ctx) {
			return pointDescription != null && pointDescription.isSearchingAddress(ctx);
		}

		public static TargetPoint create(LatLon point, PointDescription name) {
			if(point != null) {
				return new TargetPoint(point, name);
			}
			return null;
		}

		public static TargetPoint createStartPoint(LatLon point, PointDescription name) {
			if (point != null) {
				TargetPoint target = new TargetPoint(point, name);
				target.start = true;
				return target;
			}
			return null;
		}

		public double getLatitude() {
			return point.getLatitude();
		}
		
		public double getLongitude() {
			return point.getLongitude();
		}

		@Override
		public int getColor() {
			return 0;
		}

		@Override
		public boolean isVisible() {
			return false;
		}
		
	}

	public TargetPointsHelper(@NonNull OsmandApplication ctx){
		this.ctx = ctx;
		this.settings = ctx.getSettings();
		this.routingHelper = ctx.getRoutingHelper();
		readFromSettings();

		OsmAndAppCustomizationListener customizationListener = () -> {
			settings = this.ctx.getSettings();
			readFromSettings();
			updateRouteAndRefresh(true);
		};
		ctx.getAppCustomization().addListener(customizationListener);
	}

	private void readFromSettings() {
		pointToNavigate = TargetPoint.create(settings.getPointToNavigate(), settings.getPointNavigateDescription());
		pointToStart = TargetPoint.createStartPoint(settings.getPointToStart(), settings.getStartPointDescription());
		pointToNavigateBackup = TargetPoint.create(settings.getPointToNavigateBackup(), settings.getPointNavigateDescriptionBackup());
		pointToStartBackup = TargetPoint.createStartPoint(settings.getPointToStartBackup(), settings.getStartPointDescriptionBackup());
		myLocationToStart = TargetPoint.create(settings.getMyLocationToStart(), settings.getMyLocationToStartDescription());
		intermediatePoints.clear();
		List<LatLon> ips = settings.getIntermediatePoints();
		List<String> desc = settings.getIntermediatePointDescriptions(ips.size());
		for(int i = 0; i < ips.size(); i++) {
			TargetPoint targetPoint = new TargetPoint(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), i);
			intermediatePoints.add(targetPoint);
		}
	}

	private void readMyLocationPointFromSettings() {
		myLocationToStart = TargetPoint.create(settings.getMyLocationToStart(), settings.getMyLocationToStartDescription());
	}

	public void setPointToNavigate(TargetPoint pointToNavigate) {
		this.pointToNavigate = pointToNavigate;
	}

	public TargetPoint getPointToNavigate() {
		return pointToNavigate;
	}

	public void setPointToStart(TargetPoint pointToStart) {
		this.pointToStart = pointToStart;
	}

	public TargetPoint getPointToStart() {
		return pointToStart;
	}

	public TargetPoint getPointToNavigateBackup() {
		return pointToNavigateBackup;
	}

	public TargetPoint getPointToStartBackup() {
		return pointToStartBackup;
	}

	public TargetPoint getMyLocationToStart() {
		return myLocationToStart;
	}

	public void addIntermediatePoint(TargetPoint point) {
		if (point != null) {
			intermediatePoints.add(point);
		}
	}

    public void setIntermediatePoints(List<NavigationPoint> navigationPoints) {
        intermediatePoints.clear();
        for (int index = 0; index < navigationPoints.size(); index++) {
            NavigationPoint point = navigationPoints.get(index);
            LatLon latLon = point.getLatLon();
			if (latLon == null) continue;
            TargetPoint targetPoint = new TargetPoint(
                    latLon,
                    new PointDescription(latLon.getLatitude(), latLon.getLongitude())
            );
            intermediatePoints.add(targetPoint);
            navigateToPoint(latLon, false, index);
        }
    }

	public List<TargetPoint> getIntermediatePoints() {
		return new ArrayList<>(intermediatePoints);
	}
	
	public List<LatLon> getIntermediatePointsLatLon() {
		List<LatLon> intermediatePointsLatLon = new ArrayList<>();
		for (TargetPoint t : this.intermediatePoints) {
			intermediatePointsLatLon.add(t.point);
		}
		return intermediatePointsLatLon;
	}
	
	public List<LatLon> getIntermediatePointsLatLonNavigation() {
		List<LatLon> intermediatePointsLatLon = new ArrayList<>();
		for (TargetPoint t : this.intermediatePoints) {
            intermediatePointsLatLon.add(t.point);
        }
		return intermediatePointsLatLon;
	}

	public List<TargetPoint> getAllPoints() {
		List<TargetPoint> res = new ArrayList<>();
		if(pointToStart != null) {
			res.add(pointToStart);
		}
		res.addAll(this.intermediatePoints);
		if(pointToNavigate != null) {
			res.add(pointToNavigate);
		}
		return res;
	}

	@NonNull
	public List<TargetPoint> getIntermediatePointsWithTarget() {
		List<TargetPoint> res = new ArrayList<>(this.intermediatePoints);
		if (pointToNavigate != null) {
			res.add(pointToNavigate);
		}
		return res;
	}

	public TargetPoint getFirstIntermediatePoint(){
		if(intermediatePoints.size() > 0) {
			return intermediatePoints.get(0);
		}
		return null;
	}

	public void restoreTargetPoints(boolean updateRoute) {
		settings.restoreTargetPoints();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

		/**
		 * Clear the local and persistent waypoints list and destination.
		 */
	public void removeAllWayPoints(boolean updateRoute, boolean clearBackup){
		cancelStartPointAddressRequest();
		cancelTargetPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();

		settings.clearIntermediatePoints();
		settings.clearPointToNavigate();
		settings.clearPointToStart();
		if (clearBackup) {
			settings.backupTargetPoints();
		}
		updateMyLocationToStart();
		pointToNavigate = null;
		pointToStart = null;
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	/**
	 * Move an intermediate waypoint to the destination.
	 */
	public void makeWayPointDestination(boolean updateRoute, int index){
		TargetPoint targetPoint = intermediatePoints.remove(index);
		cancelTargetPointAddressRequest();
		cancelPointAddressRequests(targetPoint.point);

		pointToNavigate = targetPoint;
		settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
				pointToNavigate.pointDescription);
		pointToNavigate.intermediate = false;
		settings.deleteIntermediatePoint(index);

		updateRouteAndRefresh(updateRoute);
	}

	public void removeWayPoint(boolean updateRoute, int index) {
		int sz = intermediatePoints.size();
		if (index < 0) {
			cancelTargetPointAddressRequest();
			settings.clearPointToNavigate();
			pointToNavigate = null;
			if (sz > 0) {
				settings.deleteIntermediatePoint(sz - 1);
				pointToNavigate = intermediatePoints.remove(sz - 1);
				pointToNavigate.intermediate = false;
				settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
						pointToNavigate.pointDescription);
			}
		} else if (sz > index) {
			settings.deleteIntermediatePoint(index);
			TargetPoint targetPoint = intermediatePoints.remove(index);
			cancelPointAddressRequests(targetPoint.point);
			int ind = 0;
			for(TargetPoint tp : intermediatePoints) {
				tp.index = ind++;
			}
		}
		updateRouteAndRefresh(updateRoute);
	}

	public void updateRouteAndRefresh(boolean updateRoute) {
		if (updateRoute && (routingHelper.isPublicTransportMode() || routingHelper.isRouteBeingCalculated() ||
				routingHelper.isRouteCalculated() || routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode())) {
			updateRoutingHelper();
		}
		updateListeners();
	}

	public void updateMyLocationToStart() {
		if (pointToStart == null) {
			Location lastKnownLocation = ctx.getLocationProvider().getLastStaleKnownLocation();
			LatLon latLon = lastKnownLocation != null ?
					new LatLon(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()) : null;
			RoutingHelperUtils.checkAndUpdateStartLocation(ctx, latLon, false);
			setMyLocationPoint(latLon, false, null);
		}
	}

	private void updateRoutingHelper() {
		LatLon start = settings.getPointToStart();
		LatLon finish = settings.getPointToNavigate();
		List<LatLon> is = getIntermediatePointsLatLonNavigation();
		Location lastKnownLocation = ctx.getLocationProvider().getLastStaleKnownLocation();
		if ((routingHelper.isFollowingMode() && lastKnownLocation != null) || start == null) {
			routingHelper.setFinalAndCurrentLocation(finish, is, lastKnownLocation);
		} else {
			Location loc = wrap(start);
			routingHelper.setFinalAndCurrentLocation(finish, is, loc);
		}
	}

	private Location wrap(LatLon l) {
		if(l == null) {
			return null;
		}
		Location loc = new Location("map");
		loc.setLatitude(l.getLatitude());
		loc.setLongitude(l.getLongitude());
		return loc;
	}
	
	private Location wrap(TargetPoint l) {
		if(l == null) {
			return null;
		}
		Location loc = new Location("map");
		loc.setLatitude(l.getLatitude());
		loc.setLongitude(l.getLongitude());
		return loc;
	}

	public void addListener(StateChangedListener<Void> l) {
		listeners.add(l);
	}

	public void removeListener(StateChangedListener<Void> l) {
		listeners.remove(l);
	}

	private void updateListeners() {
		for(StateChangedListener<Void> l : listeners) {
			l.stateChanged(null);
		}
	}

	public void clearPointToNavigate(boolean updateRoute) {
		cancelTargetPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();
		settings.clearPointToNavigate();
		settings.clearIntermediatePoints();
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearStartPoint(boolean updateRoute) {
		cancelStartPointAddressRequest();
		settings.clearPointToStart();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearAllIntermediatePoints(boolean updateRoute) {
		cancelAllIntermediatePointsAddressRequests();
		settings.clearIntermediatePoints();
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearAllPoints(boolean updateRoute) {
		cancelStartPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();
		cancelTargetPointAddressRequest();
		settings.clearPointToStart();
		settings.clearIntermediatePoints();
		settings.clearPointToNavigate();
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearBackupPoints() {
		settings.clearPointToStartBackup();
		settings.clearIntermediatePointsBackup();
		settings.clearPointToNavigateBackup();
		readFromSettings();
	}

	public void reorderAllTargetPoints(List<TargetPoint> point, boolean updateRoute) {
		cancelTargetPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();
		settings.clearPointToNavigate();
		if (point.size() > 0) {
			List<TargetPoint> subList = point.subList(0, point.size() - 1);
			ArrayList<String> names = new ArrayList<>(subList.size());
			ArrayList<LatLon> ls = new ArrayList<>(subList.size());
			for (int i = 0; i < subList.size(); i++) {
				names.add(PointDescription.serializeToString(subList.get(i).pointDescription));
				ls.add(subList.get(i).point);
			}
			settings.saveIntermediatePoints(ls, names);
			TargetPoint p = point.get(point.size() - 1);
			settings.setPointToNavigate(p.getLatitude(), p.getLongitude(), p.pointDescription);
		} else {
			settings.clearIntermediatePoints();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void reorderIntermediatePoints(List<TargetPoint> points, boolean updateRoute) {
		cancelAllIntermediatePointsAddressRequests();
		if (points.size() > 0) {
			ArrayList<String> names = new ArrayList<>(points.size());
			ArrayList<LatLon> ls = new ArrayList<>(points.size());
			for (int i = 0; i < points.size(); i++) {
				names.add(PointDescription.serializeToString(points.get(i).pointDescription));
				ls.add(points.get(i).point);
			}
			settings.saveIntermediatePoints(ls, names);
		} else {
			settings.clearIntermediatePoints();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public boolean hasTooLongDistanceToNavigate() {
		if (routingHelper.getAppMode().getRouteService() != RouteService.OSMAND) {
			return false;
		}
		Location current = routingHelper.getLastProjection();
        double dist = 400000;
        if (ApplicationMode.BICYCLE.isDerivedRoutingFrom(routingHelper.getAppMode())
                && settings.getCustomRoutingBooleanProperty("height_obstacles", false).getModeValue(routingHelper.getAppMode())) { dist = 50000; }
		List<TargetPoint> list = getIntermediatePointsWithTarget();
		if(!list.isEmpty()) {
			if(current != null && MapUtils.getDistance(list.get(0).point, current.getLatitude(), current.getLongitude()) > dist) {
				return true;
			}
			for(int i = 1; i < list.size(); i++) {
				if(MapUtils.getDistance(list.get(i-1).point, list.get(i).point) > dist) {
					return true;
				}
			}
		}
		return false;
	}

	public void navigateToPoint(LatLon point, boolean updateRoute, int intermediate) {
		navigateToPoint(point, updateRoute, intermediate, null);
	}

	public void navigateToPoint(LatLon point, boolean updateRoute, int intermediate, PointDescription historyName) {
		if (point != null) {
			PointDescription pointDescription;
			if (historyName == null) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				pointDescription = historyName;
			}
			if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
				pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
			}

			if (intermediate < 0 || intermediate > intermediatePoints.size()) {
				if(intermediate > intermediatePoints.size()) {
					TargetPoint pn = getPointToNavigate();
					if(pn != null) {
						settings.insertIntermediatePoint(pn.getLatitude(), pn.getLongitude(), pn.pointDescription,
								intermediatePoints.size());
					}
				}
				settings.setPointToNavigate(point.getLatitude(), point.getLongitude(), pointDescription);
			} else {
				settings.insertIntermediatePoint(point.getLatitude(), point.getLongitude(), pointDescription,
						intermediate);
			}
		} else {
			cancelTargetPointAddressRequest();
			cancelAllIntermediatePointsAddressRequests();
			settings.clearPointToNavigate();
			settings.clearIntermediatePoints();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void setStartPoint(LatLon startPoint, boolean updateRoute, PointDescription name) {
		if (startPoint != null) {
			PointDescription pointDescription;
			if (name == null) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				pointDescription = name;
			}
			if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
				pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
			}
			settings.setPointToStart(startPoint.getLatitude(), startPoint.getLongitude(), pointDescription);
		} else {
			settings.clearPointToStart();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void setMyLocationPoint(LatLon startPoint, boolean updateRoute, PointDescription name) {
		if (startPoint != null) {
			PointDescription pointDescription;
			if (name == null) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				pointDescription = name;
			}
			if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
				pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
			}
			settings.setMyLocationToStart(startPoint.getLatitude(), startPoint.getLongitude(), pointDescription);
		} else {
			settings.clearMyLocationToStart();
		}
		readMyLocationPointFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public boolean checkPointToNavigateShort(){
    	if (pointToNavigate == null){
    		ctx.showShortToastMessage(R.string.mark_final_location_first);
			return false;
		}
    	return true;
    }

	public Location getPointToStartLocation() {
		return wrap(getPointToStart());
	}

	private void cancelStartPointAddressRequest() {
		if (startPointRequest != null) {
			ctx.getGeocodingLookupService().cancel(startPointRequest);
			startPointRequest = null;
		}
	}

	private void cancelTargetPointAddressRequest() {
		if (targetPointRequest != null) {
			ctx.getGeocodingLookupService().cancel(targetPointRequest);
			targetPointRequest = null;
		}
	}

	private void cancelAllIntermediatePointsAddressRequests() {
		List<LatLon> intermediatePointsLatLon = getIntermediatePointsLatLon();
		for (LatLon latLon : intermediatePointsLatLon) {
			cancelPointAddressRequests(latLon);
		}
	}

	private void cancelPointAddressRequests(LatLon latLon) {
		if (latLon != null) {
			ctx.getGeocodingLookupService().cancel(latLon);
		}
	}
}
