package net.osmand.plus.plugins.development;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BUILDS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OSMAND_DEV;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.mudita.maps.R;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.LocationSimulationAction;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.List;

public class OsmandDevelopmentPlugin extends OsmandPlugin {

	private final StateChangedListener<Boolean> showHeightmapsListener;

	public final OsmandPreference<Boolean> SHOW_HEIGHTMAPS;

	public OsmandDevelopmentPlugin(OsmandApplication app) {
		super(app);

		SHOW_HEIGHTMAPS = registerBooleanPreference("show_heightmaps", false).makeGlobal().makeShared().cache();

		showHeightmapsListener = change -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null && mapContext.isVectorLayerEnabled()) {
				mapContext.recreateHeightmapProvider();
			}
		};
		SHOW_HEIGHTMAPS.addListener(showHeightmapsListener);
	}

	@Override
	public String getId() {
		return PLUGIN_OSMAND_DEV;
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.osmand_development_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.debugging_and_development);
	}

	@Override
	public String getHelpFileName() {
		return "feature_articles/development_plugin.html";
	}

	@Override
	public void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
		if (Version.isDeveloperVersion(mapActivity.getMyApplication())) {
			Class<?> contributionVersionActivityClass = null;
			try {
				ClassLoader classLoader = OsmandDevelopmentPlugin.class.getClassLoader();
				if (classLoader != null) {
					contributionVersionActivityClass = classLoader.loadClass("net.osmand.plus.activities.ContributionVersionActivity");
				}
			} catch (ClassNotFoundException ignore) {
			}
			Class<?> activityClass = contributionVersionActivityClass;
			if (activityClass != null) {
				helper.addItem(new ContextMenuItem(DRAWER_BUILDS_ID)
						.setTitleId(R.string.version_settings, mapActivity)
						.setIcon(R.drawable.ic_action_apk)
						.setListener((uiAdapter, view, item, isChecked) -> {
							Intent mapIntent = new Intent(mapActivity, activityClass);
							mapActivity.startActivityForResult(mapIntent, 0);
							return true;
						}));
			}
		}
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_laptop;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.osmand_development);
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(LocationSimulationAction.TYPE);
		return quickActionTypes;
	}

	public boolean isHeightmapEnabled() {
		return isHeightmapAllowed() && SHOW_HEIGHTMAPS.get();
	}

	public boolean isHeightmapAllowed() {
		return app.useOpenGlRenderer() && isHeightmapPurchased();
	}

	public boolean isHeightmapPurchased() {
		return true;
	}
}