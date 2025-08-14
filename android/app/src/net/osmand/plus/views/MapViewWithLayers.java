package net.osmand.plus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewStub;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mudita.maps.R;

import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMap.OsmandMapListener;
import net.osmand.plus.views.corenative.NativeCoreContext;

public class MapViewWithLayers extends FrameLayout {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapView;

	private OsmandMapListener mapListener;
	private AtlasMapRendererView atlasMapRendererView;

	public MapViewWithLayers(@NonNull Context context) {
		this(context, null);
	}

	public MapViewWithLayers(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MapViewWithLayers(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MapViewWithLayers(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		app = getMyApplication();
		settings = app.getSettings();

		OsmandMap osmandMap = app.getOsmandMap();
		osmandMap.addListener(getMapListener());

		mapView = osmandMap.getMapView();
		mapView.setupTouchDetectors(getContext());

		inflate(UiUtilities.getThemedContext(context, false), R.layout.map_view_with_layers, this);
	}

	public void setupRenderingView() {
		OsmAndMapSurfaceView surfaceView = findViewById(R.id.MapView);
		OsmAndMapLayersView mapLayersView = findViewById(R.id.MapLayersView);

		boolean useOpenglRender = app.useOpenGlRenderer();
		if (useOpenglRender) {
			// Might be used in the future, but its configuration (default.renderer and POI icons) need to be updated.
			// setupAtlasMapRendererView();
			mapLayersView.setMapView(mapView);
			app.getMapViewTrackingUtilities().setMapView(mapView);
		} else {
			surfaceView.setMapView(mapView);
		}
		mapView.setMapRenderer(useOpenglRender ? atlasMapRendererView : null);
		AndroidUiHelper.updateVisibility(surfaceView, !useOpenglRender);
		AndroidUiHelper.updateVisibility(mapLayersView, useOpenglRender);
		AndroidUiHelper.updateVisibility(atlasMapRendererView, useOpenglRender);
	}

	private void setupAtlasMapRendererView() {
		ViewStub stub = findViewById(R.id.atlasMapRendererViewStub);
		if (atlasMapRendererView == null && stub != null) {
			atlasMapRendererView = (AtlasMapRendererView) stub.inflate();
			atlasMapRendererView.setAzimuth(0);
			float elevationAngle = mapView.normalizeElevationAngle(settings.getLastKnownMapElevation());
			atlasMapRendererView.setElevationAngle(elevationAngle);
			NativeCoreContext.getMapRendererContext().setMapRendererView(atlasMapRendererView);
		}
	}

	public void onResume() {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnResume();
		}
	}

	public void onPause() {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnPause();
		}
	}

	public void onDestroy() {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnDestroy();
		}
		mapView.clearTouchDetectors();
		app.getOsmandMap().removeListener(getMapListener());
	}

	@NonNull
	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}

	@NonNull
	private OsmandMapListener getMapListener() {
		if (mapListener == null) {
			mapListener = new OsmandMapListener() {

				@Override
				public void onChangeZoom(int zoomLevel) {
					mapView.showAndHideMapPosition();
				}

				@Override
				public void onSetMapElevation(float angle) {
					if (atlasMapRendererView != null) {
						atlasMapRendererView.setElevationAngle(angle);
					}
				}

				@Override
				public void onSetupRenderingView() {
					setupRenderingView();
				}
			};
		}
		return mapListener;
	}
}
