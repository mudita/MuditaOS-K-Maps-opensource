package net.osmand.plus

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Message
import android.view.accessibility.AccessibilityManager
import btools.routingapp.BRouterServiceConnection
import btools.routingapp.IBRouterService
import com.mudita.download.repository.utils.DownloadManager
import com.mudita.map.common.enums.DrivingRegion
import com.mudita.map.common.enums.MetricsConstants
import com.mudita.map.common.repository.SettingsRepository
import com.mudita.maps.BuildConfig.BUILD_TYPE
import com.mudita.maps.BuildConfig.DEBUG
import com.mudita.maps.BuildConfig.PROGUARD_UUID
import com.mudita.maps.BuildConfig.SENTRY_DSN
import com.mudita.maps.R
import com.mudita.sentry.sdk.SentryInitializer
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import net.osmand.IndexConstants
import net.osmand.PlatformUtil
import net.osmand.aidl.OsmandAidlApi
import net.osmand.map.OsmandRegions
import net.osmand.map.WorldRegion
import net.osmand.osm.MapPoiTypes
import net.osmand.plus.AppInitializer.AppInitializeListener
import net.osmand.plus.activities.RestartActivity
import net.osmand.plus.api.SQLiteAPI
import net.osmand.plus.api.SQLiteAPIImpl
import net.osmand.plus.base.MapViewTrackingUtilities
import net.osmand.plus.download.DownloadIndexesThread
import net.osmand.plus.download.DownloadService
import net.osmand.plus.helpers.AndroidApiLocationServiceHelper
import net.osmand.plus.helpers.AvoidSpecificRoads
import net.osmand.plus.helpers.LocaleHelper
import net.osmand.plus.helpers.LocationServiceHelper
import net.osmand.plus.helpers.TargetPointsHelper
import net.osmand.plus.helpers.WaypointHelper
import net.osmand.plus.mapmarkers.MapMarkersDbHelper
import net.osmand.plus.mapmarkers.MapMarkersHelper
import net.osmand.plus.measurementtool.MeasurementEditingContext
import net.osmand.plus.myplaces.FavouritesHelper
import net.osmand.plus.notifications.NotificationHelper
import net.osmand.plus.onlinerouting.OnlineRoutingHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.accessibility.AccessibilityMode
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin
import net.osmand.plus.plugins.monitoring.SavingTrackHelper
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper
import net.osmand.plus.poi.PoiFiltersHelper
import net.osmand.plus.quickaction.QuickActionRegistry
import net.osmand.plus.render.RendererRegistry
import net.osmand.plus.render.TravelRendererHelper
import net.osmand.plus.resources.ResourceManager
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper
import net.osmand.plus.routing.AvoidRoadsHelper
import net.osmand.plus.routing.RoutingHelper
import net.osmand.plus.routing.TransportRoutingHelper
import net.osmand.plus.search.QuickSearchHelper
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmAndAppCustomization
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.backup.FileSettingsHelper
import net.osmand.plus.settings.datastorage.DataStorageHelper
import net.osmand.plus.track.helpers.GpsFilterHelper
import net.osmand.plus.track.helpers.GpxDbHelper
import net.osmand.plus.track.helpers.GpxDisplayHelper
import net.osmand.plus.track.helpers.GpxSelectionHelper
import net.osmand.plus.utils.FileUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.OsmandMap
import net.osmand.plus.views.corenative.NativeCoreContext
import net.osmand.plus.voice.CommandPlayer
import net.osmand.router.GeneralRouter
import net.osmand.router.RoutingConfiguration
import net.osmand.search.SearchUICore
import net.osmand.util.Algorithms
import timber.log.Timber

@HiltAndroidApp
class OsmandApplication : Application() {

    val appInitializer = AppInitializer(this)
    var appCustomization: OsmAndAppCustomization = OsmAndAppCustomization()
        set(value) {
            field = value
            value.setup(this)
        }
    lateinit var uiHandler: Handler
    var osmandSettings: OsmandSettings? = null
        private set

    var navigationService: NavigationService? = null

    var downloadService: DownloadService? = null
    lateinit var aidlApi: OsmandAidlApi

    val sQLiteAPI: SQLiteAPI = SQLiteAPIImpl(this)
    val uIUtilities = UiUtilities(this)
    val localeHelper = LocaleHelper(this)

    // start variables
    var resourceManager: ResourceManager? = null
    var locationProvider: OsmAndLocationProvider? = null
    var rendererRegistry: RendererRegistry? = null
    var poiFilters: PoiFiltersHelper? = null
    var poiTypes: MapPoiTypes? = null
    var routingHelper: RoutingHelper? = null
    var transportRoutingHelper: TransportRoutingHelper? = null
    var favoritesHelper: FavouritesHelper? = null
    var player: CommandPlayer? = null
    var selectedGpxHelper: GpxSelectionHelper? = null
    var gpxDisplayHelper: GpxDisplayHelper? = null
    var savingTrackHelper: SavingTrackHelper? = null
    var notificationHelper: NotificationHelper? = null
    var targetPointsHelper: TargetPointsHelper? = null
    var mapMarkersHelper: MapMarkersHelper? = null
    var mapMarkersDbHelper: MapMarkersDbHelper? = null
    var waypointHelper: WaypointHelper? = null
    var routingOptionsHelper: RoutingOptionsHelper? = null
    val downloadIndexesThread: DownloadIndexesThread by lazy {
        DownloadIndexesThread(this)
    }
    var avoidSpecificRoads: AvoidSpecificRoads? = null
    var avoidRoadsHelper: AvoidRoadsHelper? = null
    var bRouterServiceConnection: BRouterServiceConnection? = null
    var regions: OsmandRegions? = null
    var geocodingLookupService: GeocodingLookupService? = null
    var searchUICore: QuickSearchHelper? = null
    var mapViewTrackingUtilities: MapViewTrackingUtilities? = null
    var osmandMap: OsmandMap? = null
    var fileSettingsHelper: FileSettingsHelper? = null
    var gpxDbHelper: GpxDbHelper? = null
    var quickActionRegistry: QuickActionRegistry? = null
    var measurementEditingContext: MeasurementEditingContext? = null
    var onlineRoutingHelper: OnlineRoutingHelper? = null
    var travelRendererHelper: TravelRendererHelper? = null
    var gpsFilterHelper: GpsFilterHelper? = null
    var downloadTilesHelper: DownloadTilesHelper? = null
    val customRoutingConfigs: Map<String, RoutingConfiguration.Builder> = ConcurrentHashMap()
    private var externalStorageDirectory: File? = null
    var isExternalStorageDirectoryReadOnly = false
        private set

    @Inject
    lateinit var settingsRepository: SettingsRepository

    // Typeface
    override fun onCreate() {

        SentryInitializer.init(
            context = this,
            dsn = SENTRY_DSN,
            proguardUuid = PROGUARD_UUID,
            environment = BUILD_TYPE,
            isDebug = DEBUG,
        )

        if (RestartActivity.isRestartProcess(this)) {
            return
        }
        var timeToStart = System.currentTimeMillis()
        if (Version.isDeveloperVersion(this)) {
            try {
                Class.forName("net.osmand.plus.base.EnableStrictMode").newInstance()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onCreate()
        uiHandler = Handler()
        appCustomization.setup(this)
        osmandSettings = appCustomization.osmandSettings
        if (appInitializer.isFirstTime) {
            osmandSettings?.initExternalStorageDirectory()
        }
        externalStorageDirectory = osmandSettings?.externalStorageDirectory
        if (!FileUtils.isWritable(externalStorageDirectory)) {
            isExternalStorageDirectoryReadOnly = true
            externalStorageDirectory = osmandSettings?.internalAppPath
        }
        DownloadManager.getAppPath = { path: String? -> getAppPath(path) }
        val storageHelper = DataStorageHelper(this)
        val externalStorageItems = storageHelper.storageItems.filter { it.type == 1 }.map { it.directory }
        DownloadManager.downloadDirectories = externalStorageItems
        Algorithms.removeAllFiles(getAppPath(IndexConstants.TEMP_DIR))
        localeHelper.checkPreferredLocale()
        appInitializer.onCreateApplication()
        osmandMap?.mapLayers?.createLayers(osmandMap!!.mapView)
        startApplication()
        println("Time to start application " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms")
        timeToStart = System.currentTimeMillis()
        PluginsHelper.initPlugins(this)
        PluginsHelper.createLayers(this, null)
        println("Time to init plugins " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms")
        osmandMap?.mapLayers?.updateLayers(null)
        SearchUICore.setDebugMode(PluginsHelper.isDevelopment())
    }

    private fun removeSqliteDbTravelFiles() {
        val files = getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles()
        if (files != null) {
            for (file in files) {
                if (file.name.endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
                    file.delete()
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        routingHelper?.voiceRouter?.onApplicationTerminate()
        notificationHelper?.removeNotifications(false)
    }

    fun createLocationServiceHelper(): LocationServiceHelper {
        return AndroidApiLocationServiceHelper(this)
    }

    /**
     * Application settings
     *
     * @return Reference to instance of OsmandSettings
     */
    val settings: OsmandSettings?
        get() {
            if (osmandSettings == null) {
                LOG.error("Trying to access settings before they were created")
            }
            return osmandSettings
        }

    @get:Synchronized
    val downloadThread: DownloadIndexesThread
        get() {
            return downloadIndexesThread
        }

    override fun onLowMemory() {
        super.onLowMemory()
        resourceManager?.onLowMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val preferredLocale = localeHelper.preferredLocale
        if (preferredLocale != null && newConfig.locale.language != preferredLocale.language) {
            super.onConfigurationChanged(newConfig)
            baseContext.resources.updateConfiguration(
                newConfig,
                baseContext.resources.displayMetrics
            )
            Locale.setDefault(preferredLocale)
        } else {
            super.onConfigurationChanged(newConfig)
        }
    }

    fun checkApplicationIsBeingInitialized(listener: AppInitializeListener?) {
        if (listener != null) {
            appInitializer.addListener(listener)
        }
    }

    fun unsubscribeInitListener(listener: AppInitializeListener?) {
        if (listener != null) {
            appInitializer.removeListener(listener)
        }
    }

    /**
     * custom setter for osmand settings for public access
     */
    fun initOsmandSettings(osmandSettings: OsmandSettings?) {
        this.osmandSettings = osmandSettings
        PluginsHelper.initPlugins(this@OsmandApplication)
    }

    val isApplicationInitializing: Boolean
        get() = appInitializer.isAppInitializing


    fun initVoiceCommandPlayer(
        context: Context,
        appMode: ApplicationMode,
        onCommandPlayerCreated: Runnable?,
        warnNoProvider: Boolean,
        showProgress: Boolean,
        forceInitialization: Boolean,
        applyAllModes: Boolean
    ) {
        osmandSettings?.VOICE_PROVIDER?.getModeValue(appMode)?.let { voiceProvider ->
            if (OsmandSettings.VOICE_PROVIDER_NOT_USE == voiceProvider) {
                osmandSettings?.VOICE_MUTE?.setModeValue(appMode, true)
            } else if (!Algorithms.isEmpty(voiceProvider)) {
                if (player == null || voiceProvider != player?.currentVoice || forceInitialization) {
                    appInitializer.initVoiceDataInDifferentThread(
                        context, appMode, voiceProvider,
                        onCommandPlayerCreated, showProgress
                    )
                }
            }
            return@let
        }
    }

    fun stopNavigation() {
        if (locationProvider?.locationSimulation?.isRouteAnimating == true) {
            locationProvider?.locationSimulation?.stop()
        }
        routingHelper?.voiceRouter?.interruptRouteCommands()
        routingHelper?.clearCurrentRoute(null, ArrayList())
        routingHelper?.isRoutePlanningMode = false
        osmandSettings?.LAST_ROUTING_APPLICATION_MODE = osmandSettings?.APPLICATION_MODE?.get()
        osmandSettings?.applicationMode = ApplicationMode.valueOfStringKey(
            osmandSettings?.LAST_USED_APPLICATION_MODE?.get(),
            ApplicationMode.DEFAULT
        )
        targetPointsHelper?.removeAllWayPoints(false, false)
    }

    fun startApplication() {
        appInitializer.startApplication()
    }

    fun showShortToastMessage(msgId: Int, vararg args: Any?) {
        uiHandler.post {
            Timber.d("Toast message: ${getString(msgId, *args)}")
        }
    }

    fun showShortToastMessage(msg: String?) {
        uiHandler.post {
            Timber.d("Toast message: $msg")
        }
    }

    fun showToastMessage(msgId: Int, vararg args: Any?) {
        uiHandler.post {
            Timber.d("Toast message: ${getString(msgId, *args)}")
        }
    }

    fun showToastMessage(text: String?) {
        uiHandler.post {
            Timber.d("Toast message: $text")
        }
    }

    fun runInUIThread(run: Runnable) {
        uiHandler.post(run)
    }

    fun runInUIThread(run: Runnable, delay: Long) {
        uiHandler.postDelayed(run, delay)
    }

    fun runMessageInUIThreadAndCancelPrevious(messageId: Int, run: Runnable, delay: Long) {
        val msg = Message.obtain(uiHandler) {
            if (!uiHandler.hasMessages(messageId)) {
                run.run()
            }
        }
        msg.what = messageId
        uiHandler.removeMessages(messageId)
        uiHandler.sendMessageDelayed(msg, delay)
    }

    fun getAppPath(path: String?): File {
        val safePath = path ?: ""
        return File(externalStorageDirectory, safePath)
    }

    fun getMapPath(directory: File, path: String?): File {
        val safePath = path ?: ""
        return File(directory, safePath)
    }

    fun setExternalStorageDirectory(type: Int, directory: String?) {
        osmandSettings?.setExternalStorageDirectory(type, directory)
        externalStorageDirectory = osmandSettings?.externalStorageDirectory
        isExternalStorageDirectoryReadOnly = false
        resourceManager?.resetStoreDirectory()
    }

    fun applyTheme(c: Context) {
        val themeResId: Int
        val doNotUseAnimations = osmandSettings?.DO_NOT_USE_ANIMATIONS?.get()
        themeResId = if (osmandSettings?.isLightContent == false) {
            if (doNotUseAnimations == true) {
                R.style.OsmandDarkTheme_NoAnimation
            } else {
                R.style.OsmandDarkTheme
            }
        } else {
            if (doNotUseAnimations == true) {
                R.style.OsmandLightTheme_NoAnimation
            } else {
                R.style.OsmandLightTheme
            }
        }
        localeHelper.setLanguage(c)
        c.setTheme(themeResId)
    }

    fun reconnectToBRouter(): IBRouterService? {
        try {
            bRouterServiceConnection = BRouterServiceConnection.connect(this)
            if (bRouterServiceConnection != null) {
                return bRouterServiceConnection?.brouterService
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    val bRouterService: IBRouterService?
        get() {
            if (bRouterServiceConnection == null) {
                return null
            }
            var s = bRouterServiceConnection?.brouterService
            if (s != null && !s.asBinder().isBinderAlive) {
                s = reconnectToBRouter()
            }
            return s
        }
    val language: String
        get() = localeHelper.language

    override fun getAssets(): AssetManager {
        return resources.assets
    }

    override fun getResources(): Resources {
        val localizedResources = localeHelper.localizedResources
        return localizedResources ?: super.getResources()
    }

    val allRoutingConfigs: List<RoutingConfiguration.Builder>
        get() {
            val builders: MutableList<RoutingConfiguration.Builder> =
                ArrayList(customRoutingConfigs.values)
            builders.add(0, defaultRoutingConfig)
            return builders
        }

    @get:Synchronized
    val defaultRoutingConfig: RoutingConfiguration.Builder
        get() = RoutingConfiguration.getDefault()

    fun getCustomRoutingConfig(key: String): RoutingConfiguration.Builder? {
        return customRoutingConfigs[key]
    }

    fun getRoutingConfigForMode(mode: ApplicationMode): RoutingConfiguration.Builder {
        var builder: RoutingConfiguration.Builder? = null
        val routingProfileKey = mode.routingProfile
        if (!Algorithms.isEmpty(routingProfileKey)) {
            val index = routingProfileKey.indexOf(IndexConstants.ROUTING_FILE_EXT)
            if (index != -1) {
                val configKey =
                    routingProfileKey.substring(0, index + IndexConstants.ROUTING_FILE_EXT.length)
                builder = customRoutingConfigs[configKey]
            }
        }
        return builder ?: defaultRoutingConfig
    }

    fun getRouter(mode: ApplicationMode): GeneralRouter? {
        val builder = getRoutingConfigForMode(mode)
        return getRouter(builder, mode)
    }

    fun getRouter(builder: RoutingConfiguration.Builder, am: ApplicationMode): GeneralRouter? {
        var router = builder.getRouter(am.routingProfile)
        if (router == null) {
            router = builder.getRouter(am.defaultRoutingProfile)
        }
        return router
    }

    fun accessibilityEnabled(): Boolean {
        return accessibilityEnabledForMode(settings?.APPLICATION_MODE?.get())
    }

    fun accessibilityEnabledForMode(appMode: ApplicationMode?): Boolean {
        val mode = settings?.ACCESSIBILITY_MODE?.getModeValue(appMode)
        if (!PluginsHelper.isActive(AccessibilityPlugin::class.java)) {
            return false
        }
        if (mode == AccessibilityMode.ON) {
            return true
        } else if (mode == AccessibilityMode.OFF) {
            return false
        }
        return systemAccessibilityEnabled()
    }

    fun systemAccessibilityEnabled(): Boolean {
        return (getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).isEnabled
    }

    fun startNavigationService(intent: Int) {
        var safeIntent = intent
        val serviceIntent = Intent(this, NavigationService::class.java)
        navigationService?.let {
            it.stopSelf()
            safeIntent = safeIntent or it.getUsedBy()
        }
        serviceIntent.putExtra(NavigationService.USAGE_INTENT, safeIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        //getNotificationHelper().showNotifications();
    }

    fun startDownloadService() {
        val serviceIntent = Intent(this, DownloadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    fun getLangTranslation(l: String): String {
        try {
            val f = R.string::class.java.getField("lang_$l")
            if (f != null) {
                val inValue = f[null] as Int
                return getString(inValue)
            }
        } catch (e: Exception) {
            System.err.println(e.message)
        }
        return l
    }

    fun setupDrivingRegion(reg: WorldRegion) {
        var drg: DrivingRegion? = null
        val params = reg.params
        //		boolean americanSigns = "american".equals(params.getRegionRoadSigns());
        val leftHand = "yes" == params.regionLeftHandDriving
        val mc1 =
            if ("miles" == params.regionMetric) MetricsConstants.MILES_AND_FEET else MetricsConstants.KILOMETERS_AND_METERS
        val mc2 =
            if ("miles" == params.regionMetric) MetricsConstants.MILES_AND_METERS else MetricsConstants.KILOMETERS_AND_METERS
        for (r in DrivingRegion.values()) {
            if (r.leftHandDriving == leftHand && (r.defMetrics == mc1 || r.defMetrics == mc2)) {
                drg = r
                break
            }
        }
        if (drg != null) {
            osmandSettings?.DRIVING_REGION?.set(drg)
        }
    }

    val userAndroidId: String
        get() {
            var userAndroidId = osmandSettings?.USER_ANDROID_ID?.get()
            if (Algorithms.isEmpty(userAndroidId) || isUserAndroidIdExpired) {
                userAndroidId = UUID.randomUUID().toString()
                osmandSettings?.USER_ANDROID_ID?.set(userAndroidId)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, 3)
                osmandSettings?.USER_ANDROID_ID_EXPIRED_TIME?.set(calendar.timeInMillis)
            }
            return userAndroidId ?: ""
        }
    val isUserAndroidIdExpired: Boolean
        get() {
            val expiredTime = osmandSettings?.USER_ANDROID_ID_EXPIRED_TIME?.get() ?: return false
            return expiredTime <= 0 || expiredTime <= System.currentTimeMillis()
        }
    val isUserAndroidIdAllowed: Boolean
        get() = osmandSettings?.SEND_UNIQUE_USER_IDENTIFIER?.get() ?: true

    fun useOpenGlRenderer(): Boolean {
        return NativeCoreContext.isInit() && osmandSettings?.USE_OPENGL_RENDER?.get() ?: (Build.VERSION.SDK_INT >= 28)
    }

    companion object {
        private val LOG = PlatformUtil.getLog(OsmandApplication::class.java)
    }
}
