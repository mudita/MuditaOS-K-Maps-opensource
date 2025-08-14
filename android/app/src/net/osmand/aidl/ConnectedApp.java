package net.osmand.aidl;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONFIGURE_MAP_ITEM_ID_SCHEME;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mudita.maps.R;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectedApp implements Comparable<ConnectedApp> {

	public static final String AIDL_LAYERS_PREFIX = "aidl_layers_";
	public static final String AIDL_WIDGETS_PREFIX = "aidl_widgets_";

	static final String AIDL_OBJECT_ID = "aidl_object_id";
	static final String AIDL_PACKAGE_NAME = "aidl_package_name";

	static final String AIDL_ADD_MAP_WIDGET = "aidl_add_map_widget";
	static final String AIDL_REMOVE_MAP_WIDGET = "aidl_remove_map_widget";

	static final String AIDL_ADD_MAP_LAYER = "aidl_add_map_layer";
	static final String AIDL_REMOVE_MAP_LAYER = "aidl_remove_map_layer";

	static final String PACK_KEY = "pack";
	static final String ENABLED_KEY = "enabled";

	private final OsmandApplication app;

	private final Map<String, AidlMapWidgetWrapper> widgets = new ConcurrentHashMap<>();
	private final Map<String, AidlMapLayerWrapper> layers = new ConcurrentHashMap<>();
	private final Map<String, OsmandMapLayer> mapLayers = new ConcurrentHashMap<>();

	private final CommonPreference<Boolean> layersPref;

	private final String pack;
	private String name;

	private Drawable icon;

	private boolean enabled;

	ConnectedApp(OsmandApplication app, String pack, boolean enabled) {
		this.app = app;
		this.pack = pack;
		this.enabled = enabled;
		layersPref = app.getSettings().registerBooleanPreference(AIDL_LAYERS_PREFIX + pack, true).cache();
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@NonNull
	public String getPack() {
		return pack;
	}

	@Nullable
	public Drawable getIcon() {
		return icon;
	}

	@NonNull
	public Map<String, AidlMapWidgetWrapper> getWidgets() {
		return widgets;
	}

	@NonNull
	public Map<String, AidlMapLayerWrapper> getLayers() {
		return layers;
	}

	@NonNull
	public Map<String, OsmandMapLayer> getMapLayers() {
		return mapLayers;
	}

	void switchEnabled() {
		enabled = !enabled;
	}

	void registerMapLayers(@NonNull Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (AidlMapLayerWrapper layer : layers.values()) {
			OsmandMapLayer mapLayer = mapLayers.get(layer.getId());
			if (mapLayer != null) {
				mapView.removeLayer(mapLayer);
			}
		}
	}

	void registerLayerContextMenu(ContextMenuAdapter menuAdapter, MapActivity mapActivity) {
		ItemClickListener listener = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
										  @NonNull View view, @NonNull ContextMenuItem item) {
				CompoundButton btn = view.findViewById(R.id.toggle_item);
				if (btn != null && btn.getVisibility() == View.VISIBLE) {
					btn.setChecked(!btn.isChecked());
					item.setColor(app, btn.isChecked() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					uiAdapter.onDataSetChanged();
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
											  @Nullable View view, @NotNull ContextMenuItem item,
											  boolean isChecked) {
				if (layersPref.set(isChecked)) {
					item.setColor(app, isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					item.setSelected(isChecked);
					uiAdapter.onDataSetChanged();
					mapActivity.refreshMap();
				}
				return false;
			}
		};
		boolean layersEnabled = layersPref.get();
		menuAdapter.addItem(new ContextMenuItem(CONFIGURE_MAP_ITEM_ID_SCHEME + AIDL_LAYERS_PREFIX + pack)
				.setTitle(name)
				.setListener(listener)
				.setSelected(layersEnabled)
				.setIcon(R.drawable.ic_extension_dark)
				.setColor(app, layersEnabled ? R.color.osmand_orange : ContextMenuItem.INVALID_ID));
	}

	@Override
	public int compareTo(@NonNull ConnectedApp app) {
		if (name != null && app.name != null) {
			return name.compareTo(app.name);
		}
		return 0;
	}
}