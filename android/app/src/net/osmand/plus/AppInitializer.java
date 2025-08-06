package net.osmand.plus;

import static net.osmand.plus.AppVersionUpgradeOnInit.LAST_APP_VERSION;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.mudita.download.repository.utils.DownloadManager;
import com.mudita.maps.R;
import com.mudita.maps.frontitude.R.string;

import net.osmand.GPXUtilities;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.core.android.NativeCore;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.notifications.NotificationHelper;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.render.TravelRendererHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.AvoidRoadsHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.track.helpers.GpsFilterHelper;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.CommandPlayerException;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import btools.routingapp.IBRouterService;
import kotlin.Unit;

public class AppInitializer implements IProgress {

	private static final Log LOG = PlatformUtil.getLog(AppInitializer.class);

	private final OsmandApplication app;
	private final AppVersionUpgradeOnInit appVersionUpgrade;

	private final List<String> warnings = new ArrayList<>();
	private final List<AppInitializeListener> listeners = new ArrayList<>();

	private boolean initSettings;
	private boolean activityChangesShowed;
	private long startTime;
	private long startBgTime;
	private boolean appInitializing = true;
	private String taskName;
	private SharedPreferences startPrefs;

	public enum InitEvents {
		FAVORITES_INITIALIZED, NATIVE_INITIALIZED, NATIVE_OPEN_GL_INITIALIZED, TASK_CHANGED,
		MAPS_INITIALIZED, POI_TYPES_INITIALIZED, POI_FILTERS_INITIALIZED, ASSETS_COPIED,
		INIT_RENDERERS, RESTORE_BACKUPS, INDEX_REGION_BOUNDARIES, SAVE_GPX_TRACKS, LOAD_GPX_TRACKS,
		ROUTING_CONFIG_INITIALIZED
	}

	static {
		//Set old time format of GPX for Android 6.0 and lower
		GPXUtilities.GPX_TIME_OLD_FORMAT = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M;
	}

	public interface AppInitializeListener {

		@WorkerThread
		default void onStart(@NonNull AppInitializer init) {

		}

		@UiThread
		default void onProgress(@NonNull AppInitializer init, @NonNull InitEvents event) {

		}

		@UiThread
		default void onFinish(@NonNull AppInitializer init) {

		}
	}

	public interface LoadRoutingFilesCallback {
		void onRoutingFilesLoaded();
	}

	public interface InitOpenglListener {
		void onOpenglInitialized();
	}

	public AppInitializer(@NonNull OsmandApplication app) {
		this.app = app;
		appVersionUpgrade = new AppVersionUpgradeOnInit(app);
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public boolean isAppInitializing() {
		return appInitializing;
	}

	@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
	public void initVariables() {
		if (initSettings) {
			return;
		}
		ApplicationMode.onApplicationStart(app);
		startPrefs = app.getSharedPreferences(
				getLocalClassName(app.getAppCustomization().getMapActivity().getName()),
				Context.MODE_PRIVATE);
		appVersionUpgrade.upgradeVersion(startPrefs, LAST_APP_VERSION);
		initSettings = true;
	}

	public void resetFirstTimeRun() {
		appVersionUpgrade.resetFirstTimeRun(startPrefs);
	}

	public boolean isFirstTime() {
		initVariables();
		return appVersionUpgrade.isFirstTime();
	}

	public boolean isAppVersionChanged() {
		return appVersionUpgrade.isAppVersionChanged();
	}

	public int getPrevAppVersion() {
		return appVersionUpgrade.getPrevAppVersion();
	}

	private void indexRegionsBoundaries(List<String> warnings) {
		DataStorageHelper storageHelper = new DataStorageHelper(app);
		List<StorageItem> externalStorageItems = storageHelper.getStorageItems().stream().filter(it -> it.getType() == 1 ).collect(Collectors.toList());
		Optional<StorageItem> phoneStorageItem = externalStorageItems.stream().findFirst();
		if (phoneStorageItem.isPresent()) {
			StorageItem item = phoneStorageItem.get();
			File file = new File(item.getDirectory() + "/regions.ocbf");
			try {
                if (!file.exists()) {
                    Algorithms.streamCopy(OsmandRegions.class.getResourceAsStream("regions.ocbf"),
                            new FileOutputStream(file));
                }
                app.getRegions().prepareFile(file.getAbsolutePath());
                DownloadManager.INSTANCE.setAppRegions(app.getRegions());
            } catch (Exception e) {
				warnings.add(e.getMessage());
				file.delete(); // recreate file
				LOG.error(e.getMessage(), e);
			}

		}
	}

	private void initPoiTypes() {
		app.getPoiTypes().setForbiddenTypes(app.getOsmandSettings().getForbiddenTypes());
		if (app.getAppPath(IndexConstants.SETTINGS_DIR + "poi_types.xml").exists()) {
			app.getPoiTypes().init(app.getAppPath(IndexConstants.SETTINGS_DIR + "poi_types.xml").getAbsolutePath());
		} else {
			app.getPoiTypes().init();
		}
		app.getPoiTypes().setPoiTranslator(new MapPoiTypesTranslator(app));
	}

	public void onCreateApplication() {
		// always update application mode to default
		OsmandSettings osmandSettings = app.getSettings();
		if (osmandSettings.FOLLOW_THE_ROUTE.get()) {
			ApplicationMode savedMode = osmandSettings.readApplicationMode();
			if (!osmandSettings.APPLICATION_MODE.get().getStringKey().equals(savedMode.getStringKey())) {
				osmandSettings.setApplicationMode(savedMode);
			}
		} else {
			osmandSettings.setApplicationMode(osmandSettings.DEFAULT_APPLICATION_MODE.get());
		}
		startTime = System.currentTimeMillis();
		getLazyRoutingConfig();
		app.applyTheme(app);
		startupInit(app.reconnectToBRouter(), IBRouterService.class);
		app.setPoiTypes(startupInit(MapPoiTypes.getDefaultNoInit(), MapPoiTypes.class));
		app.setTransportRoutingHelper(startupInit(new TransportRoutingHelper(app), TransportRoutingHelper.class));
		app.setRoutingHelper(startupInit(new RoutingHelper(app), RoutingHelper.class));
		app.setRoutingOptionsHelper(startupInit(new RoutingOptionsHelper(app), RoutingOptionsHelper.class));
		app.setResourceManager(startupInit(new ResourceManager(app), ResourceManager.class));
		app.setLocationProvider(startupInit(new OsmAndLocationProvider(app), OsmAndLocationProvider.class));
		app.setAvoidSpecificRoads(startupInit(new AvoidSpecificRoads(app), AvoidSpecificRoads.class));
		app.setAvoidRoadsHelper(startupInit(new AvoidRoadsHelper(app), AvoidRoadsHelper.class));
		app.setSavingTrackHelper(startupInit(new SavingTrackHelper(app), SavingTrackHelper.class));
		app.setNotificationHelper(startupInit(new NotificationHelper(app), NotificationHelper.class));
		app.setSelectedGpxHelper(startupInit(new GpxSelectionHelper(app, app.getSavingTrackHelper()), GpxSelectionHelper.class));
		app.setGpxDisplayHelper(startupInit(new GpxDisplayHelper(app), GpxDisplayHelper.class));
		app.setGpxDbHelper(startupInit(new GpxDbHelper(app), GpxDbHelper.class));
		app.setFavoritesHelper(startupInit(new FavouritesHelper(app), FavouritesHelper.class));
		app.setWaypointHelper(startupInit(new WaypointHelper(app), WaypointHelper.class));
		app.aidlApi = startupInit(new OsmandAidlApi(app), OsmandAidlApi.class);

		app.setRegions(startupInit(new OsmandRegions(), OsmandRegions.class));
		updateRegionVars();

		app.setPoiFilters(startupInit(new PoiFiltersHelper(app), PoiFiltersHelper.class));
		app.setRendererRegistry(startupInit(new RendererRegistry(app), RendererRegistry.class));
		app.setGeocodingLookupService(startupInit(new GeocodingLookupService(app), GeocodingLookupService.class));
		app.setTargetPointsHelper(startupInit(new TargetPointsHelper(app), TargetPointsHelper.class));
		app.setMapMarkersDbHelper(startupInit(new MapMarkersDbHelper(app), MapMarkersDbHelper.class));
		app.setMapMarkersHelper(startupInit(new MapMarkersHelper(app), MapMarkersHelper.class));
		app.setSearchUICore(startupInit(new QuickSearchHelper(app), QuickSearchHelper.class));
		app.setMapViewTrackingUtilities(startupInit(new MapViewTrackingUtilities(app), MapViewTrackingUtilities.class));
		app.setOsmandMap(startupInit(new OsmandMap(app), OsmandMap.class));

		app.setTravelRendererHelper(startupInit(new TravelRendererHelper(app), TravelRendererHelper.class));

		app.setFileSettingsHelper(startupInit(new FileSettingsHelper(app), FileSettingsHelper.class));
		app.setQuickActionRegistry(startupInit(new QuickActionRegistry(app.getSettings()), QuickActionRegistry.class));
		app.setOnlineRoutingHelper(startupInit(new OnlineRoutingHelper(app), OnlineRoutingHelper.class));
		app.setGpsFilterHelper(startupInit(new GpsFilterHelper(app), GpsFilterHelper.class));
		app.setDownloadTilesHelper(startupInit(new DownloadTilesHelper(app), DownloadTilesHelper.class));

		initOpeningHoursParser();
		registerLocaleChangeListener();
	}

	private void initOpeningHoursParser() {
		OpeningHoursParser.setAdditionalString("off", app.getString(R.string.day_off_label));
		OpeningHoursParser.setAdditionalString("is_open", app.getString(R.string.poi_dialog_opening_hours));
		OpeningHoursParser.setAdditionalString("is_open_24_7", app.getString(R.string.shared_string_is_open_24_7));
		OpeningHoursParser.setAdditionalString("will_open_at", app.getString(R.string.will_open_at));
		OpeningHoursParser.setAdditionalString("open_from", app.getString(R.string.open_from));
		OpeningHoursParser.setAdditionalString("will_close_at", app.getString(R.string.will_close_at));
		OpeningHoursParser.setAdditionalString("open_till", app.getString(R.string.open_till));
		OpeningHoursParser.setAdditionalString("will_open_tomorrow_at", app.getString(R.string.will_open_tomorrow_at));
		OpeningHoursParser.setAdditionalString("will_open_on", app.getString(R.string.will_open_on));
	}

	private void updateRegionVars() {
		app.getRegions().setTranslator(id -> {
			if (WorldRegion.AFRICA_REGION_ID.equals(id)) {
				return app.getString(string.maps_label_africa);
			} else if (WorldRegion.AUSTRALIA_AND_OCEANIA_REGION_ID.equals(id)) {
				return app.getString(string.maps_label_australiaandoceania);
			} else if (WorldRegion.ASIA_REGION_ID.equals(id)) {
				return app.getString(string.maps_label_asia);
			} else if (WorldRegion.CENTRAL_AMERICA_REGION_ID.equals(id)) {
				return app.getString(string.maps_label_centralamerica);
			} else if (WorldRegion.EUROPE_REGION_ID.equals(id)) {
				return app.getString(string.settings_all_label_europe);
			} else if (WorldRegion.RUSSIA_REGION_ID.equals(id)) {
				return app.getString(string.maps_label_russia);
			} else if (WorldRegion.NORTH_AMERICA_REGION_ID.equals(id)) {
				return app.getString(string.maps_label_northamerica);
			} else if (WorldRegion.SOUTH_AMERICA_REGION_ID.equals(id)) {
				return app.getString(string.maps_label_southamerica);
			} else if (WorldRegion.ANTARCTICA_REGION_ID.equals(id)) {
				return app.getString(string.maps_label_antarctica);
			}
			return null;
		});
		app.getRegions().setLocale(app.getLanguage(), app.getLocaleHelper().getCountry());
	}


	private <T> T startupInit(T object, Class<T> class1) {
		long t = System.currentTimeMillis();
		if (t - startTime > 7) {
			System.err.println("Startup service " + class1.getName() + " took too long " + (t - startTime) + " ms");
		}
		startTime = t;
		return object;
	}

	@SuppressLint("StaticFieldLeak")
	private void getLazyRoutingConfig() {
		loadRoutingFiles(app, () -> notifyEvent(InitEvents.ROUTING_CONFIG_INITIALIZED));
	}

	public static void loadRoutingFiles(@NonNull OsmandApplication app, @Nullable LoadRoutingFilesCallback callback) {
		new AsyncTask<Void, Void, Map<String, RoutingConfiguration.Builder>>() {

			@Override
			protected Map<String, RoutingConfiguration.Builder> doInBackground(Void... voids) {
				Map<String, String> defaultAttributes = getDefaultAttributes();
				Map<String, RoutingConfiguration.Builder> customConfigs = new HashMap<>();

				File routingFolder = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
				if (routingFolder.isDirectory()) {
					File[] fl = routingFolder.listFiles();
					if (fl != null && fl.length > 0) {
						for (File f : fl) {
							if (f.isFile() && f.getName().endsWith(IndexConstants.ROUTING_FILE_EXT) && f.canRead()) {
								try {
									String fileName = f.getName();
									RoutingConfiguration.Builder builder = new RoutingConfiguration.Builder(defaultAttributes);
									RoutingConfiguration.parseFromInputStream(new FileInputStream(f), fileName, builder);

									customConfigs.put(fileName, builder);
								} catch (XmlPullParserException | IOException e) {
									Algorithms.removeAllFiles(f);
									LOG.error(e.getMessage(), e);
								}
							}
						}
					}
				}
				return customConfigs;
			}

			@Override
			protected void onPostExecute(Map<String, RoutingConfiguration.Builder> customConfigs) {
				if (!customConfigs.isEmpty()) {
					app.getCustomRoutingConfigs().putAll(customConfigs);
				}
				app.getAvoidSpecificRoads().initRouteObjects(false);
				if (callback != null) {
					callback.onRoutingFilesLoaded();
				}
			}

			private Map<String, String> getDefaultAttributes() {
				Map<String, String> defaultAttributes = new HashMap<>();
				RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
				for (Map.Entry<String, String> entry : builder.getAttributes().entrySet()) {
					String key = entry.getKey();
					if (!"routerName".equals(key)) {
						defaultAttributes.put(key, entry.getValue());
					}
				}
				return defaultAttributes;
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}


	public synchronized void initVoiceDataInDifferentThread(@NonNull Context context,
	                                                        @NonNull ApplicationMode applicationMode,
	                                                        @NonNull String voiceProvider,
	                                                        @Nullable Runnable onFinishInitialization,
	                                                        boolean showProgress) {
		String progressTitle = app.getString(R.string.loading_data);
		String progressMessage = app.getString(R.string.voice_data_initializing);
		ProgressDialog progressDialog = showProgress && context instanceof Activity
				? ProgressDialog.show(context, progressTitle, progressMessage)
				: null;

		new Thread(() -> {
			try {
				if (app.getPlayer() != null) {
					app.getPlayer().clear();
				}
				app.setPlayer(CommandPlayer.createCommandPlayer(app, applicationMode, voiceProvider));
				app.getRoutingHelper().getVoiceRouter().setPlayer(app.getPlayer());
			} catch (CommandPlayerException e) {
				LOG.error("Failed to create CommandPlayer", e);
			} finally {
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
				if (onFinishInitialization != null) {
					((OsmandApplication) context.getApplicationContext()).runInUIThread(onFinishInitialization);
				}
			}
		}).start();
	}

	private void startApplicationBackground() {
		try {
			notifyStart();
			startBgTime = System.currentTimeMillis();
			app.getRendererRegistry().initRenderers();
			notifyEvent(InitEvents.INIT_RENDERERS);
			// native depends on renderers
			initOpenGl();

			// init poi types before indexes and before POI
			initPoiTypes();
			notifyEvent(InitEvents.POI_TYPES_INITIALIZED);
			app.getResourceManager().reloadIndexesOnStart(this, warnings);
			// native depends on renderers
			initNativeCore();
			app.getFavoritesHelper().loadFavorites();
			app.getGpxDbHelper().loadGpxItems();
			notifyEvent(InitEvents.FAVORITES_INITIALIZED);
			app.getPoiFilters().reloadAllPoiFilters();
			app.getPoiFilters().loadSelectedPoiFilters();
			notifyEvent(InitEvents.POI_FILTERS_INITIALIZED);
			indexRegionsBoundaries(warnings);
			notifyEvent(InitEvents.INDEX_REGION_BOUNDARIES);
			app.getSelectedGpxHelper().loadGPXTracks(this);
			notifyEvent(InitEvents.LOAD_GPX_TRACKS);
			saveGPXTracks();
			notifyEvent(InitEvents.SAVE_GPX_TRACKS);
			// restore backuped favorites to normal file -> this is obsolete with new favorite concept
			//restoreBackupForFavoritesFiles();
			notifyEvent(InitEvents.RESTORE_BACKUPS);
			app.getMapMarkersHelper().syncAllGroups();
			app.getSearchUICore().initSearchUICore();
			DownloadManager.INSTANCE.setReindexMaps(map -> {
				List<File> maps = new ArrayList<>();
				maps.add(map);
				app.getResourceManager().indexingMaps(null, maps);
				return Unit.INSTANCE;
			});
		} catch (RuntimeException e) {
			e.printStackTrace();
			warnings.add(e.getMessage());
		} finally {
			appInitializing = false;
			notifyFinish();
			if (!Algorithms.isEmpty(warnings)) {
				app.showToastMessage(AndroidUtils.formatWarnings(warnings).toString());
			}
		}
	}

	private void registerLocaleChangeListener() {
		app.registerReceiver(
				new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						app.getRegions().setLocale(app.getLanguage(), app.getLocaleHelper().getCountry());
						initPoiTypes();
						indexRegionsBoundaries(warnings);
					}
				}, new IntentFilter(Intent.ACTION_LOCALE_CHANGED)
		);
	}

	private void saveGPXTracks() {
		if (app.getSavingTrackHelper().hasDataToSave()) {
			long timeUpdated = app.getSavingTrackHelper().getLastTrackPointTime();
			if (System.currentTimeMillis() - timeUpdated >= 1000 * 60 * 30) {
				startTask(app.getString(R.string.saving_gpx_tracks), -1);
				try {
					warnings.addAll(app.getSavingTrackHelper().saveDataToGpx(app.getAppCustomization().getTracksDir()).getWarnings());
				} catch (RuntimeException e) {
					warnings.add(e.getMessage());
				}
			} else {
				app.getSavingTrackHelper().loadGpxFromDatabase();
			}
		}
	}

	private final ExecutorService initOpenglSingleThreadExecutor = Executors.newSingleThreadExecutor();

	@SuppressLint("StaticFieldLeak")
	public void initOpenglAsync(@Nullable InitOpenglListener listener) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... voids) {
				initOpenGl();
				return null;
			}

			@Override
			protected void onPostExecute(Void unused) {
				if (listener != null) {
					listener.onOpenglInitialized();
				}
			}
		}.executeOnExecutor(initOpenglSingleThreadExecutor);
	}

	private void initOpenGl() {
		OsmandSettings settings = app.getSettings();
		if (!NativeCore.isAvailable() && settings.USE_OPENGL_RENDER.get()) {
  			settings.USE_OPENGL_RENDER.set(false);
		} else if (NativeCore.isAvailable() && !Version.isQnxOperatingSystem()) {
			try {
				NativeCoreContext.init(app);
				settings.USE_OPENGL_RENDER.set(true);
				settings.OPENGL_RENDER_FAILED.set(false);
			} catch (Throwable throwable) {
				settings.USE_OPENGL_RENDER.set(false);
				settings.OPENGL_RENDER_FAILED.set(true);
				LOG.error("NativeCoreContext", throwable);
			}
			if (settings.OPENGL_RENDER_FAILED.get()) {
				settings.USE_OPENGL_RENDER.set(false);
				warnings.add("Native OpenGL library is not supported. Please try again after exit");
			}
		}
		notifyEvent(InitEvents.NATIVE_OPEN_GL_INITIALIZED);
	}

	private void initNativeCore() {
		if (!Version.isQnxOperatingSystem()) {
			OsmandSettings osmandSettings = app.getSettings();
			if (osmandSettings.NATIVE_RENDERING_FAILED.get()) {
				osmandSettings.SAFE_MODE.set(true);
				osmandSettings.NATIVE_RENDERING_FAILED.set(false);
				warnings.add(app.getString(R.string.native_library_not_supported));
			} else {
				osmandSettings.SAFE_MODE.set(false);
				osmandSettings.NATIVE_RENDERING_FAILED.set(true);
				startTask(app.getString(R.string.init_native_library), -1);
				RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
				NativeOsmandLibrary lib = NativeOsmandLibrary.getLibrary(storage, app);
				boolean initialized = lib != null;
				osmandSettings.NATIVE_RENDERING_FAILED.set(false);
				if (!initialized) {
					LOG.info("Native library could not be loaded!");
				} else {
					File ls = app.getAppPath("fonts");
					lib.loadFontData(ls);
				}

			}
			app.getResourceManager().initMapBoundariesCacheNative();
		}
		notifyEvent(InitEvents.NATIVE_INITIALIZED);
	}

	public void notifyStart() {
		for (AppInitializeListener listener : listeners) {
			listener.onStart(this);
		}
	}

	public void notifyFinish() {
		app.getUiHandler().post(() -> {
			List<AppInitializeListener> listeners = new ArrayList<>(this.listeners);
			for (AppInitializeListener l : listeners) {
				l.onFinish(this);
			}
		});
	}

	public void notifyEvent(@NonNull InitEvents event) {
		if (event != InitEvents.TASK_CHANGED) {
			long time = System.currentTimeMillis();
			System.out.println("Initialized " + event + " in " + (time - startBgTime) + " ms");
			startBgTime = time;
		}
		for (AppInitializeListener l : listeners) {
			l.onProgress(AppInitializer.this, event);
		}
	}

	@Override
	public void startTask(String taskName, int work) {
		this.taskName = taskName;
		notifyEvent(InitEvents.TASK_CHANGED);
	}

	@Override
	public void startWork(int work) {
	}

	@Override
	public void progress(int deltaWork) {
	}

	@Override
	public void remaining(int remainingWork) {
	}

	@Override
	public void finishTask() {
		taskName = null;
		notifyEvent(InitEvents.TASK_CHANGED);
	}

	public String getCurrentInitTaskName() {
		return taskName;
	}


	@Override
	public boolean isIndeterminate() {
		return true;
	}


	@Override
	public boolean isInterrupted() {
		return false;
	}


	private boolean applicationBgInitializing;

	public synchronized void startApplication() {
		if (applicationBgInitializing) {
			return;
		}
		applicationBgInitializing = true;
		new Thread(() -> {
			try {
				startApplicationBackground();
			} finally {
				applicationBgInitializing = false;
			}
		}, "Initializing app").start();
	}

	public void addListener(@NonNull AppInitializeListener listener) {
		this.listeners.add(listener);
		if (!appInitializing) {
			listener.onFinish(this);
		}
	}

	public void removeListener(@NonNull AppInitializeListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public void setGeneralProgress(String genProgress) {
	}

	private String getLocalClassName(@NonNull String cls) {
		String pkg = app.getPackageName();
		int packageLen = pkg.length();
		if (!cls.startsWith(pkg) || cls.length() <= packageLen
				|| cls.charAt(packageLen) != '.') {
			return cls;
		}
		return cls.substring(packageLen + 1);
	}
}
