package net.osmand.plus.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.google.gson.Gson
import com.mudita.download.repository.utils.DownloadManager
import com.mudita.download.service.bindDownloadService
import com.mudita.download.ui.DownloadScreen
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.map.common.BuildConfig
import com.mudita.map.common.enums.MapType
import com.mudita.map.common.enums.MetricsConstants
import com.mudita.map.common.model.MapTypesData
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.model.SettingItem
import com.mudita.map.common.model.SettingItemAction
import com.mudita.map.common.model.SettingsData
import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.model.navigation.NavigationPoint
import com.mudita.map.common.model.navigation.NavigationPointType
import com.mudita.map.common.model.navigation.bottomRightMostLatLon
import com.mudita.map.common.model.navigation.topLeftMostLatLon
import com.mudita.map.common.navigation.Screen
import com.mudita.map.common.navigation.data.NavigationItemParamType
import com.mudita.map.common.navigation.data.SavedPlaceDataParamType
import com.mudita.map.common.navigation.data.SearchItemParamType
import com.mudita.map.common.navigation.navigateToMap
import com.mudita.map.common.navigation.navigateToRequiredMaps
import com.mudita.map.common.sharedPrefs.MapTypesPreference
import com.mudita.map.common.utils.INTERMEDIATE_POINTS_MAX
import com.mudita.map.common.utils.initIcons
import com.mudita.map.ui.MapScreen
import com.mudita.map.ui.MapViewModel
import com.mudita.map.ui.ScreenState
import com.mudita.map.ui.routePlanning.NavigationPointSelectionType
import com.mudita.map.window.HideStatusBarEffect
import com.mudita.maptypes.MapTypesScreen
import com.mudita.menu.repository.model.MenuItem
import com.mudita.menu.ui.MenuScreen
import com.mudita.myplaces.repository.mapper.toSearchItem
import com.mudita.myplaces.ui.MyPlacesScreen
import com.mudita.myplaces.ui.add.AddMyPlaceScreen
import com.mudita.requiredmaps.ui.RequiredMapsScreen
import com.mudita.search.ui.SearchScreen
import com.mudita.search.ui.advanced.SearchAdvancedScreen
import com.mudita.searchhistory.ui.SearchHistoryScreen
import com.mudita.settings.ui.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.IProgress
import net.osmand.Location
import net.osmand.StateChangedListener
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.ValueHolder
import net.osmand.osm.AbstractPoiType
import net.osmand.plus.AppInitializer
import net.osmand.plus.AppInitializer.AppInitializeListener
import net.osmand.plus.AppInitializer.InitEvents
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.viewmodels.MapActivityViewModel
import net.osmand.plus.base.MapViewTrackingUtilities
import net.osmand.plus.dashboard.DashboardOnMap
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint
import net.osmand.plus.importfiles.ImportHelper
import net.osmand.plus.mapmarkers.MapMarker
import net.osmand.plus.mapmarkers.MapMarkersHelper.MapMarkerChangedListener
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.receivers.SDCardBroadcastReceiver
import net.osmand.plus.render.UpdateVectorRendererAsyncTask
import net.osmand.plus.resources.ResourceManager.ResourceListener
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.routing.RouteCalculationProgressListener
import net.osmand.plus.routing.RouteCalculationResult
import net.osmand.plus.routing.RoutingHelper
import net.osmand.plus.routing.TransportRoutingHelper.TransportRouteCalculationProgressCallback
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.settings.datastorage.DataStorageHelper
import net.osmand.plus.settings.datastorage.item.StorageItem
import net.osmand.plus.settings.datastorage.item.getStorageType
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.FileUtils
import net.osmand.plus.utils.openLocationSettings
import net.osmand.plus.views.MapLayers
import net.osmand.plus.views.MapViewWithLayers
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.OsmandMapTileView.OnDrawMapListener
import net.osmand.plus.views.corenative.NativeCoreContext
import net.osmand.plus.views.listeners.MapGestureListener
import net.osmand.router.GeneralRouter
import net.osmand.router.errors.RouteCalculationError
import net.osmand.search.SearchUICore
import net.osmand.util.Algorithms
import timber.log.Timber


@AndroidEntryPoint
class MapActivity : OsmandActionBarActivity(), DownloadEvents, IRouteInformationListener,
    MapMarkerChangedListener, OnDrawMapListener,
    OsmAndAppCustomizationListener, MapGestureListener {

    private val activityResultListeners: MutableList<ActivityResultListener> = mutableListOf()
    private lateinit var sdCardBroadcastReceiver: SDCardBroadcastReceiver
    val mapActions: MapActivityActions by lazy { MapActivityActions(this) }

    private val viewModel: MapActivityViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var storageHelper: DataStorageHelper

    private val app: OsmandApplication
        get() = myApplication
    private val appSettings: OsmandSettings? by lazy { app.settings }
    val importHelper: ImportHelper by lazy { ImportHelper(this, app) }

    private var applicationModeListener: StateChangedListener<ApplicationMode>? = null
    val dashboard = DashboardOnMap(this)
    private var initListener: AppInitializeListener? = null
    private lateinit var mapViewWithLayers: MapViewWithLayers
    var isActivityDestroyed = false
        private set
    private var stopped = true
    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    private var onMapCloseClicked: () -> Unit = {}

    val mapView: OsmandMapTileView?
        get() = app.osmandMap?.mapView
    val mapViewTrackingUtilities: MapViewTrackingUtilities?
        get() = app.mapViewTrackingUtilities

    val mapLayers: MapLayers?
        get() = app.osmandMap?.mapLayers

    private val reloadIndexesListener = object : ResourceListener {
        override fun onMapsIndexed() {
            mapView?.refreshMap(updateVectorRendering = true)
        }

        override fun onMapClosed(fileName: String?) {}
    }

    @Inject
    lateinit var mapTypesPreference: MapTypesPreference

    val routeCalculationProgressListener: RouteCalculationProgressListener =
        object : RouteCalculationProgressListener {
            override fun onCalculationStart() {
                mapViewModel.onRouteLoading()
            }

            override fun onUpdateCalculationProgress(progress: Int) {}

            override fun onRequestPrivateAccessRouting() {
                val routingProfile = routingHelper?.appMode
                if (AndroidUtils.isActivityNotDestroyed(this@MapActivity)
                    && appSettings?.FORCE_PRIVATE_ACCESS_ROUTING_ASKED?.getModeValue(
                        routingProfile
                    ) == false
                ) {
                    val modes = ApplicationMode.values(app)
                    for (mode in modes) {
                        if (!getAllowPrivatePreference(mode).getModeValue(mode)) {
                            appSettings?.FORCE_PRIVATE_ACCESS_ROUTING_ASKED?.setModeValue(
                                mode,
                                true
                            )
                        }
                    }
                }
            }

            private fun getAllowPrivatePreference(appMode: ApplicationMode?): OsmandPreference<Boolean> {
                val derivedProfile = appMode?.derivedProfile
                val allowPrivate = appSettings!!.getCustomRoutingBooleanProperty(
                    GeneralRouter.ALLOW_PRIVATE,
                    false
                )
                val allowPrivateForTruck = appSettings!!.getCustomRoutingBooleanProperty(
                    GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK, false
                )
                return if (Algorithms.objectEquals(derivedProfile, "truck"))
                    allowPrivateForTruck
                else allowPrivate
            }

            override fun onUpdateMissingMaps(missingMaps: List<String>?) {
                if (missingMaps?.isNotEmpty() == true) {
                    lifecycleScope.launch(Dispatchers.IO) { clearNavigation() }
                    mapViewModel.goBackToPlanRoute(missingMaps = missingMaps)
                }
            }

            override fun onCalculationFinish(error: Exception?) {
                mapViewModel.cancelRouteCalculationTimeoutJob()
                mapViewModel.cancelRouteCalculationMemoryJob()

                val screenState = mapViewModel.uiState.value.screenState
                if (screenState is ScreenState.PlanningRoute && !screenState.isCalculating) return

                if (error != null && screenState is ScreenState.PlanningRoute) {
                    handleRouteCalculationError(error)
                    return
                }

                routingHelper?.also(mapViewModel::updateNavigationProperties)
                mapView?.backToLocation()

                val routingAppMode = routingHelper?.appMode
                if (routingAppMode != null) {
                    appSettings?.AUDIO_MANAGER_STREAM?.getModeValue(routingAppMode)?.let {
                        volumeControlStream = it
                    }
                }
            }

            private fun handleRouteCalculationError(error: Exception) {
                when (error) {
                    is RouteCalculationError.EmptyRoute,
                    is RouteCalculationError.RouteNotFound,
                    is RouteCalculationError.StartPointTooFarFromRoad,
                    is RouteCalculationError.EndPointTooFarFromRoad,
                    is RouteCalculationError.IntermediatePointTooFarFromRoad,
                    is RouteCalculationError.SelectedServiceNotAvailable,
                    is RouteCalculationError.ApplicationModeNotSupported,
                    is RouteCalculationError.RouteIsTooComplex,
                    is RouteCalculationError.CalculationTimeLimitExceeded -> {
                        mapViewModel.goBackToPlanRoute(calculationError = error as RouteCalculationError)
                    }

                    is RouteCalculationError.MissingMaps,
                    is RouteCalculationError.CalculationCancelled -> Unit

                    else -> {
                        mapViewModel.goBackToPlanRoute(calculationError = RouteCalculationError.EmptyRoute(error.message.orEmpty()))
                    }
                }
            }
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        val tm = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        mapViewWithLayers = MapViewWithLayers(this)

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        app.searchUICore?.apply {
            initSearchUICore()
            core?.init()
            core?.updateSettings(core?.searchSettings?.setRadiusLevel(1))
        }
        app.initVoiceCommandPlayer(
            this@MapActivity,
            app.settings!!.APPLICATION_MODE.get(),
            null,
            warnNoProvider = true,
            showProgress = false,
            forceInitialization = true,
            applyAllModes = true
        )
        sdCardBroadcastReceiver = SDCardBroadcastReceiver(storageHelper)
        addListenersForRouting()
        app.resourceManager?.addResourceListener(reloadIndexesListener)
        routingHelper?.appMode = when (mapTypesPreference.getMapType()) {
            MapType.DRIVING -> {
                ApplicationMode.CAR
            }

            MapType.CYCLING -> {
                ApplicationMode.BICYCLE
            }

            else -> {
                ApplicationMode.PEDESTRIAN
            }
        }
        lifecycleScope.launch {
            mapViewModel.navigationRefreshFlow().collect {
                routingHelper?.also(mapViewModel::updateNavigationProperties)
            }
        }
        setKeepScreenOnDuringNavigation()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            KompaktTheme {
                val navController = rememberNavController()
                bindDownloadService(viewModel.downloadAction())

                initIcons()

                mapLayers?.contextMenuLayer?.onLongPressCallback = { latLng ->
                    try {
                        removeAllMarkers()
                        mapViewModel.setCurrentLocation(getCurrentOrLastKnownLatLon())
                        mapViewModel.onLongPressed(latLng)
                    } catch (e: Exception) {
                        Timber.e(e)
                        false
                    }
                }
                mapLayers?.contextMenuLayer?.onSinglePressCallback = { latLon, amenity ->
                    mapViewModel.onSinglePressed(latLon, amenity)
                    removeAllMarkers()
                }
                mapLayers?.contextMenuLayer?.shouldSearchAmenities = {
                    mapViewModel.uiState.value.screenState.amenitySearchEnabled
                }
                NavHost(
                    navController = navController,
                    startDestination = Screen.Map.ROUTE_WITH_ARGS,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                    modifier = Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                ) {
                    composable(
                        route = Screen.Map.route + Screen.ARG_SEARCH_ITEM + Screen.ARG_NAV_ITEM,
                        arguments = listOf(
                            navArgument(Screen.SEARCH_ITEM) {
                                nullable = true
                                defaultValue = null
                                type = SearchItemParamType()
                            },
                            navArgument(Screen.NAV_ITEM) {
                                nullable = true
                                defaultValue = null
                                type = NavigationItemParamType()
                            },
                        )
                    ) { backStackEntry ->
                        val navItem: NavigationItem? = backStackEntry.savedStateHandle[Screen.NAV_ITEM]
                            ?: backStackEntry.arguments?.getParcelable(Screen.NAV_ITEM)
                        val item = if (navItem == null) backStackEntry.arguments?.getParcelable<SearchItem?>(Screen.SEARCH_ITEM) else null
                        onMapCloseClicked = {
                            backStackEntry.arguments?.putParcelable(Screen.NAV_ITEM, null)
                            lifecycleScope.launch(Dispatchers.IO) { clearNavigation() }
                        }

                        MapScreen(
                            mapViewModel,
                            searchItem = item,
                            navigationItem = navItem,
                            mapView = { modifier ->
                                AndroidView(
                                    modifier = modifier,
                                    factory = {
                                        mapViewWithLayers.apply { onResume() }
                                    },
                                    update = {
                                        mapViewWithLayers.onResume()
                                    }
                                )
                            },
                            onMenuClicked = {
                                navController.navigate(Screen.Menu.route)
                            },
                            onSearchClicked = {
                                val latLon = getCurrentOrLastKnownLatLon() ?: LatLon(0.0, 0.0)
                                val searchItem = SearchItem(localName = "null", latLon = latLon, itemType = SearchItemType.HISTORY)
                                val jsonItem = gson.toJson(searchItem)
                                navController.navigate("${Screen.Search.route}/$jsonItem/null")
                            },
                            onZoomInClicked = { mapView?.zoomIn() },
                            onZoomOutClicked = { mapView?.zoomOut() },
                            onMyLocationClicked = {
                                if (OsmAndLocationProvider.isLocationPermissionAvailable(this@MapActivity)) {
                                    val targetZoom = mapView?.zoom.let { zoom ->
                                        when (zoom) {
                                            null, 17 -> 16
                                            else -> 17
                                        }
                                    }
                                    if (getCurrentLatLon() != null) {
                                        mapView?.backToLocation(targetZoom)
                                    } else {
                                        mapViewModel.onUnableToMoveToCurrentLocation()
                                        removeAllMarkers()
                                    }
                                } else {
                                    mapViewModel.showLocationSharingDialog()
                                    removeAllMarkers()
                                }
                            },
                            onCenterNavigationClick = {
                                mapView?.backToLocation()
                                mapViewModel.onCenterNavigationClicked()
                            },
                            onMyPlaceSaved = {
                                val jsonItem = Uri.encode(gson.toJson(it))
                                navController.navigate("${Screen.AddMyPlace.route}/$jsonItem/false")
                            },
                            onNavigateClicked = { navigationItem ->
                                val myLocation = getCurrentLatLon()
                                val intermediatePoints = navigationItem.intermediatePoints
                                val targetLatLon = navigationItem.endPoint?.latLon ?: return@MapScreen

                                if (OsmAndLocationProvider.isLocationPermissionAvailable(this@MapActivity)) {
                                    mapViewModel.prepareForNavigation(navigationItem, myLocation)
                                    mapViewModel.setFinalAndCurrentLocation(myLocation, targetLatLon)

                                    if (mapViewModel.checkIfRouteIsTooLong(navigationItem)) {
                                        mapViewModel.goBackToPlanRoute(
                                            calculationError = RouteCalculationError.RouteIsTooComplex("Distance is too long for the selected mode")
                                        )
                                    } else if (myLocation != null) {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            setNavigationDirections(navigationItem)
                                            routingHelper?.isFollowingMode = true
                                            routingHelper?.setFinalAndCurrentLocation(
                                                targetLatLon,
                                                intermediatePoints.map { it.latLon },
                                                Location("", myLocation.latitude, myLocation.longitude)
                                            )
                                        }
                                    }
                                } else {
                                    mapViewModel.showLocationSharingDialog()
                                }
                            },
                            onNavigationItemChanged = ::setNavigationDirections,
                            onNavigationPointSelected = { type, selectedNavItem ->
                                val jsonItem = Uri.encode(gson.toJson(selectedNavItem))
                                val route = when (type) {
                                    NavigationPointSelectionType.SEARCH -> "${Screen.Search.route}/null/$jsonItem"
                                    NavigationPointSelectionType.SAVED_PLACES -> "${Screen.MyPlaces.route}/$jsonItem"
                                    else -> throw IllegalArgumentException("Unsupported navigation point selection type")
                                }
                                navController.navigate(route)
                            },
                            onNavigationModeChanged = {
                                routingHelper?.appMode = when (it.toMapType()) {
                                    MapType.DRIVING -> ApplicationMode.CAR
                                    MapType.WALKING -> ApplicationMode.PEDESTRIAN
                                    MapType.CYCLING -> ApplicationMode.BICYCLE
                                }
                                mapTypesPreference.setMapType(it.toMapType())
                                routingHelper?.onSettingsChanged(true)
                            },
                            onSoundClicked = { isEnabled ->
                                appSettings?.VOICE_MUTE?.set(isEnabled.not())
                            },
                            onCloseClicked = onMapCloseClicked,
                            setNavigationResumed = { navigationResumed ->
                                if (routingHelper?.isPauseNavigation != !navigationResumed) routingHelper?.isPauseNavigation = !navigationResumed
                            },
                            myLocation = { getCurrentOrLastKnownLatLon(allowMapLocation = false) },
                            cancelCurrentRoute = ::clearNavigation,
                            clearAllMarkers = ::removeAllMarkers,
                            fitMapToNavigationItem = ::fitMapToNavigationItem,
                            applySearchItemToMap = ::applySearchItemToMap,
                            onMissingMapsClick = { navController.navigateToRequiredMaps(it, gson) },
                            onIntroContinueClick = {
                                val shouldShowLocationSharingDialog = !OsmAndLocationProvider.isLocationPermissionAvailable(this@MapActivity)
                                mapViewModel.closeWelcomeScreen(shouldShowLocationSharingDialog)
                            },
                            onOpenLocationSettingsClick = {
                                mapViewModel.closeLocationSharingDialog()
                                openLocationSettings()
                            },
                            hasLocationPermission = { OsmAndLocationProvider.hasLocationAccess(this@MapActivity) },
                        )
                    }
                    composable(
                        Screen.Search.route + Screen.ARG_SEARCH_ITEM + Screen.ARG_NAV_ITEM,
                        arguments = listOf(
                            navArgument(Screen.SEARCH_ITEM) {
                                nullable = true
                                defaultValue = null
                                type = SearchItemParamType()
                            },
                            navArgument(Screen.NAV_ITEM) {
                                nullable = true
                                defaultValue = null
                                type = NavigationItemParamType()
                            },
                        )
                    ) { backStackEntry ->
                        val searchItem = backStackEntry.arguments?.getParcelable<SearchItem?>(Screen.SEARCH_ITEM)
                        val navItem = backStackEntry.arguments?.getParcelable<NavigationItem?>(Screen.NAV_ITEM)
                        val searchUICore = app.searchUICore?.core ?: return@composable
                        updateCurrentLocationInSearchSettings(searchUICore)

                        HideStatusBarEffect(enabled = false, restoreOnDispose = false)
                        SearchScreen(
                            searchItem = searchItem,
                            searchUICore = searchUICore,
                            onBackClicked = { navController.navigateUp() },
                            onSearchCategoryClicked = {
                                val latLon = getCurrentOrLastKnownLatLon() ?: LatLon(0.0, 0.0)
                                val item = SearchItem(
                                    localName = it?.title,
                                    desc = it?.desc,
                                    latLon = latLon,
                                    itemType = SearchItemType.CATEGORY,
                                    icon = it?.iconRes ?: com.mudita.map.common.R.drawable.icon_search,
                                )
                                val jsonItem = Uri.encode(gson.toJson(item))
                                navController.navigate("${Screen.Search.route}/$jsonItem/null")
                            },
                            onSearchSubCategoryClicked = {
                                val latLon = getCurrentOrLastKnownLatLon() ?: LatLon(0.0, 0.0)
                                val item = SearchItem(
                                    localName = it?.title,
                                    desc = it?.desc,
                                    latLon = latLon,
                                    itemType = SearchItemType.SUB_CATEGORY,
                                    icon = it?.iconRes ?: com.mudita.map.common.R.drawable.icon_search,
                                )
                                val jsonItem = Uri.encode(gson.toJson(item))
                                navController.navigate("${Screen.Search.route}/$jsonItem/null")
                            },
                            onSearchCityClicked = {
                                val latLon = getCurrentOrLastKnownLatLon() ?: LatLon(0.0, 0.0)
                                val item = SearchItem(
                                    localName = it?.title,
                                    desc = it?.desc,
                                    latLon = latLon,
                                    itemType = SearchItemType.CITY,
                                    icon = it?.iconRes ?: com.mudita.map.common.R.drawable.icon_search,
                                )
                                val jsonItem = Uri.encode(gson.toJson(item))
                                val jsonNavigationItem = Uri.encode(gson.toJson(navItem))
                                navController.navigate("${Screen.SearchCity.route}/$jsonItem/$jsonNavigationItem")
                            },
                            showOnMap = { item ->
                                if (navItem != null) {
                                    val updatedNavItem = when (navItem.currentlySelectedPoint?.type) {
                                        NavigationPointType.START -> navItem.copy(
                                            startPoint = NavigationPoint(
                                                latLon = item.latLon,
                                                address = item.address ?: item.formattedTitle,
                                                isActionActive = navItem.endPoint != null
                                            ),
                                            currentlySelectedPoint = null,
                                        )

                                        NavigationPointType.INTERMEDIATE -> navItem.intermediatePoints.find { navigationPoint ->
                                            navigationPoint.uuid == navItem.currentlySelectedPoint?.uuid
                                        }?.let { intermediatePoint ->
                                            val updatedIntermediatePoint = intermediatePoint.copy(
                                                latLon = item.latLon,
                                                address = item.address ?: item.formattedTitle,
                                            )
                                            val updatedIntermediatePoints = navItem.intermediatePoints.map { point ->
                                                if (point.uuid == updatedIntermediatePoint.uuid) updatedIntermediatePoint else point
                                            }
                                            navItem.copy(
                                                intermediatePoints = updatedIntermediatePoints,
                                                currentlySelectedPoint = null,
                                            )
                                        } ?: navItem

                                        NavigationPointType.DESTINATION -> navItem.copy(
                                            startPoint = navItem.startPoint?.copy(isActionActive = true),
                                            endPoint = NavigationPoint(
                                                latLon = item.latLon,
                                                address = item.address ?: item.formattedTitle,
                                                isActionActive = navItem.intermediatePoints.size < INTERMEDIATE_POINTS_MAX,
                                                type = NavigationPointType.DESTINATION
                                            ),
                                            currentlySelectedPoint = null,
                                        )

                                        else -> navItem
                                    }
                                    if (navController.currentDestination?.route?.startsWith(Screen.Search.route) == true) {
                                        navController.previousBackStackEntry?.savedStateHandle?.set(Screen.NAV_ITEM, updatedNavItem)
                                        navController.popBackStack()
                                    } else {
                                        val jsonItem = Uri.encode(gson.toJson(updatedNavItem))
                                        navController.navigateToMap(navItem = jsonItem)
                                    }
                                } else {
                                    val jsonItem = Uri.encode(gson.toJson(item))
                                    navController.navigateToMap(searchItem = jsonItem)
                                }
                            },
                        )
                    }
                    composable(
                        route = Screen.SearchCity.route + Screen.ARG_SEARCH_ITEM + Screen.ARG_NAV_ITEM,
                        arguments = listOf(
                            navArgument(Screen.SEARCH_ITEM) {
                                nullable = true
                                defaultValue = null
                                type = SearchItemParamType()
                            },
                            navArgument(Screen.NAV_ITEM) {
                                nullable = true
                                defaultValue = null
                                type = NavigationItemParamType()
                            },
                        )
                    ) { backStackEntry ->
                        val searchItem = backStackEntry.arguments?.getParcelable<SearchItem?>(Screen.SEARCH_ITEM)
                        val navItem = backStackEntry.arguments?.getParcelable<NavigationItem?>(Screen.NAV_ITEM)

                        val searchHelper = app.searchUICore
                        val searchUICore = searchHelper?.core
                        val searchLatLon = mapView?.currentRotatedTileBox?.centerLatLon
                        var settings = searchLatLon?.let {
                            searchUICore?.searchSettings?.setOriginalLocation(LatLon(searchLatLon.latitude, searchLatLon.longitude))
                        }
                        val locale = app.settings!!.MAP_PREFERRED_LOCALE.get()
                        val transliterate = app.settings!!.MAP_TRANSLITERATE_NAMES.get()
                        settings = settings?.setLang(locale, transliterate)
                        searchUICore?.updateSettings(settings)
                        SearchAdvancedScreen(
                            onBackClicked = { navController.navigateUp() },
                            searchItem = searchItem,
                            navigationItem = navItem,
                            searchUICore = searchUICore,
                            myLocation = {
                                getLocation()?.let {
                                    LatLon(it.latitude, it.longitude)
                                }
                            },
                            showOnMap = { addressItem ->
                                if (navItem != null) {
                                    val latLon = LatLon(addressItem.lat, addressItem.lon)
                                    val updatedNavItem = when (navItem.currentlySelectedPoint?.type) {
                                        NavigationPointType.START -> navItem.copy(
                                            startPoint = NavigationPoint(
                                                latLon,
                                                addressItem.address ?: addressItem.formattedTitle,
                                                isActionActive = navItem.endPoint != null,
                                            )
                                        )

                                        NavigationPointType.INTERMEDIATE -> navItem.intermediatePoints.find { navigationPoint ->
                                            navigationPoint.uuid == navItem.currentlySelectedPoint?.uuid
                                        }?.let { intermediatePoint ->
                                            val updatedIntermediatePoint = intermediatePoint.copy(
                                                latLon = latLon,
                                                address = addressItem.address ?: addressItem.formattedTitle,
                                            )
                                            val updatedIntermediatePoints = navItem.intermediatePoints.map { point ->
                                                if (point.uuid == updatedIntermediatePoint.uuid) updatedIntermediatePoint else point
                                            }
                                            navItem.copy(
                                                intermediatePoints = updatedIntermediatePoints
                                            )
                                        } ?: navItem

                                        NavigationPointType.DESTINATION -> navItem.copy(
                                            startPoint = navItem.startPoint?.copy(isActionActive = true),
                                            endPoint = NavigationPoint(
                                                latLon,
                                                addressItem.address ?: addressItem.formattedTitle,
                                                isActionActive = navItem.intermediatePoints.size < INTERMEDIATE_POINTS_MAX,
                                                type = NavigationPointType.DESTINATION
                                            )
                                        )

                                        else -> navItem
                                    }
                                    val jsonItem = Uri.encode(gson.toJson(updatedNavItem))
                                    navController.navigateToMap(navItem = jsonItem)
                                } else {
                                    val item = SearchItem(
                                        localName = addressItem.address,
                                        desc = addressItem.desc,
                                        distance = addressItem.distance,
                                        latLon = LatLon(addressItem.lat, addressItem.lon),
                                        icon = addressItem.iconRes,
                                        itemType = addressItem.itemType
                                    )
                                    val jsonItem = Uri.encode(gson.toJson(item))
                                    navController.navigateToMap(
                                        searchItem = jsonItem,
                                        navOptions = navOptions {
                                            popUpTo(Screen.SearchCity.route) {
                                                saveState = true
                                            }
                                            restoreState = true
                                        }
                                    )
                                }
                            }
                        )
                    }
                    composable(Screen.Menu.route) {
                        MenuScreen { item ->
                            when (item) {
                                null -> navController.navigateUp()
                                MenuItem.About -> Unit
                                MenuItem.History -> navController.navigate(Screen.SearchHistory.route)
                                MenuItem.ManageMaps -> navController.navigate(Screen.Download.route)
                                MenuItem.SavedLocation -> navController.navigate("${Screen.MyPlaces.route}/null")
                                MenuItem.Navigation -> {
                                    lifecycleScope.launch(Dispatchers.IO) { clearNavigation() }
                                    val navigationItem = NavigationItem(
                                        startPoint = getCurrentOrLastKnownLatLon(allowMapLocation = false)?.let { latLon ->
                                            NavigationPoint(latLon = latLon, address = "", isCurrentLocation = true)
                                        },
                                    )

                                    val jsonItem = Uri.encode(gson.toJson(navigationItem))
                                    navController.navigateToMap(navItem = jsonItem)
                                }

                                MenuItem.Settings -> navController.navigate(Screen.Settings.route)
                            }
                        }
                    }
                    composable(Screen.Download.route) {
                        val externalStorageItems = storageHelper.storageItems
                        externalStorageItems.removeIf { i: StorageItem -> i.type != 1 }
                        DownloadScreen(
                            onBackClicked = { navController.navigateUp() },
                            getIndexFileNames = { app.resourceManager?.indexFileNames ?: emptyMap() },
                            getIndexFiles = { app.resourceManager?.indexFiles ?: emptyMap() },
                            getLocalPath = { path -> app.getMapPath(File(externalStorageItems[0].directory), path) },
                            getSdCardPath = { path ->
                                if (externalStorageItems.size == 1) null
                                else app.getMapPath(File(externalStorageItems[1].directory), path)
                            },
                            getNameToDisplay = {
                                app.regions?.getRegionDataByDownloadName(it.substringBeforeLast("."))?.localeName ?: ""
                            },
                            reloadIndexFiles = {
                                app.downloadThread.runReloadIndexFiles(this@MapActivity)
                            }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            settingsData = SettingsData(
                                soundEnabled = app.settingsRepository.getSoundEnabled(),
                                sdCardExists = sdCardExists(),
                                storageType = storageHelper.currentStorage.getStorageType()
                            ),
                            onItemClicked = {},
                            onItemSwitched = { item ->
                                when (item) {
                                    is SettingItem.ScreenAlwaysOn -> Unit
                                    is SettingItem.Sound -> appSettings?.VOICE_MUTE?.set(item.isChecked)
                                    is SettingItem.WifiOnly -> Unit
                                }
                            },
                            onItemChecked = ::onSettingsItemChecked,
                            isDownloading = DownloadManager.isDownloading.collectAsState().value,
                        ) {
                            navController.navigateUp()
                        }
                    }
                    composable(
                        route = Screen.MyPlaces.route + Screen.ARG_NAV_ITEM,
                        arguments = listOf(
                            navArgument(Screen.NAV_ITEM) {
                                nullable = true
                                defaultValue = null
                                type = NavigationItemParamType()
                            }
                        )
                    ) {
                        val navigationItem = it.arguments?.getParcelable<NavigationItem?>(Screen.NAV_ITEM)
                        MyPlacesScreen(
                            onInfoClicked = { myPlace ->
                                val jsonItem = Uri.encode(gson.toJson(myPlace))
                                navController.navigate("${Screen.AddMyPlace.route}/$jsonItem/true")
                            },
                            onItemClicked = { myPlace ->
                                if (navigationItem != null) {
                                    if (myPlace.lat == null && myPlace.lng == null) return@MyPlacesScreen

                                    val latLon = LatLon(myPlace.lat!!, myPlace.lng!!)
                                    val updatedNavItem = when (navigationItem.currentlySelectedPoint?.type) {
                                        NavigationPointType.START -> navigationItem.copy(
                                            startPoint = NavigationPoint(
                                                latLon,
                                                address = if (myPlace.address.isNullOrEmpty()) myPlace.formattedTitle else myPlace.address.toString(),
                                                isActionActive = navigationItem.endPoint != null
                                            ),
                                            currentlySelectedPoint = null,
                                        )

                                        NavigationPointType.INTERMEDIATE -> navigationItem.intermediatePoints.find { navigationPoint ->
                                            navigationPoint.uuid == navigationItem.currentlySelectedPoint?.uuid
                                        }?.let { intermediatePoint ->
                                            val updatedIntermediatePoint = intermediatePoint.copy(
                                                latLon = latLon,
                                                address = if (myPlace.address.isNullOrEmpty()) myPlace.formattedTitle else myPlace.address.toString(),
                                            )
                                            val updatedIntermediatePoints = navigationItem.intermediatePoints.map { point ->
                                                if (point.uuid == updatedIntermediatePoint.uuid) updatedIntermediatePoint else point
                                            }
                                            navigationItem.copy(
                                                intermediatePoints = updatedIntermediatePoints,
                                                currentlySelectedPoint = null,
                                            )
                                        } ?: navigationItem

                                        NavigationPointType.DESTINATION -> navigationItem.copy(
                                            startPoint = navigationItem.startPoint?.copy(isActionActive = true),
                                            endPoint = NavigationPoint(
                                                latLon,
                                                address = if (myPlace.address.isNullOrEmpty()) myPlace.formattedTitle else myPlace.address.toString(),
                                                isActionActive = navigationItem.intermediatePoints.size < INTERMEDIATE_POINTS_MAX,
                                                type = NavigationPointType.DESTINATION
                                            ),
                                            currentlySelectedPoint = null,
                                        )

                                        else -> navigationItem
                                    }
                                    val jsonItem = Uri.encode(gson.toJson(updatedNavItem))
                                    navController.navigateToMap(navItem = jsonItem)
                                } else {
                                    val jsonItem = Uri.encode(gson.toJson(myPlace.toSearchItem()))
                                    navController.navigateToMap(searchItem = jsonItem)
                                }
                            },
                        ) {
                            navController.navigateUp()
                        }
                    }
                    composable(
                        Screen.AddMyPlace.route + Screen.ARG_ITEM + Screen.ARG_BOOL_FLAG,
                        arguments = listOf(
                            navArgument(Screen.ITEM) {
                                nullable = true
                                defaultValue = null
                                type = SavedPlaceDataParamType()
                            },
                            navArgument(Screen.ITEM_FLAG) {
                                defaultValue = false
                                type = NavType.BoolType
                            }
                        )
                    ) { backStackEntry ->
                        val myPlaceItem = backStackEntry.arguments?.getParcelable<MyPlaceItem?>(Screen.ITEM)
                        val isEditMode = backStackEntry.arguments?.getBoolean(Screen.ITEM_FLAG) ?: false
                        AddMyPlaceScreen(
                            myPlaceItem = myPlaceItem,
                            isEditMode = isEditMode,
                            onCloseClicked = { navController.navigateUp() },
                            hideKeyboard = {
                                val view = this@MapActivity.currentFocus
                                if (view != null) {
                                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                                }
                            },
                        ) { modifiedPlace ->
                            val jsonItem = Uri.encode(gson.toJson(modifiedPlace?.toSearchItem()))
                            if (isEditMode) navController.navigateUp()
                            else navController.navigateToMap(searchItem = jsonItem)
                        }
                    }
                    composable(Screen.MapTypes.route) {
                        MapTypesScreen(
                            onItemClicked = {
                                when (it.mapType) {
                                    MapType.DRIVING -> {
                                        routingHelper?.appMode = ApplicationMode.CAR
                                    }

                                    MapType.WALKING -> {
                                        routingHelper?.appMode = ApplicationMode.PEDESTRIAN
                                    }

                                    MapType.CYCLING -> {
                                        routingHelper?.appMode = ApplicationMode.BICYCLE
                                    }
                                }
                                mapTypesPreference.setMapType(it.mapType)
                                routingHelper?.onSettingsChanged(true)
                            },
                            onBackClicked = {
                                navController.popBackStack()
                            },
                            mapTypesData = MapTypesData(mapTypesPreference.getMapType())
                        )
                    }
                    composable(Screen.SearchHistory.route) {
                        SearchHistoryScreen(
                            onBackClicked = {
                                navController.popBackStack()
                            },
                            onItemClicked = {
                                val item = SearchItem(
                                    localName = it.title,
                                    desc = it.desc,
                                    latLon = LatLon(it.lat, it.lon),
                                    itemType = SearchItemType.POI,
                                    icon = it.iconRes,
                                    distance = it.distance,
                                    address = it.address
                                )
                                val jsonItem = Uri.encode(gson.toJson(item))
                                navController.navigateToMap(searchItem = jsonItem)
                            }
                        )
                    }
                    composable(Screen.RequiredMaps.route + Screen.ARG_MAP_NAMES) {
                        RequiredMapsScreen(
                            onBackClicked = { navController.popBackStack() },
                        )
                    }
                }
            }
        }

        mapView?.mapActivity = this
        mapLayers?.setMapActivity(this)
        val mapView = mapView
        mapViewTrackingUtilities?.setMapView(mapView)
        mapLayers?.createAdditionalLayers(this)

        val appSettings = appSettings
        if (appSettings != null && appSettings.isLastKnownMapLocation) {
            appSettings.lastKnownMapLocation?.also { latLon ->
                mapView?.setLatLon(latLon.latitude, latLon.longitude)
            }
            mapView?.setIntZoom(appSettings.lastKnownMapZoom)
            if (appSettings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_COMPASS) {
                mapView?.setRotateValue(appSettings.lastKnownMapRotation, true)
            }
        } else {
            // show first time when application ran
            val location =
                app.locationProvider?.getFirstTimeRunDefaultLocation { loc: Location? ->
                    if (app.locationProvider?.lastKnownLocation == null) {
                        setMapInitialLatLon(mapView, loc)
                    }
                }
            mapViewTrackingUtilities?.isMapLinkedToLocation = true
            if (location != null) {
                setMapInitialLatLon(mapView, location)
            } else {
                mapView?.apply { setIntZoom(minZoom) }
            }
        }
        app.locationProvider?.addLocationListener(
            object : OsmAndLocationListener {
                override fun updateLocation(location: Location?) {
                    setMapInitialLatLon(mapView, location)
                    location
                        ?.let { LatLon(it.latitude, it.longitude) }
                        ?.also(mapViewModel::onInitialLocation)
                    app.locationProvider?.removeLocationListener(this)
                }
            }
        )
        PluginsHelper.onMapActivityCreate(this)
        if (System.currentTimeMillis() - tm > 50) {
            System.err.println("OnCreate for MapActivity took " + (System.currentTimeMillis() - tm) + " ms")
        }
        mapView?.refreshMap(true)
        checkAppInitialization()
        registerReceiver(sdCardBroadcastReceiver, SDCardBroadcastReceiver.getIntentFilter())
        app.aidlApi.onCreateMapActivity(this)
        isActivityDestroyed = false
    }

    private fun onSettingsItemChecked(item: SettingItemAction.Checkable.CheckableItem) {
        val metricSystemKey = "default_metric_system"
        when (item) {
            is SettingItemAction.Checkable.CheckableItem.Kilometers -> {
                appSettings?.setPreferenceForAllModes(metricSystemKey, MetricsConstants.KILOMETERS_AND_METERS)
                return
            }
            is SettingItemAction.Checkable.CheckableItem.Miles -> {
                appSettings?.setPreferenceForAllModes(metricSystemKey, MetricsConstants.MILES_AND_FEET)
                return
            }
            is SettingItemAction.Checkable.CheckableItem.Card,
            is SettingItemAction.Checkable.CheckableItem.Phone -> {
                val externalStorageItems = storageHelper.storageItems.filter { it.type == 1 }
                val storage = when (item) {
                    is SettingItemAction.Checkable.CheckableItem.Card -> externalStorageItems[1]
                    is SettingItemAction.Checkable.CheckableItem.Phone -> externalStorageItems[0]
                    else -> return
                }
                val newDirectory = storage.directory
                val newDirectoryFile = File(newDirectory)
                if (FileUtils.isWritable(newDirectoryFile)) {
                    app.setExternalStorageDirectory(storage.type, newDirectory)
                    app.resourceManager?.reloadIndexesAsync(IProgress.EMPTY_PROGRESS, null)
                    storageHelper.updateStorageItems()
                }
            }
            else -> Unit
        }
    }

    private suspend fun applySearchItemToMap(searchItem: SearchItem?) {
        withContext(Dispatchers.IO) {
            if (searchItem != null) {
                val searchHelper = app.searchUICore
                val searchUICore = searchHelper?.core
                app.poiFilters?.clearSelectedPoiFilters()
                if (searchItem.itemType == SearchItemType.CATEGORY || searchItem.itemType == SearchItemType.SUB_CATEGORY) {
                    val filterPoi = when (searchItem.itemType) {
                        SearchItemType.CATEGORY -> app.poiTypes?.getPoiTypeByKey(searchItem.categoryKeyName)
                        SearchItemType.SUB_CATEGORY -> app.poiTypes?.getPoiTypeByKey(searchItem.subCategoryKeyName)
                        else -> null
                    }
                    val nFilter = PoiUIFilter(filterPoi, app, "").apply {
                        filterByName = searchItem.localName?.lowercase()?.trim()
                    }
                    app.poiFilters?.addSelectedPoiFilter(nFilter)
                } else {
                    if (searchItem.itemType == SearchItemType.POI) {
                        val filterByName by lazy {
                            val abstractPoiType =
                                searchUICore?.phrase?.lastSelectedWord?.result?.`object` as? AbstractPoiType
                            PoiUIFilter(abstractPoiType, app, "").apply {
                                filterByName = searchItem.localName?.lowercase()?.trim()
                            }
                        }
                        val filter =
                            app.poiFilters?.getFilterById(PoiUIFilter.STD_PREFIX + searchItem.localName)
                                ?: filterByName
                        app.poiFilters?.addSelectedPoiFilter(filter)
                    }

                    app.osmandMap?.setMapLocation(
                        searchItem.latLon.latitude,
                        searchItem.latLon.longitude
                    )
                    removeAllMarkers()
                    app.mapMarkersHelper?.addMapMarker(
                        LatLon(searchItem.latLon.latitude, searchItem.latLon.longitude),
                        null,
                        searchItem.formattedTitle
                    )
                    mapView?.apply { setIntZoom(max(zoom, 15)) }
                }
            }
        }
    }

    private fun setNavigationDirections(navigationItem: NavigationItem) {
        val targetPointsHelper = app.targetPointsHelper ?: return
        targetPointsHelper.removeAllWayPoints(false, false)
        removeAllMarkers()

        (navigationItem.startPoint?.latLon ?: getCurrentOrLastKnownLatLon(allowMapLocation = false))?.let { myLocation ->
            targetPointsHelper.setStartPoint(myLocation, false, PointDescription(myLocation.latitude, myLocation.longitude))
        }

        targetPointsHelper.setIntermediatePoints(navigationItem.intermediatePoints)

        navigationItem.endPoint?.latLon?.let { targetLatLon ->
            targetPointsHelper.navigateToPoint(targetLatLon, false, -1)
        }
    }

    private fun fitMapToNavigationItem(navigationItem: NavigationItem, topBarHeight: Int, bottomBarHeight: Int) {
        if (navigationItem.currentlySelectedPoint != null) return
        mapView?.apply {
            val start = navigationItem.topLeftMostLatLon()
            val end = navigationItem.bottomRightMostLatLon()
            val tileBox = currentRotatedTileBox
            val positionOffset = (bottomBarHeight - topBarHeight) / 2
            val rightTopIconOffset = AndroidUtils.dpToPx(this@MapActivity, 36f)
            val leftBottomIconOffset = AndroidUtils.dpToPx(this@MapActivity, 14f)

            if (start != null && end != null && tileBox != null) {
                fitRectToMap(
                    start = start,
                    end = end,
                    tileBoxWidthPx = tileBox.pixWidth,
                    tileBoxHeightPx = tileBox.pixHeight - topBarHeight - bottomBarHeight,
                    leftBottomRectOffset = leftBottomIconOffset,
                    rightTopRectOffset = rightTopIconOffset,
                    marginTopPx = positionOffset
                )
            }
        }
    }

    private fun clearNavigation() {
        routingHelper?.clearCurrentRoute(null, emptyList())
        app.targetPointsHelper?.removeAllWayPoints(false, false)
        removeAllMarkers()
        mapView?.setRotateValue(0f, true)
        appSettings?.lastKnownMapRotation = 0f
        refreshMap()
    }

    private fun removeAllMarkers() {
        lifecycleScope.launch(Dispatchers.IO) {
            app.mapMarkersHelper?.removeMarkers(app.mapMarkersHelper?.mapMarkers)
        }
    }

    private fun sdCardExists(): Boolean {
        val externalStorageItems = storageHelper.storageItems.filter { it.type == 1 }
        return externalStorageItems.size > 1
    }

    private fun setMapInitialLatLon(mapView: OsmandMapTileView?, location: Location?) {
        if (location != null) {
            mapView?.setLatLon(location.latitude, location.longitude)
            mapView?.setIntZoom(14)
            mapViewModel.setCurrentLocation(getCurrentOrLastKnownLatLon())
        }
    }

    private fun getLocation(): Location? {
        val locationProvider = app.locationProvider
        val lastKnownLocation = locationProvider?.lastKnownLocation
        val lastStaleKnownLocation = locationProvider?.lastStaleKnownLocation
        return lastKnownLocation ?: lastStaleKnownLocation
    }

    private fun getCurrentOrLastKnownLatLon(allowMapLocation: Boolean = true): LatLon? = getLocation()?.let {
        LatLon(it.latitude, it.longitude)
    } ?: if (allowMapLocation) {
        appSettings?.lastKnownMapLocation ?: mapLocation
    } else {
        null
    }

    private fun getCurrentLatLon(): LatLon? =
        app.locationProvider?.currentLocation?.let { LatLon(it.latitude, it.longitude) }

    private fun updateCurrentLocationInSearchSettings(searchUICore: SearchUICore) {
        mapView?.currentRotatedTileBox?.centerLatLon?.also {
            lifecycleScope.launch(Dispatchers.IO) {
                val settings = searchUICore.searchSettings.setOriginalLocation(it)
                settings?.resetSearchTypes()?.isEmptyQueryAllowed = false
                searchUICore.updateSettings(settings)
            }
        }
    }

    private fun checkAppInitialization() {
        if (app.isApplicationInitializing) {
            initListener = object : AppInitializeListener {
                var renderingViewSetup = false
                override fun onProgress(init: AppInitializer, event: InitEvents) {

                    val openGlInitialized =
                        event == InitEvents.NATIVE_OPEN_GL_INITIALIZED && NativeCoreContext.isInit()
                    if ((openGlInitialized || event == InitEvents.NATIVE_INITIALIZED) && !renderingViewSetup) {
                        lifecycleScope.launch {
                            app.osmandMap?.setupRenderingView()
                            renderingViewSetup = true
                        }
                    }
                    if (event == InitEvents.MAPS_INITIALIZED) {
                        // TODO investigate if this false cause any issues!
                        mapView?.refreshMap(false)
                    }
                    if (event == InitEvents.FAVORITES_INITIALIZED) {
                        refreshMap()
                    }
                }

                override fun onFinish(init: AppInitializer) {
                    if (!renderingViewSetup) {
                        app.osmandMap?.setupRenderingView()
                    }
                    mapView?.refreshMap(false)
                    dashboard.updateLocation(true, true, false)
                }
            }
            myApplication.checkApplicationIsBeingInitialized(initListener)
        } else {
            app.osmandMap?.setupRenderingView()
        }
    }

    private fun addListenersForRouting() {
        app.routingHelper?.addCalculationProgressListener(routeCalculationProgressListener)
        app.transportRoutingHelper?.setProgressBar(object :
            TransportRouteCalculationProgressCallback {
            override fun start() {
                routeCalculationProgressListener.onCalculationStart()
            }

            override fun updateProgress(progress: Int) {
                routeCalculationProgressListener.onUpdateCalculationProgress(progress)
            }

            override fun finish() {
                routeCalculationProgressListener.onCalculationFinish(null)
            }
        })
    }

    override fun startActivity(intent: Intent) {
        clearPrevActivityIntent()
        super.startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        // for voice navigation. Lags behind routingAppMode changes, hence repeated under onCalculationFinish()
        val routingAppMode = routingHelper?.appMode
        if (routingAppMode != null) {
            appSettings?.let {
                volumeControlStream = it.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode)
            }
        }
        applicationModeListener =
            StateChangedListener { prevAppMode: ApplicationMode ->
                app.runInUIThread {
                    if (appSettings?.APPLICATION_MODE?.get() !== prevAppMode) {
                        appSettings?.setLastKnownMapElevation(prevAppMode, mapView!!.elevationAngle)
                        updateApplicationModeSettings()
                    }
                }
            }
        applicationModeListener?.let {
            appSettings?.APPLICATION_MODE?.addListener(it)
        }
        updateApplicationModeSettings()


        // if destination point was changed try to recalculate route
        val targets = app.targetPointsHelper
        val routingHelper = app.routingHelper
        if (routingHelper?.isFollowingMode == true
            && (!Algorithms.objectEquals(
                targets?.pointToNavigate?.point,
                routingHelper.finalLocation
            )
                    || !Algorithms.objectEquals(
                targets?.intermediatePointsLatLonNavigation,
                routingHelper.intermediatePoints
            )
                    )
        ) {
            targets?.updateRouteAndRefresh(true)
        }
        app.locationProvider?.resumeAllUpdates()
        val mapView = mapView
        appSettings?.MAP_ACTIVITY_ENABLED?.set(true)
        PluginsHelper.checkInstalledMarketPlugins(app, this)
        PluginsHelper.onMapActivityResume(this)
        mapView?.refreshMap(true)
        mapViewWithLayers.onResume()
        app.downloadThread.setUiActivity(this)

        routingHelper?.addListener(this)
        app.mapMarkersHelper?.addListener(this)

        mapView?.setOnDrawMapListener(this)
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        if (isTopResumedActivity) {
            PluginsHelper.onMapActivityResumeOnTop(this)
        }
    }

    override fun onDrawOverMap() {
        mapView?.setOnDrawMapListener(null)
    }

    override fun onStart() {
        super.onStart()
        stopped = false
        myApplication.notificationHelper?.showNotifications()
    }

    override fun onStop() {
        myApplication.notificationHelper?.removeNotifications(true)
        stopped = true
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapLayers?.setMapActivity(null)
        mapView?.mapActivity = null
        unregisterReceiver(sdCardBroadcastReceiver)
        app.aidlApi.onDestroyMapActivity(this)
        PluginsHelper.onMapActivityDestroy(this)
        myApplication.unsubscribeInitListener(initListener)
        mapViewWithLayers.onDestroy()
        isActivityDestroyed = true
        app.routingHelper?.removeCalculationProgressListener(routeCalculationProgressListener)
    }

    val mapLocation: LatLon?
        get() = mapViewTrackingUtilities?.mapLocation
    val mapRotate: Float
        get() = mapView?.rotate ?: 0f

    // Duplicate methods to OsmAndApplication
    val pointToNavigate: TargetPoint?
        get() = app.targetPointsHelper?.pointToNavigate

    val routingHelper: RoutingHelper?
        get() = app.routingHelper

    override fun onPause() {
        super.onPause()
        appSettings?.LAST_MAP_ACTIVITY_PAUSED_TIME?.set(System.currentTimeMillis())
        onPauseActivity()
    }

    private fun onPauseActivity() {
        mapView?.animatedDraggingThread?.stopAnimatingSync()
        val mapView = mapView
        mapView?.setOnDrawMapListener(null)
        app.mapMarkersHelper?.removeListener(this)
        app.routingHelper?.removeListener(this)
        app.downloadThread.resetUiActivity(this)
        mapViewWithLayers.onPause()

        app.locationProvider?.pauseAllUpdates()
        applicationModeListener?.let {
            appSettings?.APPLICATION_MODE?.removeListener(it)
        }
        if (mapView != null) {
            mapViewModel.saveMapLastKnownLocation(LatLon(mapView.latitude, mapView.longitude), mapView.zoom)
        }
        appSettings?.MAP_ACTIVITY_ENABLED?.set(false)
        app.resourceManager?.interruptRendering()
        PluginsHelper.onMapActivityPause(this)
    }

    fun updateApplicationModeSettings() {
        updateMapSettings()
        app.poiFilters?.loadSelectedPoiFilters()
        mapViewTrackingUtilities?.appModeChanged()
        val mapView = mapView
        val mapLayers = mapLayers
        mapLayers?.updateLayers(this)
        mapView?.setMapDensity(mapView.settingsMapDensity)
        mapView?.refreshMap(true)
        app.appCustomization.updateMapMargins(this)
        dashboard.onAppModeChanged()
    }

    private fun updateMapSettings() {
        if (!app.isApplicationInitializing) {
            val task = UpdateVectorRendererAsyncTask(app) { _ -> true }
            task.executeOnExecutor(singleThreadExecutor)
        }
    }

    fun refreshMap() {
        mapView?.refreshMap()
    }

    fun updateLayers() {
        mapLayers?.updateLayers(this)
    }

    fun refreshMapComplete() {
        myApplication.resourceManager?.renderer?.clearCache()
        updateMapSettings()
        mapView?.refreshMap(true)
    }

    val layout: View
        get() = window.decorView.findViewById(android.R.id.content)

    fun setMargins(leftMargin: Int, topMargin: Int, rightMargin: Int, bottomMargin: Int) {
        val layout: View = layout
        val params = layout.layoutParams
        if (params is ViewGroup.MarginLayoutParams) {
            params.setMargins(leftMargin, topMargin, rightMargin, bottomMargin)
        }
    }

    override fun onUpdatedIndexesList() {
        refreshMap()
    }

    override fun downloadInProgress() {}

    override fun downloadHasFinished() {
        refreshMapComplete()
    }

    override fun onMapMarkerChanged(mapMarker: MapMarker) {
        refreshMap()
    }

    override fun onMapMarkersChanged() {
        refreshMap()
    }

    private fun setKeepScreenOnDuringNavigation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel.uiState.collect { uiState ->
                    val isCalculatingRoute = (uiState.screenState as? ScreenState.PlanningRoute)?.isCalculating == true

                    if (isCalculatingRoute || uiState.isNavigating) {
                        if (app.settingsRepository.getScreenAlwaysOn()) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
    }

    override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean>) {
        refreshMap()
        val rh = app.routingHelper
        if (app.settings?.simulateNavigation == true) {
            val sim = app.locationProvider?.locationSimulation
            if (newRoute && rh?.isFollowingMode == true && sim?.isRouteAnimating == false) {
                sim.startStopRouteAnimation(this)
            }
        }
    }

    override fun routeWasCancelled() = Unit

    override fun routeWasFinished() {
        mapViewModel.onDestinationReached(onMapCloseClicked)
    }

    fun checkMissingRegion(latLon: LatLon?) {
        mapViewModel.checkMissingRegion(latLon)
    }

    fun registerActivityResultListener(listener: ActivityResultListener) {
        activityResultListeners.add(listener)
    }

    override fun onOsmAndSettingsCustomized() {
        restart()
    }

    fun restart() {
        if (!stopped) {
            recreate()
        }
    }

    companion object {
        const val INTENT_KEY_PARENT_MAP_ACTIVITY = "intent_parent_map_activity_key"
        const val INTENT_PARAMS = "intent_prarams"
        private var prevActivityIntent: Intent? = null

        @JvmOverloads
        fun launchMapActivityMoveToTop(
            activity: Context,
            prevIntentParams: Bundle? = null,
            intentData: Uri? = null,
            intentParams: Bundle? = null
        ) {
            if (activity is MapActivity) {
                if (activity.dashboard.isVisible) {
                    activity.dashboard.hideDashboard()
                }
            } else {
                var additionalFlags = 0
                if (activity is Activity) {
                    val intent = activity.intent
                    if (intent != null) {
                        prevActivityIntent = Intent(intent)
                        if (prevIntentParams != null) {
                            prevActivityIntent?.putExtra(INTENT_PARAMS, prevIntentParams)
                            prevActivityIntent?.putExtras(prevIntentParams)
                        }
                        prevActivityIntent?.putExtra(INTENT_KEY_PARENT_MAP_ACTIVITY, true)
                    } else {
                        prevActivityIntent = null
                    }
                } else {
                    prevActivityIntent = null
                    additionalFlags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val newIntent = Intent(
                    activity, (activity.applicationContext as OsmandApplication)
                        .appCustomization.mapActivity
                )
                newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP or additionalFlags)
                if (intentData != null) {
                    newIntent.action = Intent.ACTION_VIEW
                    newIntent.data = intentData
                }
                if (intentParams != null) {
                    newIntent.putExtra(INTENT_PARAMS, intentParams)
                    newIntent.putExtras(intentParams)
                }
                AndroidUtils.startActivityIfSafe(activity, newIntent)
            }
        }

        fun clearPrevActivityIntent() {
            prevActivityIntent = null
        }
    }

    override fun onMapGestureDetected(latLon: LatLon, zoom: Int, moved: Boolean) {
        mapViewModel.onMapGestureDetected(latLon, zoom, moved)
    }
}

private fun MapViewModel.updateNavigationProperties(routingHelper: RoutingHelper) {
    val nextRouteDirectionInfo = routingHelper.getNextRouteDirectionInfo(RouteCalculationResult.NextDirectionInfo(), false)
    updateNavigationProperties(
        estimatedRouteDistance = routingHelper.leftDistance,
        estimatedRouteTime = routingHelper.leftTime,
        estimatedNextTurnDistance = nextRouteDirectionInfo?.distanceTo ?: 0,
        routeDirections = routingHelper.routeDirections,
        getLocation = routingHelper::getNextLocationFromRouteDirection,
    )
}
