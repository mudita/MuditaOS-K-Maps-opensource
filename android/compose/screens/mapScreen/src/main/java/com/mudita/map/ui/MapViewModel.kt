package com.mudita.map.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.map.common.di.DispatcherQualifier
import com.mudita.map.common.geocode.GeocodingAddress
import com.mudita.map.common.maps.GetMissingMapsUseCase
import com.mudita.map.common.maps.OnMapsReIndexedUseCase
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.model.navigation.NavigationPoint
import com.mudita.map.common.model.navigation.NavigationPointType
import com.mudita.map.common.model.navigation.activated
import com.mudita.map.common.model.navigation.getLatLons
import com.mudita.map.common.model.routing.RouteDirectionInfo
import com.mudita.map.common.navigation.IntermediatePointReachedUseCase
import com.mudita.map.common.navigation.StopVoiceRouterUseCase
import com.mudita.map.common.repository.SettingsRepository
import com.mudita.map.common.repository.geocoding.GeocodingRepository
import com.mudita.map.common.sharedPrefs.AppFirstRunPreference
import com.mudita.map.common.sharedPrefs.MapTypesPreference
import com.mudita.map.common.sharedPrefs.SetMapLastKnownLocationUseCase
import com.mudita.map.common.utils.ChangeMapRotationModeUseCase
import com.mudita.map.common.utils.DESCRIPTION_SEPARATOR
import com.mudita.map.common.utils.INTERMEDIATE_POINTS_MAX
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.map.common.utils.VibrationFeedbackManager
import com.mudita.map.common.utils.formattedCoordinates
import com.mudita.map.common.utils.getDefaultLanguage
import com.mudita.map.common.utils.getTypeName
import com.mudita.map.common.utils.round
import com.mudita.map.repository.HistoryRepository
import com.mudita.map.repository.NavigationDisplayMode
import com.mudita.map.repository.NavigationModeItem
import com.mudita.map.repository.NavigationPointItem
import com.mudita.map.ui.routeCalculation.RouteCalculationState
import com.mudita.map.ui.routePlanning.MissingMapsState
import com.mudita.map.ui.routePlanning.RoutePlanningBottomSheetType
import com.mudita.map.usecase.CreateNavigationDirectionUseCase
import com.mudita.map.usecase.CheckMemoryExceededUseCase
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import com.mudita.myplaces.repository.MyPlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.Location
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.router.errors.RouteCalculationError
import net.osmand.util.MapUtils

private const val NAVIGATION_ROUTE_UPDATE_INTERVAL = 2000L

private const val LAUNCHED_NAVIGATION_ITEM = "current_nav_item"
private const val SELECTED_NAVIGATION_MODE = "selected_navigation_mode"
private const val NAV_ARG_NAVIGATION_ITEM = "nav_arg_navigation_item"
private const val NAV_ARG_SEARCH_ITEM = "nav_arg_search_item"
private const val PIN_SEARCH_ITEM = "pin_search_item"
private const val INITIAL_LOCATION_CHECK_COMPLETE = "initial_location_check_complete"

@HiltViewModel
class MapViewModel @Inject constructor(
    private val myPlacesRepository: MyPlacesRepository,
    private val geocodingRepository: GeocodingRepository,
    private val mapTypesPreference: MapTypesPreference,
    private val appFirstRunPreference: AppFirstRunPreference,
    private val osmAndFormatter: OsmAndFormatter,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val vibrationFeedbackManager: VibrationFeedbackManager,
    private val savedStateHandle: SavedStateHandle,
    private val intermediatePointReachedUseCase: IntermediatePointReachedUseCase,
    private val stopVoiceRouterUseCase: StopVoiceRouterUseCase,
    private val getMissingMapsUseCase: GetMissingMapsUseCase,
    private val onMapsReIndexedUseCase: OnMapsReIndexedUseCase,
    private val setMapLastKnownLocationUseCase: SetMapLastKnownLocationUseCase,
    private val changeMapRotationModeUseCase: ChangeMapRotationModeUseCase,
    private val checkMemoryExceededUseCase: CheckMemoryExceededUseCase,
    @DispatcherQualifier.IO private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val onMapGestureEvent = MutableSharedFlow<Pair<LatLon, Int>>(replay = 1)

    private var detectReachedIntermediatePointsJob: Job? = null

    val uiState = MutableStateFlow(UiState())

    val routeState = MutableStateFlow(RouteState())

    val nextSteps: StateFlow<NextSteps?> = routeState
        .map { state ->
            if (state.navigationSteps.isNotEmpty()) {
                NextSteps(state.navigationSteps[0], state.navigationSteps.getOrNull(1))
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val createNavigationDirection = CreateNavigationDirectionUseCase()

    var selectedNavigationModeItem: NavigationModeItem
        get() = savedStateHandle[SELECTED_NAVIGATION_MODE] ?: NavigationModeItem.Walking
        private set(value) {
            savedStateHandle[SELECTED_NAVIGATION_MODE] = value
        }

    private val latLonsToProcess = MutableSharedFlow<LatLon?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private var itemAddressSearchJob: Job? = null
    private var routeCalculationTimeoutJob: Job? = null
    private var routeCalculationMemoryJob: Job? = null

    init {
        checkIfAppOpenedForFirstTime()
        restoreNavigationItem()
        updateMissingMapsOnMapsReIndex()
        adaptMapRotationToScreenState()
        observeSearchResultCancelled()
    }

    private fun adaptMapRotationToScreenState() {
        uiState
            .map { it.screenState is ScreenStateWithCenterButton }
            .distinctUntilChanged()
            .onEach { isMapRotationEnabled ->
                changeMapRotationModeUseCase(
                    mapRotationEnabled = isMapRotationEnabled,
                    isWalkingNavigation = selectedNavigationModeItem == NavigationModeItem.Walking,
                )
            }
            .launchIn(viewModelScope)
    }

    private fun restoreNavigationItem() {
        val existingNavigationItemKey =
            when {
                savedStateHandle.contains(LAUNCHED_NAVIGATION_ITEM) -> LAUNCHED_NAVIGATION_ITEM
                savedStateHandle.contains(NAV_ARG_NAVIGATION_ITEM) -> NAV_ARG_NAVIGATION_ITEM
                else -> return
            }
        setNavigationItem(savedStateHandle[existingNavigationItemKey])
    }

    private fun checkIfAppOpenedForFirstTime() {
        if (appFirstRunPreference.isAppOpenedFirstTime()) {
            uiState.update {
                it.copy(screenState = ScreenState.WelcomeToMaps)
            }
        }
    }

    fun closeWelcomeScreen(shouldShowLocationSharingDialog: Boolean) {
        appFirstRunPreference.setAppOpened()
        val dialogType = if (shouldShowLocationSharingDialog) {
            ScreenState.Idle.DialogType.LocationSharing
        } else {
            ScreenState.Idle.DialogType.None
        }
        uiState.update {
            it.copy(screenState = ScreenState.Idle(dialogType = dialogType))
        }
    }

    fun initSettings() {
        uiState.update { it.copy(isSoundEnabled = settingsRepository.getSoundEnabled()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun detectReachedIntermediatePoints() {
        cancelDetectReachedIntermediatePointsJob()
        detectReachedIntermediatePointsJob = savedStateHandle.getStateFlow<NavigationItem?>(LAUNCHED_NAVIGATION_ITEM, null)
            .filterNotNull()
            .transformLatest<NavigationItem, Unit> { navigationItem ->
                val intermediatePoints = navigationItem.intermediatePoints.iterator()
                intermediatePointReachedUseCase().collect {
                    if (intermediatePoints.hasNext()) {
                        uiState.update { state ->
                            state.copy(
                                screenState = ScreenState.StopPoint(
                                    address = intermediatePoints.next().address,
                                    isCenterButtonVisible = state.showCenterNavigationButton
                                )
                            )
                        }
                        enablePointReachedWindowAutoClose()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun getMyPlaces(item: SearchItem): MyPlaceItem? {
        return withContext(ioDispatcher) {
            val myPlaces = myPlacesRepository.getMyPlaces().getOrNull()
            val itemLat = item.latLon.latitude.round(5)
            val itemLng = item.latLon.longitude.round(5)
            val foundItem = myPlaces?.find { place -> place.lat == itemLat && place.lng == itemLng }
            foundItem
        }
    }

    fun deleteMyPlace(myPlaceItem: MyPlaceItem) = viewModelScope.launch {
        myPlacesRepository
            .deleteMyPlace(myPlaceItem)
            .onSuccess {
                routeState.update { it.copy(myPlaceItem = null) }
            }
    }

    fun prepareForNavigation(item: NavigationItem, myLocation: LatLon?) {
        savedStateHandle[LAUNCHED_NAVIGATION_ITEM] = item
        detectReachedIntermediatePoints()
        uiState.update { it.copy(isNavigationBackHandlerEnabled = true) }
        routeState.update { it.copy(searchItem = null, navigationItem = null) }
        item.startPoint?.let {
            addNavPointToHistory(it, myLocation)
        }
        item.intermediatePoints.forEach {
            addNavPointToHistory(it, myLocation)
        }
        item.endPoint?.let {
            addNavPointToHistory(it, myLocation)
        }
    }

    fun onLongPressed(latLon: LatLon): Boolean {
        val screenState = uiState.value.screenState
        if (
            screenState !is ScreenState.Idle && screenState !is ScreenState.SearchResult &&
            screenState !is ScreenState.MissingRegionOverlay
        ) return false

        setSearchItem(
            SearchItem(
                id = UUID.randomUUID(),
                latLon = latLon,
                itemType = SearchItemType.ADDRESS,
                distance = routeState.value.currentLocation?.let { calculateDistance(it, latLon) },
            )
        )

        return true
    }

    fun setCurrentLocationToNavigationItem(latLon: LatLon): NavigationItem {
        val navigationItem = routeState.value.navigationItem ?: NavigationItem()
        val updatedNavigationItem = updateNavigationItem(navigationItem, latLon, "", isCurrentLocation = true)
        setNavigationItem(updatedNavigationItem)
        return updatedNavigationItem
    }

    private fun updateNavigationItem(
        navigationItem: NavigationItem,
        latLon: LatLon,
        address: String,
        isCurrentLocation: Boolean = false
    ): NavigationItem {
        val currentlySelectedPoint = navigationItem.currentlySelectedPoint

        return when (currentlySelectedPoint?.type) {
            NavigationPointType.INTERMEDIATE -> {
                val updatedIntermediatePoints = navigationItem.intermediatePoints.map { navigationPoint ->
                    if (navigationPoint.uuid == currentlySelectedPoint.uuid) {
                        NavigationPoint(
                            latLon,
                            address,
                            type = NavigationPointType.INTERMEDIATE,
                            isActionActive = true,
                            isCurrentLocation = isCurrentLocation
                        )
                    } else {
                        navigationPoint.copy(isActive = true, isActionActive = true, isCurrentLocation = false)
                    }
                }
                NavigationItem(
                    startPoint = navigationItem.startPoint?.copy(isActive = true, isActionActive = navigationItem.endPoint != null),
                    intermediatePoints = updatedIntermediatePoints,
                    endPoint = navigationItem.endPoint?.copy(
                        isActive = true,
                        isActionActive = navigationItem.intermediatePoints.size < INTERMEDIATE_POINTS_MAX,
                    ),
                    currentlySelectedPoint = null,
                )
            }

            NavigationPointType.DESTINATION -> {
                NavigationItem(
                    startPoint = navigationItem.startPoint?.copy(isActive = true, isActionActive = true),
                    intermediatePoints = navigationItem.intermediatePoints.map { it.copy(isActive = true, isActionActive = true) },
                    endPoint = NavigationPoint(
                        latLon = latLon,
                        address = address,
                        type = NavigationPointType.DESTINATION,
                        isActionActive = navigationItem.intermediatePoints.size < INTERMEDIATE_POINTS_MAX,
                        isCurrentLocation = isCurrentLocation,
                    ),
                    currentlySelectedPoint = null,
                )
            }

            else -> {
                NavigationItem(
                    startPoint = NavigationPoint(
                        address = address,
                        latLon = latLon,
                        isActionActive = navigationItem.endPoint != null,
                        isCurrentLocation = isCurrentLocation,
                    ),
                    intermediatePoints = navigationItem.intermediatePoints.map { it.copy(isActive = true, isActionActive = true) },
                    endPoint = navigationItem.endPoint?.copy(
                        isActive = true,
                        isActionActive = navigationItem.intermediatePoints.size < INTERMEDIATE_POINTS_MAX,
                    ),
                    currentlySelectedPoint = null,
                )
            }
        }
    }

    fun resetState() {
        uiState.update {
            it.copy(
                screenState = ScreenState.Idle(),
                navigationDisplayMode = NavigationDisplayMode.Map,
            )
        }

        routeState.update {
            it.copy(
                finalLocation = null,
                navigationSteps = emptyList(),
                estimatedRouteTime = null,
                estimatedRouteDistance = null,
                searchItem = null,
                navigationItem = null,
                myPlaceItem = null,
                missingMaps = emptyList(),
            )
        }

        savedStateHandle.remove<NavigationItem>(LAUNCHED_NAVIGATION_ITEM)
        cancelDetectReachedIntermediatePointsJob()
    }

    private fun setSearchItem(item: SearchItem?) {
        cancelItemAddressSearchJob()

        if (item == null) {
            routeState.update { it.copy(searchItem = null) }
            return
        }
        val location = routeState.value.currentLocation
        val itemWithDistance = item.copy(
            distance = item.distance ?: location?.let { calculateDistance(it, item.latLon) }
        )

        val updateState: (suspend (SearchItem) -> Unit) = { searchItem: SearchItem ->
            routeState.update {
                it.copy(navigationItem = null, myPlaceItem = getMyPlaces(searchItem), searchItem = searchItem)
            }
            uiState.update { it.copy(screenState = ScreenState.SearchResult(searchItem = searchItem)) }
        }
        val poiDescriptionWithAddress: ((GeocodingAddress) -> String) = { address ->
            val description: String? = itemWithDistance.desc
            val addressDetails: String? = address.streetWithBuildingNumber.takeUnless { address.street.isBlank() }
            listOfNotNull(description, addressDetails).joinToString(separator = DESCRIPTION_SEPARATOR)
        }

        itemAddressSearchJob = viewModelScope.launch(ioDispatcher) {
            updateState(itemWithDistance)

            when (itemWithDistance.itemType) {
                SearchItemType.POI -> {
                    geocodingRepository.searchAddress(itemWithDistance.latLon)
                        .onSuccess { address ->
                            updateState(itemWithDistance.copy(desc = poiDescriptionWithAddress(address)))
                        }
                }

                SearchItemType.ADDRESS -> {
                    geocodingRepository.searchAddress(itemWithDistance.latLon)
                        .onSuccess { address ->
                            updateState(
                                itemWithDistance.copy(
                                    localName = address.streetWithBuildingNumber.takeUnless { address.street.isBlank() },
                                    desc = address.city,
                                )
                            )
                        }
                }

                else -> Unit
            }
        }
    }

    fun clearSearchItem() {
        setSearchItem(null)
        uiState.update { it.copy(screenState = ScreenState.Idle()) }
    }

    fun onBackstackNavigationItemsChanged(navigationItem: NavigationItem?, searchItem: SearchItem?) {
        if (savedStateHandle.get<NavigationItem>(NAV_ARG_NAVIGATION_ITEM) != navigationItem) {
            savedStateHandle[NAV_ARG_NAVIGATION_ITEM] = navigationItem
            setNavigationItem(navigationItem)
        }
        if (savedStateHandle.get<SearchItem>(NAV_ARG_SEARCH_ITEM) != searchItem) {
            savedStateHandle[NAV_ARG_SEARCH_ITEM] = searchItem
            setSearchItem(searchItem)
        }
    }

    fun wasPinAdded(searchItem: SearchItem): Boolean {
        val isTheSameItem = savedStateHandle.get<SearchItem>(PIN_SEARCH_ITEM) == searchItem
        if (!isTheSameItem) savedStateHandle[PIN_SEARCH_ITEM] = searchItem
        return isTheSameItem
    }

    fun setNavigationItem(item: NavigationItem?) {
        if (item != null) {
            routeState.update { it.copy(searchItem = null) }
            uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = item,
                        selectedNavigationMode = selectedNavigationModeItem,
                        bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                        missingMapsState = MissingMapsState.Checking,
                    )
                )
            }
            updateMissingMaps(item)
        }
        routeState.update { it.copy(navigationItem = item) }
    }

    fun shouldShowInitialNoLocationError(navigationItem: NavigationItem): Boolean {
        if (savedStateHandle.get<Boolean>(INITIAL_LOCATION_CHECK_COMPLETE) == true) return false
        savedStateHandle[INITIAL_LOCATION_CHECK_COMPLETE] = true
        return navigationItem.startPoint?.latLon == null
    }

    private fun updateMissingMaps(item: NavigationItem) {
        if (item.startPoint?.latLon != null && item.endPoint?.latLon != null) {
            viewModelScope.launch {
                getMissingMapsUseCase(item.getLatLons())
                    .onSuccess { missingMapFileNames ->
                        if (missingMapFileNames.isNotEmpty()) {
                            onMapsMissing(missingMapFileNames)
                        } else {
                            clearMissingMaps()
                        }
                    }
                    .onFailure {
                        clearMissingMaps()
                    }
            }
        } else {
            clearMissingMaps()
        }
    }

    private fun updateMissingMapsOnMapsReIndex() {
        viewModelScope.launch {
            uiState
                .collectLatest { state ->
                    val screenState = state.screenState
                    if (screenState is ScreenState.PlanningRoute &&
                        screenState.missingMapsState is MissingMapsState.MissingMapsFound
                    ) {
                        onMapsReIndexedUseCase()
                            .collectLatest { updateMissingMaps(screenState.navigationItem) }
                    }
                }
        }
    }

    private fun onMapsMissing(missingMaps: List<String>) {
        routeState.update { it.copy(missingMaps = missingMaps) }

        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRoute ->
            uiState.update {
                it.copy(
                    screenState = planningRoute.copy(
                        missingMapsState = if (missingMaps.isNotEmpty()) {
                            MissingMapsState.MissingMapsFound(missingMaps)
                        } else {
                            MissingMapsState.Idle
                        }
                    )
                )
            }
        }
    }

    private fun clearMissingMaps() {
        routeState.update { it.copy(missingMaps = emptyList()) }

        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRoute ->
            uiState.update {
                it.copy(
                    screenState = planningRoute.copy(
                        missingMapsState = MissingMapsState.Idle
                    )
                )
            }
        }
    }

    fun activateNavigationItemPoints(onActivated: (NavigationItem) -> Unit = {}) {
        val item = checkNotNull(routeState.value.navigationItem?.activated()) { "Expected navigationItem to be not null, but was null" }
        onActivated(item)
        routeState.update { it.copy(navigationItem = item.copy(currentlySelectedPoint = null)) }
        uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = item.copy(currentlySelectedPoint = null),
                    selectedNavigationMode = selectedNavigationModeItem,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = if (routeState.value.missingMaps.isNotEmpty()) {
                        MissingMapsState.MissingMapsFound(routeState.value.missingMaps)
                    } else {
                        MissingMapsState.Idle
                    },
                )
            )
        }
    }

    fun setCurrentLocation(myLocation: LatLon?) {
        routeState.update { it.copy(currentLocation = myLocation) }
    }

    fun setFinalAndCurrentLocation(myLocation: LatLon?, targetLatLon: LatLon) {
        if (myLocation != null) {
            routeState.update {
                it.copy(currentLocation = myLocation, finalLocation = targetLatLon)
            }
        } else {
            updatePlanRouteStateOnCurrentLocation(null)
        }
    }

    fun checkIfRouteIsTooLong(navigationItem: NavigationItem): Boolean {
        val mode = (uiState.value.screenState as? ScreenState.PlanningRoute)?.selectedNavigationMode
        return when (mode) {
            NavigationModeItem.Walking ->
                checkIfRouteIsTooLong(
                    navigationItem = navigationItem,
                    pointsDistanceLimit = WALKING_NAVIGATION_POINTS_DISTANCE_METERS_LIMIT,
                    routeDistanceLimit = WALKING_NAVIGATION_ROUTE_DISTANCE_METERS_LIMIT,
                )

            NavigationModeItem.Cycling ->
                checkIfRouteIsTooLong(
                    navigationItem = navigationItem,
                    pointsDistanceLimit = CYCLING_NAVIGATION_POINTS_DISTANCE_METERS_LIMIT,
                    routeDistanceLimit = CYCLING_NAVIGATION_ROUTE_DISTANCE_METERS_LIMIT,
                )

            else -> false
        }
    }

    private fun checkIfRouteIsTooLong(navigationItem: NavigationItem, pointsDistanceLimit: Double, routeDistanceLimit: Double): Boolean {
        // If start or end point is null we can't check route length
        val startLatLon: LatLon = navigationItem.startPoint?.latLon ?: return false
        val endLatLon: LatLon = navigationItem.endPoint?.latLon ?: return false

        if (navigationItem.intermediatePoints.isEmpty()) {
            return MapUtils.getDistance(startLatLon, endLatLon) > pointsDistanceLimit
        } else {
            val latLons: List<LatLon> = navigationItem.getLatLons()

            var routeDistance = 0.0
            for (i in 0 until latLons.size - 1) {
                val distanceBetweenPoints = MapUtils.getDistance(latLons[i], latLons[i+1])
                routeDistance += distanceBetweenPoints

                if (distanceBetweenPoints > pointsDistanceLimit || routeDistance > routeDistanceLimit) return true
            }

            return false
        }
    }

    fun onRouteLoading() {
        if (!uiState.value.isNavigating) {
            val navigationItem: NavigationItem = savedStateHandle[LAUNCHED_NAVIGATION_ITEM] ?: return
            routeState.update { it.copy(searchItem = null) }
            uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = navigationItem,
                        selectedNavigationMode = selectedNavigationModeItem,
                        bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                        missingMapsState = MissingMapsState.Idle,
                        routeCalculationState = RouteCalculationState.InProgress(stage = RouteCalculationState.InProgress.Stage.Started),
                    )
                )
            }

            cancelRouteCalculationTimeoutJob()
            routeCalculationTimeoutJob = viewModelScope.launch {
                delay(NAVIGATION_CALCULATION_FIRST_TIME_LIMIT_MILLIS)
                showLongRouteCalculationAlert()
                delay(getRouteCalculationTimeLimit(selectedNavigationModeItem))
                val isCalculating = (uiState.value.screenState as? ScreenState.PlanningRoute)?.isCalculating == true
                if (isCalculating) {
                    goBackToPlanRoute(calculationError = RouteCalculationError.RouteIsTooComplex("Route calculation time limit exceeded"))
                }
            }
            cancelRouteCalculationMemoryJob()
            routeCalculationMemoryJob = viewModelScope.launch(ioDispatcher) {
                val memoryExceeded = checkMemoryExceededUseCase()
                if (memoryExceeded) goBackToPlanRoute(
                    calculationError = RouteCalculationError.RouteIsTooComplex("Route calculation memory limit exceeded")
                )
            }
        }
    }

    private fun getRouteCalculationTimeLimit(mode: NavigationModeItem): Long {
        return when (mode) {
            NavigationModeItem.Walking -> WALKING_NAVIGATION_CALCULATION_TIME_LIMIT_MILLIS
            NavigationModeItem.Cycling -> CYCLING_NAVIGATION_CALCULATION_TIME_LIMIT_MILLIS
            NavigationModeItem.Driving -> DRIVING_NAVIGATION_CALCULATION_TIME_LIMIT_MILLIS
        }
    }

    private fun showLongRouteCalculationAlert() {
        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRouteState ->
            uiState.update {
                it.copy(
                    screenState = planningRouteState.copy(
                        routeCalculationState = RouteCalculationState.InProgress(stage = RouteCalculationState.InProgress.Stage.Alert)
                    )
                )
            }
        }
    }

    fun onContinueRouteCalculationClick() {
        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRouteState ->
            uiState.update {
                it.copy(
                    screenState = planningRouteState.copy(
                        routeCalculationState = RouteCalculationState.InProgress(stage = RouteCalculationState.InProgress.Stage.Continued)
                    )
                )
            }
        }
    }
    
    fun onDismissRouteCalculationError() {
        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRouteState ->
            uiState.update { 
                it.copy(screenState = planningRouteState.copy(routeCalculationState = RouteCalculationState.NotStarted))
            }
        }
    }

    fun onContinueRouteClick() {
        uiState.update {
            it.copy(
                screenState = ScreenState.NavigationInProgress(
                    isCenterButtonVisible = it.showCenterNavigationButton
                )
            )
        }
    }

    fun onSinglePressed(latLon: LatLon, amenity: Amenity? = null) {
        when (val screenState = uiState.value.screenState) {
            is ScreenState.Idle -> {
                if (routeState.value.searchItem != null) {
                    resetState()
                } else if (amenity != null) {
                    selectAmenity(amenity)
                }
            }
            is ScreenState.SearchResult -> resetState()
            is ScreenState.SelectLocation -> selectLocation(screenState, latLon, amenity)
            else -> Unit
        }
    }

    fun checkMissingRegion(latLon: LatLon?) {
        viewModelScope.launch { latLonsToProcess.emit(latLon) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun detectMissingRegions(): Flow<Unit> =
        latLonsToProcess.transformLatest { latLon ->
            emit(latLon)
            emitAll(onMapsReIndexedUseCase().map { latLon })
        }.map { latLon -> latLon?.let { getMissingMapsUseCase(it).getOrNull()?.firstOrNull() } }
            .distinctUntilChanged()
            .map { missingRegion -> setCurrentlyViewedMissingRegion(missingRegion) }

    private fun setCurrentlyViewedMissingRegion(missingRegion: String?) {
        val screenState = uiState.value.screenState
        if (screenState is ScreenState.Idle || screenState is ScreenState.MissingRegionOverlay) {
            uiState.update {
                it.copy(
                    screenState = missingRegion?.let(ScreenState::MissingRegionOverlay)
                        ?: ScreenState.Idle()
                )
            }
        }
    }

    fun dismissMissingRegion() {
        setCurrentlyViewedMissingRegion(null)
    }

    private fun selectAmenity(amenity: Amenity) {
        val (name, typeName) = amenity.getDisplayNameAndType()
        val distance = routeState.value.currentLocation?.let { calculateDistance(it, amenity.location) }

        setSearchItem(
            SearchItem(
                id = UUID.randomUUID(),
                latLon = amenity.location,
                itemType = SearchItemType.POI,
                localName = name,
                typeName = typeName,
                distance = distance,
            )
        )
    }

    private fun Amenity.getDisplayNameAndType(): Pair<String, String> =
        getName(getDefaultLanguage())
            .takeUnless { it.isBlank() }
            ?.let { Pair(it, getTypeName(this)) }
            ?: Pair(getTypeName(this), type.translation)

    private fun selectLocation(screenState: ScreenState.SelectLocation, latLon: LatLon, amenity: Amenity?) {
        viewModelScope.launch {
            val navigationItem = screenState.navigationItem
            val amenityName = amenity?.getDisplayNameAndType()?.first
            val updatedNavigationItem = if (amenityName != null) {
                updateNavigationItem(
                    navigationItem = navigationItem,
                    latLon = latLon,
                    address = amenityName,
                )
            } else {
                geocodingRepository.searchAddress(latLon)
                    .fold(
                        onSuccess = { address ->
                            updateNavigationItem(
                                navigationItem = navigationItem,
                                latLon = latLon,
                                address = address.getDisplayAddress(),
                            )
                        }, onFailure = {
                            updateNavigationItem(
                                navigationItem = navigationItem,
                                latLon = latLon,
                                address = latLon.formattedCoordinates()
                            )
                        }
                    )
            }
            setNavigationItem(updatedNavigationItem)
        }
    }

    private fun GeocodingAddress.getDisplayAddress(): String =
        streetWithBuildingNumber.takeUnless { street.isBlank() } ?: latLon.formattedCoordinates()

    fun setNavigationMode(navigationModeItem: NavigationModeItem? = null) {
        val modeItem = navigationModeItem ?: NavigationModeItem.fromMapType(mapTypesPreference.getMapType())
        navigationModeItem?.toMapType()?.also(mapTypesPreference::setMapType)

        selectedNavigationModeItem = modeItem
        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRouteState ->
            uiState.update {
                it.copy(screenState = planningRouteState.copy(selectedNavigationMode = modeItem))
            }
        }
    }

    fun onNavigationPointSelected(navigationPointItem: NavigationPointItem) {
        val currentNavItem = routeState.value.navigationItem
        val selectedPoint = when (navigationPointItem) {
            is NavigationPointItem.Start -> currentNavItem?.startPoint ?: NavigationPoint(
                null,
                "",
                type = NavigationPointType.START
            )

            is NavigationPointItem.Intermediate -> currentNavItem?.intermediatePoints?.find {
                it.uuid == navigationPointItem.uuid
            } ?: NavigationPoint(
                null,
                "",
                type = NavigationPointType.INTERMEDIATE
            )

            is NavigationPointItem.Destination -> currentNavItem?.endPoint ?: NavigationPoint(
                null,
                "",
                type = NavigationPointType.DESTINATION
            )
        }

        val isStartPoint = selectedPoint.type == NavigationPointType.START
        val isEndPoint = selectedPoint.type == NavigationPointType.DESTINATION
        val navigationItem = currentNavItem?.copy(
            startPoint = currentNavItem.startPoint?.copy(
                isActive = isStartPoint,
                isActionActive = false,
            ),
            intermediatePoints = currentNavItem.intermediatePoints.map { point ->
                point.copy(
                    isActive = point.uuid == selectedPoint.uuid,
                    isActionActive = false,
                )
            },
            endPoint = currentNavItem.endPoint?.copy(
                isActive = isEndPoint,
                isActionActive = false,
            ),
            currentlySelectedPoint = selectedPoint
        )

        routeState.update { it.copy(navigationItem = navigationItem) }
        navigationItem?.let { navItem ->
            uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = navItem,
                        selectedNavigationMode = selectedNavigationModeItem,
                        bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                        missingMapsState = if (routeState.value.missingMaps.isNotEmpty()) {
                            MissingMapsState.MissingMapsFound(routeState.value.missingMaps)
                        } else {
                            MissingMapsState.Idle
                        },
                    )
                )
            }
        }
    }

    fun onNavigationPointActionClicked(navigationPointItem: NavigationPointItem) {
        val currentNavItem = routeState.value.navigationItem
        when (navigationPointItem) {
            is NavigationPointItem.Destination -> {
                val intermediatePoints = currentNavItem
                    ?.intermediatePoints
                    .orEmpty()
                    .map { it.copy(isActive = false) }
                    .toMutableList()

                if (intermediatePoints.size < INTERMEDIATE_POINTS_MAX && navigationPointItem.isActionButtonActive) {
                    val newIntermediatePoint = NavigationPoint(
                        null,
                        "",
                        isActive = true,
                        isActionActive = false,
                        type = NavigationPointType.INTERMEDIATE
                    )
                    intermediatePoints.add(newIntermediatePoint)

                    routeState.update {
                        it.copy(
                            navigationItem = currentNavItem?.copy(
                                startPoint = currentNavItem.startPoint?.copy(isActive = false, isActionActive = false),
                                endPoint = currentNavItem.endPoint?.copy(isActive = false, isActionActive = false),
                                intermediatePoints = intermediatePoints,
                                currentlySelectedPoint = newIntermediatePoint,
                            )
                        )
                    }
                }

                routeState.value.navigationItem?.let { navItem ->
                    uiState.update {
                        it.copy(
                            screenState = ScreenState.PlanningRoute(
                                navigationItem = navItem,
                                selectedNavigationMode = selectedNavigationModeItem,
                                bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                                missingMapsState = if (routeState.value.missingMaps.isNotEmpty()) {
                                    MissingMapsState.MissingMapsFound(routeState.value.missingMaps)
                                } else {
                                    MissingMapsState.Idle
                                },
                            )
                        )
                    }
                }
            }

            is NavigationPointItem.Intermediate -> {
                val intermediatePoints = currentNavItem?.intermediatePoints.orEmpty().toMutableList()
                intermediatePoints.removeIf { it.uuid == navigationPointItem.uuid }

                routeState.update {
                    it.copy(
                        navigationItem = currentNavItem?.copy(
                            endPoint = currentNavItem.endPoint?.copy(isActionActive = intermediatePoints.size < INTERMEDIATE_POINTS_MAX),
                            intermediatePoints = intermediatePoints
                        )
                    )
                }

                setNavigationItem(routeState.value.navigationItem)
            }

            else -> Unit
        }
    }

    fun onDestinationReached(onClose: () -> Unit) {
        val address = savedStateHandle.get<NavigationItem>(LAUNCHED_NAVIGATION_ITEM)?.endPoint?.address ?: return
        uiState.update {
            it.copy(
                screenState = ScreenState.DestinationReached(
                    address = address,
                    isCenterButtonVisible = it.showCenterNavigationButton
                )
            )
        }
        vibrationFeedbackManager.vibrate()
        enablePointReachedWindowAutoClose(onClose)
    }

    private fun enablePointReachedWindowAutoClose(onClose: (() -> Unit)? = null) {
        viewModelScope.launch {
            delay(POINT_REACHED_AUTO_CLOSE_DELAY)
            when (uiState.value.screenState) {
                is ScreenState.DestinationReached -> {
                    resetState()
                    onClose?.invoke()
                }
                is ScreenState.StopPoint -> {
                    uiState.update {
                        it.copy(screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = it.showCenterNavigationButton))
                    }
                }
                else -> Unit
            }
        }
    }

    fun onSoundClicked() {
        val isSoundEnabled = uiState.value.isSoundEnabled

        settingsRepository.saveSoundEnabled(isSoundEnabled.not())

        uiState.update { it.copy(isSoundEnabled = isSoundEnabled.not()) }
    }

    fun onNavigationDisplayModeChanged() {
        val updatedNavigationDisplayMode = when (uiState.value.navigationDisplayMode) {
            is NavigationDisplayMode.Map -> NavigationDisplayMode.Commands
            is NavigationDisplayMode.Commands -> NavigationDisplayMode.Map
        }

        uiState.update { it.copy(navigationDisplayMode = updatedNavigationDisplayMode) }
    }

    fun updateNavigationProperties(
        estimatedRouteDistance: Int,
        estimatedRouteTime: Int,
        estimatedNextTurnDistance: Int,
        routeDirections: List<RouteDirectionInfo>?,
        getLocation: (RouteDirectionInfo) -> Location,
    ) {
        cancelRouteCalculationMemoryJob()
        viewModelScope.launch {
            routeDirections?.take(VISIBLE_INSTRUCTION_COUNT)?.mapNotNull { direction ->
                if (direction.roadDetails.name == null) {
                    async {
                        val location = getLocation(direction)
                        val latLon = LatLon(location.latitude, location.longitude)
                        direction.streetName = geocodingRepository.searchAddress(latLon).getOrNull()?.street
                    }
                } else {
                    null
                }
            }?.awaitAll()

            val screenState = uiState.value.screenState
            val isCalculatingRoute = (screenState as? ScreenState.PlanningRoute)?.isCalculating == true
            val isNavigationInProgress = screenState is ScreenState.NavigationInProgress
            if (!isCalculatingRoute && !isNavigationInProgress) return@launch

            routeState.update {
                it.copy(
                    estimatedRouteDistance = osmAndFormatter.getFormattedDistanceValue(estimatedRouteDistance.toFloat()).formattedValue,
                    estimatedRouteTime = NavigationTime.create(estimatedRouteTime),
                    navigationSteps = NavigationStep.createSteps(estimatedNextTurnDistance.toFloat(), routeDirections.orEmpty(), osmAndFormatter),
                    searchItem = null,
                    missingMaps = emptyList(),
                )
            }

            if (!routeDirections.isNullOrEmpty()) {
                uiState.update {
                    it.copy(
                        screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = it.showCenterNavigationButton)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun navigationRefreshFlow() = uiState
        .transformLatest {
            if (it.screenState !is ScreenState.NavigationInProgress) return@transformLatest
            while (currentCoroutineContext().isActive) {
                emit(Unit)
                delay(NAVIGATION_ROUTE_UPDATE_INTERVAL)
            }
        }

    fun selectNavigationPoint(navigationItem: NavigationItem) {
        val activatedNavigationItem = navigationItem.activated()
        uiState.update { it.copy(screenState = ScreenState.SelectLocation(activatedNavigationItem)) }
        routeState.update { it.copy(navigationItem = activatedNavigationItem) }
    }

    fun showNavigationPointSelection() {
        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRouteState ->
            uiState.update {
                it.copy(screenState = planningRouteState.copy(bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection))
            }
        }
    }

    fun closeNavigationPointSelection() {
        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRouteState ->
            uiState.update {
                it.copy(screenState = planningRouteState.copy(bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection))
            }
        }
    }

    fun updatePlanRouteStateOnCurrentLocation(currentLocation: LatLon?) {
        val isGPSError = currentLocation == null
        val bottomSheetType = if (isGPSError) RoutePlanningBottomSheetType.GPSError else RoutePlanningBottomSheetType.NavigationModeSelection

        (uiState.value.screenState as? ScreenState.PlanningRoute)?.let { planningRouteState ->
            val navigationItem = if (isGPSError) {
                getUpdatedNavigationPointsOnGPSError(planningRouteState.navigationItem)
            } else {
                planningRouteState.navigationItem
            }

            uiState.update {
                it.copy(
                    screenState = planningRouteState.copy(
                        navigationItem = navigationItem,
                        bottomSheetType = bottomSheetType,
                        missingMapsState = if (isGPSError) MissingMapsState.Idle else planningRouteState.missingMapsState,
                    )
                )
            }

            routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                    missingMaps = if (isGPSError) emptyList() else it.missingMaps
                )
            }
        }
    }

    private fun getUpdatedNavigationPointsOnGPSError(navigationItem: NavigationItem): NavigationItem {
        fun NavigationPoint.deactivate() = copy(isActive = false, isActionActive = false)

        return navigationItem.copy(
            startPoint = navigationItem.startPoint?.deactivate(),
            intermediatePoints = navigationItem.intermediatePoints.map { it.deactivate() },
            endPoint = navigationItem.endPoint?.deactivate(),
            currentlySelectedPoint = null
        )
    }

    private fun NavigationPoint.toSearchHistoryEntity(): SearchHistoryEntity? =
        latLon?.let {
            SearchHistoryEntity(
                lat = it.latitude,
                lon = it.longitude,
                searchTime = System.currentTimeMillis(),
                searchQuery = address,
                localName = address,
                iconRes = com.mudita.map.common.R.drawable.icon_search,
                searchCategory = city ?: "",
                itemType = SearchItemType.POI.name,
            )
        }

    private fun calculateDistance(toLoc: LatLon, location: LatLon): Double {
        val mes = floatArrayOf(0f, 0f)
        toLoc.let {
            Location.distanceBetween(
                toLoc.latitude,
                toLoc.longitude,
                location.latitude,
                location.longitude,
                mes
            )
        }
        return mes[0].toDouble()
    }

    private fun addNavPointToHistory(navPoint: NavigationPoint, myLocation: LatLon?) {
        if (!navPoint.city.isNullOrEmpty() && myLocation != null) {
            val searchHistoryEntity = navPoint.toSearchHistoryEntity() ?: return
            viewModelScope.launch(ioDispatcher) {
                historyRepository.addHistoryItem(searchHistoryEntity)
            }
        }
    }

    fun onUnableToMoveToCurrentLocation() {
        uiState.update { it.copy(screenState = ScreenState.Idle(dialogType = ScreenState.Idle.DialogType.GPSError)) }
        routeState.update { it.copy(searchItem = null) }
    }

    fun goBackToPlanRoute(missingMaps: List<String> = routeState.value.missingMaps, calculationError: RouteCalculationError? = null) {
        stopVoiceRouterUseCase()
        val navigationItem: NavigationItem = savedStateHandle[LAUNCHED_NAVIGATION_ITEM] ?: return
        savedStateHandle[LAUNCHED_NAVIGATION_ITEM] = null
        uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = selectedNavigationModeItem,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = if (missingMaps.isNotEmpty()) {
                        MissingMapsState.MissingMapsFound(missingMaps)
                    } else {
                        MissingMapsState.Idle
                    },
                    routeCalculationState = if (calculationError != null) {
                        RouteCalculationState.Error(calculationError)
                    } else {
                        RouteCalculationState.NotStarted
                    },
                ),
                navigationDisplayMode = NavigationDisplayMode.Map,
                isNavigationBackHandlerEnabled = false,
            )
        }

        routeState.update {
            it.copy(
                missingMaps = missingMaps,
                finalLocation = null,
                navigationSteps = emptyList(),
                estimatedRouteTime = null,
                estimatedRouteDistance = null,
                searchItem = null,
                navigationItem = navigationItem,
            )
        }

        cancelDetectReachedIntermediatePointsJob()
    }

    fun onMapGestureDetected(latLon: LatLon, zoom: Int, moved: Boolean) {
        if (moved) setCenterNavigationVisibility(true)
        saveMapLastKnownLocation(latLon, zoom)
    }

    fun saveMapLastKnownLocation(latLon: LatLon, zoom: Int) {
        viewModelScope.launch { onMapGestureEvent.emit(latLon to zoom) }
    }

    fun onCenterNavigationClicked() {
        setCenterNavigationVisibility(false)
    }

    fun showLocationSharingDialog() {
        when (val screenState = uiState.value.screenState) {
            is ScreenState.PlanningRoute -> {
                val updatedNavigationItem = getUpdatedNavigationPointsOnGPSError(screenState.navigationItem)
                uiState.update {
                    it.copy(
                        screenState = screenState.copy(
                            navigationItem = updatedNavigationItem,
                            bottomSheetType = RoutePlanningBottomSheetType.LocationSharing,
                            missingMapsState = MissingMapsState.Idle,
                        )
                    )
                }
                routeState.update {
                    it.copy(
                        navigationItem = updatedNavigationItem,
                        missingMaps = emptyList(),
                    )
                }
            }

            is ScreenState.Idle,
            is ScreenState.SearchResult -> {
                uiState.update {
                    it.copy(screenState = ScreenState.Idle(dialogType = ScreenState.Idle.DialogType.LocationSharing))
                }
                routeState.update {
                    it.copy(searchItem = null, myPlaceItem = null)
                }
            }

            else -> Unit
        }
    }

    fun closeLocationSharingDialog() {
        when (val state = uiState.value.screenState) {
            is ScreenState.Idle -> uiState.update { it.copy(screenState = state.copy(dialogType = ScreenState.Idle.DialogType.None)) }
            is ScreenState.PlanningRoute -> activateNavigationItemPoints()
            else -> Unit
        }
    }

    private fun setCenterNavigationVisibility(isVisible: Boolean) {
        when (val screenState = uiState.value.screenState) {
            is ScreenState.NavigationInProgress -> uiState.update { it.copy(screenState = ScreenState.NavigationInProgress(isVisible)) }
            is ScreenState.StopPoint -> uiState.update { it.copy(screenState = screenState.copy(isCenterButtonVisible = isVisible)) }
            is ScreenState.DestinationReached -> uiState.update { it.copy(screenState = screenState.copy(isCenterButtonVisible = isVisible)) }
            else -> Unit
        }
    }

    private fun cancelDetectReachedIntermediatePointsJob() {
        detectReachedIntermediatePointsJob?.cancel()
        detectReachedIntermediatePointsJob = null
    }

    private fun cancelItemAddressSearchJob() {
        itemAddressSearchJob?.cancel()
        itemAddressSearchJob = null
    }

    fun cancelRouteCalculationTimeoutJob() {
        routeCalculationTimeoutJob?.cancel()
        routeCalculationTimeoutJob = null
    }

    fun cancelRouteCalculationMemoryJob() {
        routeCalculationMemoryJob?.cancel()
        routeCalculationMemoryJob = null
    }

    private fun observeSearchResultCancelled() {
        viewModelScope.launch {
            uiState.collectLatest {
                if (it.screenState !is ScreenState.SearchResult) cancelItemAddressSearchJob()
            }
        }
    }

    suspend fun observeAndSaveMapLocation() {
        observeAndSaveMapLocationFlow().collect()
    }

    fun onInitialLocation(latLon: LatLon) {
        var screenState = uiState.value.screenState
        if (screenState is ScreenState.PlanningRoute) {
            val navigationItem = screenState.navigationItem
            screenState = screenState.copy(
                navigationItem = navigationItem.copy(
                    startPoint =
                    NavigationPoint(latLon = latLon, address = "", isCurrentLocation = true)
                )
            )
            uiState.update { it.copy(screenState = screenState) }
        }
    }

    @OptIn(FlowPreview::class)
    @VisibleForTesting
    suspend fun observeAndSaveMapLocationFlow(): Flow<Unit> =
        onMapGestureEvent
            .sample(SAVE_MAP_LOCATION_SAMPLE_RATE)
            .map { (latLon, zoom) -> setMapLastKnownLocationUseCase(latLon, zoom) }

    companion object {
        const val POINT_REACHED_AUTO_CLOSE_DELAY = 5_000L
        const val SAVE_MAP_LOCATION_SAMPLE_RATE = 1_000L
        private const val VISIBLE_INSTRUCTION_COUNT = 4
        private const val NAVIGATION_CALCULATION_FIRST_TIME_LIMIT_MILLIS = 60000L // 1 minute
        private const val WALKING_NAVIGATION_CALCULATION_TIME_LIMIT_MILLIS = 240000L // 4 minutes
        private const val CYCLING_NAVIGATION_CALCULATION_TIME_LIMIT_MILLIS = 300000L // 5 minutes
        private const val DRIVING_NAVIGATION_CALCULATION_TIME_LIMIT_MILLIS = 360000L // 6 minutes
        private const val WALKING_NAVIGATION_POINTS_DISTANCE_METERS_LIMIT = 40000.0
        private const val WALKING_NAVIGATION_ROUTE_DISTANCE_METERS_LIMIT = 140000.0
        private const val CYCLING_NAVIGATION_POINTS_DISTANCE_METERS_LIMIT = 110000.0
        private const val CYCLING_NAVIGATION_ROUTE_DISTANCE_METERS_LIMIT = 440000.0
    }
}