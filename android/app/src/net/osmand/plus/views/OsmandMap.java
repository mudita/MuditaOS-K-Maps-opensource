package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class OsmandMap {

	private final OsmandApplication app;

	private final MapViewTrackingUtilities mapViewTrackingUtilities;
	private final OsmandMapTileView mapView;
	private final MapLayers mapLayers;
	private final MapActions mapActions;
	private final IMapDownloaderCallback downloaderCallback;

	private List<OsmandMapListener> listeners = new ArrayList<>();

	public interface OsmandMapListener {
		void onChangeZoom(int zoomLevel);

		void onSetMapElevation(float angle);

		void onSetupRenderingView();
	}

	public void addListener(@NonNull OsmandMapListener listener) {
		if (!listeners.contains(listener)) {
			listeners = Algorithms.addToList(listeners, listener);
		}
	}

	public void removeListener(@NonNull OsmandMapListener listener) {
		listeners = Algorithms.removeFromList(listeners, listener);
	}

	public OsmandMap(@NonNull OsmandApplication app) {
		this.app = app;
		mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
		mapActions = new MapActions(app);

		WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point screenDimensions = new Point(0, 0);
		display.getSize(screenDimensions);
		int w = screenDimensions.x;
		int h = screenDimensions.y - AndroidUtils.getStatusBarHeight(app);

		mapView = new OsmandMapTileView(app, w, h);
		mapLayers = new MapLayers(app);

		// to not let it gc
		downloaderCallback = request -> {
			if (request != null && !request.error && request.fileToSave != null) {
				ResourceManager mgr = app.getResourceManager();
				mgr.tileDownloaded(request);
			}
			if (request == null || !request.error) {
				mapView.tileDownloaded(request);
			}
		};
		app.getResourceManager().getMapTileDownloader().addDownloaderCallback(downloaderCallback);
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public OsmandMapTileView getMapView() {
		return mapView;
	}

	@NonNull
	public MapLayers getMapLayers() {
		return mapLayers;
	}

	@NonNull
	public MapActions getMapActions() {
		return mapActions;
	}

	public void refreshMap() {
		mapView.refreshMap();
	}

	public void refreshMap(boolean updateVectorRendering) {
		mapView.refreshMap(updateVectorRendering);
	}

	public void changeZoom(int step, long time) {
		mapViewTrackingUtilities.setZoomTime(time);
		changeZoom(step);
	}

	public void changeZoom(int step) {
		int newZoom = mapView.getZoom() + step;
		changeZoomLevel(newZoom);
	}

	public void changeZoomLevel(int zoomLevel) {
		if (mapView.getZoomFractionalPart() == null || mapView.getAnimatedDraggingThread() == null) return;

		if (zoomLevel > mapView.getMaxZoom()) {
			Timber.d("Maximum zoom reached.");
			return;
		}
		if (zoomLevel < mapView.getMinZoom()) {
			Timber.d("Minimum zoom reached.");
			return;
		}

		double zoomFrac = mapView.getZoomFractionalPart();
		mapView.getAnimatedDraggingThread().startZooming(zoomLevel, zoomFrac, false);

		for (OsmandMapListener listener : listeners) {
			listener.onChangeZoom(zoomLevel);
		}
	}

	public void setMapLocation(double lat, double lon) {
		mapView.setLatLon(lat, lon);
		mapViewTrackingUtilities.locationChanged(lat, lon, this);
	}

	public void setMapElevation(float angle) {
		for (OsmandMapListener listener : listeners) {
			listener.onSetMapElevation(angle);
		}
	}

	public void setupRenderingView() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (OsmandMapListener listener : listeners) {
			listener.onSetupRenderingView();
		}
		if (mapView.getMapActivity() == null) {
			app.getMapViewTrackingUtilities().setMapView(null);
		}
	}

	public float getTextScale() {
		float scale = app.getSettings().TEXT_SCALE.get();
		return scale;
	}

	public float getOriginalTextScale() {
		return app.getSettings().TEXT_SCALE.get();
	}

	public float getMapDensity() {
		float scale = app.getSettings().MAP_DENSITY.get();
		return scale;
	}

	public void fitCurrentRouteToMap(boolean portrait, int leftBottomPaddingPx) {
		RoutingHelper rh = app.getRoutingHelper();
		Location lt = rh.getLastProjection();
		if (lt == null) {
			lt = app.getTargetPointsHelper().getPointToStartLocation();
		}
		if (lt != null) {
			double left = lt.getLongitude(), right = lt.getLongitude();
			double top = lt.getLatitude(), bottom = lt.getLatitude();
			List<Location> list = rh.getCurrentCalculatedRoute();
			for (Location l : list) {
				left = Math.min(left, l.getLongitude());
				right = Math.max(right, l.getLongitude());
				top = Math.max(top, l.getLatitude());
				bottom = Math.min(bottom, l.getLatitude());
			}
			List<TargetPointsHelper.TargetPoint> targetPoints = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
			if (rh.getRoute().hasMissingMaps()) {
				TargetPointsHelper.TargetPoint pointToStart = app.getTargetPointsHelper().getPointToStart();
				if (pointToStart != null) {
					targetPoints.add(pointToStart);
				}
			}
			for (TargetPointsHelper.TargetPoint l : targetPoints) {
				left = Math.min(left, l.getLongitude());
				right = Math.max(right, l.getLongitude());
				top = Math.max(top, l.getLatitude());
				bottom = Math.min(bottom, l.getLatitude());
			}
			RotatedTileBox tb = getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;
			if (!portrait) {
				tileBoxWidthPx = tb.getPixWidth() - leftBottomPaddingPx;
			} else {
				tileBoxHeightPx = tb.getPixHeight() - leftBottomPaddingPx;
			}
			getMapView().fitRectToMap(left, right, top, bottom, tileBoxWidthPx, tileBoxHeightPx, 0);
		}
	}
}
