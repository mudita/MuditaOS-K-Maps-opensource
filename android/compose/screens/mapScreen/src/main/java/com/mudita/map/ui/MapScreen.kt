package com.mudita.map.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.model.navigation.NavigationPoint
import com.mudita.map.common.model.navigation.NavigationPointType
import com.mudita.map.common.model.navigation.updateIfUsesCurrentLocation
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.map.common.utils.conditional
import com.mudita.map.common.utils.formattedCoordinates
import com.mudita.map.missingregionoverlay.ui.MissingRegionOverlayScreen
import com.mudita.map.repository.NavigationDirection
import com.mudita.map.repository.NavigationDisplayMode
import com.mudita.map.repository.NavigationModeItem
import com.mudita.map.repository.NavigationPointItem
import com.mudita.map.searchresult.ui.SearchResultBottomSheet
import com.mudita.map.ui.buttons.CenterNavigationMapButton
import com.mudita.map.ui.buttons.MapButtons
import com.mudita.map.ui.buttons.PickLocationMapButtons
import com.mudita.map.ui.commandView.CommandNavigationView
import com.mudita.map.ui.dialogs.LocationSharingDialog
import com.mudita.map.ui.errors.GPSErrorBottomSheet
import com.mudita.map.ui.intro.MapsWelcomeScreen
import com.mudita.map.ui.pointReached.DestinationReachedScreen
import com.mudita.map.ui.pointReached.PointReachedBottomSheet
import com.mudita.map.ui.pointReached.PointReachedTopSheet
import com.mudita.map.ui.pointReached.StopPointScreen
import com.mudita.map.ui.route.RouteDetailsBottomSheet
import com.mudita.map.ui.route.RouteDirectionsTopSheet
import com.mudita.map.ui.routeCalculation.RouteCalculationErrorScreen
import com.mudita.map.ui.routeCalculation.RouteCalculationScreen
import com.mudita.map.ui.routeCalculation.RouteCalculationState
import com.mudita.map.ui.routePlanning.MissingMapsScreen
import com.mudita.map.ui.routePlanning.MissingMapsState
import com.mudita.map.ui.routePlanning.NavigationDestination
import com.mudita.map.ui.routePlanning.NavigationModeBottomSheet
import com.mudita.map.ui.routePlanning.NavigationPointSelectionBottomSheet
import com.mudita.map.ui.routePlanning.NavigationPointSelectionType
import com.mudita.map.ui.routePlanning.RoutePlanningBottomSheetType
import com.mudita.map.ui.routePlanning.SelectLocation
import com.mudita.map.window.HideStatusBarEffect
import com.mudita.maps.frontitude.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import net.osmand.data.LatLon

@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    searchItem: SearchItem?,
    navigationItem: NavigationItem?,
    mapView: @Composable (modifier: Modifier) -> Unit = {},
    myLocation: () -> LatLon?,
    onMenuClicked: () -> Unit = {},
    onSearchClicked: () -> Unit = {},
    onMyLocationClicked: () -> Unit = {},
    onCenterNavigationClick: () -> Unit = {},
    onZoomInClicked: () -> Unit = {},
    onZoomOutClicked: () -> Unit = {},
    onNavigationPointSelected: (NavigationPointSelectionType, NavigationItem) -> Unit = { _, _ -> },
    onNavigateClicked: (NavigationItem) -> Unit = {},
    onNavigationItemChanged: (NavigationItem) -> Unit = {},
    onNavigationModeChanged: (NavigationModeItem) -> Unit = {},
    onSoundClicked: (Boolean) -> Unit = {},
    onMyPlaceSaved: (MyPlaceItem) -> Unit = {},
    onCloseClicked: () -> Unit = {},
    setNavigationResumed: (Boolean) -> Unit = {},
    cancelCurrentRoute: () -> Unit = {},
    clearAllMarkers: () -> Unit = {},
    fitMapToNavigationItem: (navigationItem: NavigationItem, topBarHeight: Int, bottomBarHeight: Int) -> Unit = { _, _, _ -> },
    applySearchItemToMap: suspend (SearchItem?) -> Unit = {},
    onMissingMapsClick: (List<String>) -> Unit,
    onIntroContinueClick: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    hasLocationPermission: () -> Boolean,
) {
    val uiState by mapViewModel.uiState.collectAsState()
    val routeState by mapViewModel.routeState.collectAsState()
    val nextSteps by mapViewModel.nextSteps.collectAsState()
    val routeNavigationItem = routeState.navigationItem
    val (topBarHeight, setTopBarHeight) = remember { mutableIntStateOf(0) }
    val (bottomBarHeight, setBottomBarHeight) = remember { mutableIntStateOf(0) }

    val screenState: ScreenState = uiState.screenState
    val navigationDisplayMode: NavigationDisplayMode = uiState.navigationDisplayMode

    HideStatusBarEffect(enabled = screenState.requiresHiddenStatusBar())

    LaunchedEffect(true) {
        mapViewModel.initSettings()
        mapViewModel.onBackstackNavigationItemsChanged(navigationItem, searchItem)
        mapViewModel.setNavigationMode()
    }

    LaunchedEffect(Unit) { mapViewModel.detectMissingRegions().collect() }

    LaunchedEffect(key1 = routeNavigationItem) {
        withContext(Dispatchers.IO) {
            if (routeNavigationItem != null) {
                if (screenState is ScreenState.PlanningRoute) {
                    cancelCurrentRoute()
                }
                onNavigationItemChanged(routeNavigationItem)
            } else {
                if (screenState is ScreenState.Idle) {
                    cancelCurrentRoute()
                } else {
                    clearAllMarkers()
                }
            }
        }
    }

    LaunchedEffect(screenState, topBarHeight, bottomBarHeight) {
        if (screenState is ScreenState.PlanningRoute) {
            fitMapToNavigationItem(screenState.navigationItem, topBarHeight, bottomBarHeight)
            if (mapViewModel.shouldShowInitialNoLocationError(screenState.navigationItem)) {
                if (hasLocationPermission()) {
                    mapViewModel.updatePlanRouteStateOnCurrentLocation(null)
                } else {
                    mapViewModel.showLocationSharingDialog()
                }
            }
        }
        setNavigationResumed(screenState !is ScreenState.StopPoint)
    }

    LaunchedEffect(routeState.searchItem) {
        val searchItem = routeState.searchItem ?: return@LaunchedEffect
        if (!mapViewModel.wasPinAdded(searchItem)) {
            applySearchItemToMap(searchItem)
        }
    }

    LaunchedEffect(Unit) { mapViewModel.observeAndSaveMapLocation() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            screenState is ScreenState.WelcomeToMaps -> {
                MapsWelcomeScreen(
                    onContinueClick = onIntroContinueClick,
                )
            }

            screenState is ScreenState.NavigationInProgress && navigationDisplayMode is NavigationDisplayMode.Commands -> {
                CommandNavigationView(
                    navigationSteps = routeState.navigationSteps,
                    onEndRouteClick = { mapViewModel.goBackToPlanRoute() },
                )
            }

            screenState is ScreenState.StopPoint && navigationDisplayMode is NavigationDisplayMode.Commands -> {
                StopPointScreen(
                    destinationName = screenState.address,
                    onEndRouteClick = { mapViewModel.goBackToPlanRoute() },
                    onContinueClick = { mapViewModel.onContinueRouteClick() },
                )
            }

            screenState is ScreenState.DestinationReached && navigationDisplayMode is NavigationDisplayMode.Commands -> {
                DestinationReachedScreen(
                    destinationName = screenState.address,
                    onDoneClick = {
                        mapViewModel.resetState()
                        onCloseClicked()
                    }
                )
            }

            else -> {
                mapView(Modifier)

                MapScreenOverlay(
                    uiState = uiState,
                    nextSteps = nextSteps,
                    onMenuClick = onMenuClicked,
                    onSearchClick = onSearchClicked,
                    onMyLocationClick = onMyLocationClicked,
                    onZoomInClick = onZoomInClicked,
                    onZoomOutClick = onZoomOutClicked,
                    onCenterNavigationClick = onCenterNavigationClick,
                    onNavigationPointSelect = { mapViewModel.onNavigationPointSelected(it) },
                    onShowPointSelectionBottomSheet = { mapViewModel.showNavigationPointSelection() },
                    onNavigationPointActionClick = { navigationPointItem ->
                        mapViewModel.onNavigationPointActionClicked(navigationPointItem)
                    },
                    onEndRouteClick = {
                        mapViewModel.goBackToPlanRoute()
                    },
                    onCancelRouteCalculationClick = { mapViewModel.goBackToPlanRoute() },
                    onContinueRouteCalculationClick = { mapViewModel.onContinueRouteCalculationClick() },
                    onDismissCalculationErrorClick = { mapViewModel.onDismissRouteCalculationError() },
                    onResetClick = { mapViewModel.resetState() },
                    createNavigationDirection = { navigationItem, allInactive ->
                        mapViewModel.createNavigationDirection(
                            navigationItem,
                            allInactive
                        )
                    },
                    setNavigationItem = { mapViewModel.setNavigationItem(it) },
                    setTopBarHeight = setTopBarHeight,
                    bottomBarHeight = bottomBarHeight,
                    onMissingMapsClick = onMissingMapsClick,
                    onCloseLocationSharingDialog = { mapViewModel.closeLocationSharingDialog() },
                    onOpenLocationSettingsClick = onOpenLocationSettingsClick,
                )

                BackHandler(screenState is ScreenState.PlanningRoute) { mapViewModel.resetState() }
            }
        }

        BackHandler(uiState.isNavigationBackHandlerEnabled) {
            mapViewModel.goBackToPlanRoute()
        }

        BackHandler(screenState is ScreenState.SearchResult) {
            mapViewModel.clearSearchItem()
            clearAllMarkers()
        }

        MapScreenBottomSheet(
            mapViewModel = mapViewModel,
            uiState = uiState,
            routeState = routeState,
            myLocation = myLocation,
            onNavigationPointSelect = onNavigationPointSelected,
            onNavigateClick = onNavigateClicked,
            onNavigationModeChange = onNavigationModeChanged,
            onSoundClick = onSoundClicked,
            onSaveMyPlace = onMyPlaceSaved,
            onCloseClick = onCloseClicked,
            onCloseLocationSharingDialog = { mapViewModel.closeLocationSharingDialog() },
            onOpenLocationSettingsClick = onOpenLocationSettingsClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { layoutCoordinates ->
                    setBottomBarHeight(layoutCoordinates.size.height)
                },
        )
    }
}

@Composable
private fun MapScreenOverlay(
    uiState: UiState,
    nextSteps: NextSteps?,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    onCenterNavigationClick: () -> Unit,
    onNavigationPointSelect: (NavigationPointItem) -> Unit,
    onShowPointSelectionBottomSheet: () -> Unit,
    onNavigationPointActionClick: (NavigationPointItem) -> Unit,
    onEndRouteClick: () -> Unit,
    onCancelRouteCalculationClick: () -> Unit,
    onContinueRouteCalculationClick: () -> Unit,
    onDismissCalculationErrorClick: () -> Unit,
    onResetClick: () -> Unit,
    createNavigationDirection: (NavigationItem, Boolean) -> NavigationDirection,
    setNavigationItem: (NavigationItem) -> Unit,
    setTopBarHeight: (Int) -> Unit,
    bottomBarHeight: Int,
    onMissingMapsClick: (List<String>) -> Unit,
    onCloseLocationSharingDialog: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
) {
    if (uiState.showMapButtons) {
        val density = LocalDensity.current.density
        MapButtons(
            onMenuClick = onMenuClick,
            onSearchClick = onSearchClick,
            onMyLocationClick = onMyLocationClick,
            onZoomInClick = onZoomInClick,
            onZoomOutClick = onZoomOutClick,
            isNotInNavigationMode = uiState.isNavigating.not(),
            modifier = Modifier.padding(bottom = (bottomBarHeight / density).dp),
        )
    } else if (uiState.showCenterNavigationButton) {
        val density = LocalDensity.current.density
        CenterNavigationMapButton(
            modifier = Modifier.padding(bottom = (bottomBarHeight / density).dp),
            onCenterClick = onCenterNavigationClick,
        )
    }

    when (val screenState = uiState.screenState) {
        is ScreenState.PlanningRoute -> {
            Column {
                NavigationDestination(
                    navigationDirection = createNavigationDirection(
                        screenState.navigationItem,
                        screenState.bottomSheetType == RoutePlanningBottomSheetType.GPSError
                                || screenState.routeCalculationState != RouteCalculationState.NotStarted,
                    ),
                    onItemClick = {
                        if (it !is NavigationPointItem.Start) {
                            onNavigationPointSelect(it)
                            onShowPointSelectionBottomSheet()
                        }
                    },
                    onItemActionClick = { navigationPointItem ->
                        onNavigationPointActionClick(navigationPointItem)
                        if (navigationPointItem is NavigationPointItem.Destination) {
                            onShowPointSelectionBottomSheet()
                        }
                    },
                    onCancelClick = onResetClick,
                    isCalculatingRoute = screenState.routeCalculationState !is RouteCalculationState.NotStarted,
                    modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                        setTopBarHeight(layoutCoordinates.size.height)
                    }
                )
                
                if (screenState.missingMapsState is MissingMapsState.MissingMapsFound) {
                    MissingMapsScreen(
                        onMissingMapsClick = {
                            onMissingMapsClick(screenState.missingMapsState.missingMaps)
                        },
                    )
                } else {
                    when (val calculationState = screenState.routeCalculationState) {
                        is RouteCalculationState.InProgress -> {
                            RouteCalculationScreen(
                                onCancelClick = onCancelRouteCalculationClick,
                                onContinueClick = onContinueRouteCalculationClick,
                                stage = calculationState.stage,
                                hasIntermediatePoints = screenState.navigationItem.intermediatePoints.isNotEmpty(),
                            )
                            BackHandler { onCancelRouteCalculationClick() }
                        }
                        
                        is RouteCalculationState.Error -> {
                            RouteCalculationErrorScreen(
                                error = calculationState.error,
                                navigationItem = screenState.navigationItem,
                                navigationMode = screenState.selectedNavigationMode,
                                onDismissErrorClick = onDismissCalculationErrorClick,
                            )
                            BackHandler { onDismissCalculationErrorClick() }
                        }

                        else -> Unit
                    }
                }
            }
        }

        is ScreenState.NavigationInProgress -> {
            RouteDirectionsTopSheet(
                nextSteps = nextSteps,
                onEndRouteClick = onEndRouteClick,
            )
        }

        is ScreenState.DestinationReached -> {
            PointReachedTopSheet(
                icon = painterResource(com.mudita.map.common.R.drawable.icon_arrived),
                title = stringResource(R.string.maps_label_youhavearrived),
                address = screenState.address,
            )
        }

        is ScreenState.StopPoint -> {
            PointReachedTopSheet(
                icon = painterResource(com.mudita.map.common.R.drawable.icon_stop_point),
                title = stringResource(R.string.maps_label_stoppoint),
                address = screenState.address,
                iconPadding = PaddingValues(6.dp),
                onEndRouteClick = onEndRouteClick,
            )
        }

        is ScreenState.SelectLocation -> {
            Column {
                PickLocationMapButtons(
                    onBackClick = { setNavigationItem(screenState.navigationItem.copy(currentlySelectedPoint = null)) },
                    onMyLocationClick = onMyLocationClick,
                    onZoomInClick = onZoomInClick,
                    onZoomOutClick = onZoomOutClick,
                    modifier = Modifier.weight(1f),
                )

                SelectLocation()

                BackHandler { setNavigationItem(screenState.navigationItem) }
            }
        }

        is ScreenState.Idle -> {
            when (screenState.dialogType) {
                ScreenState.Idle.DialogType.GPSError -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GPSErrorBottomSheet(
                            onCloseClick = onResetClick,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }

                ScreenState.Idle.DialogType.LocationSharing -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LocationSharingDialog(
                            onOpenSettingsClick = onOpenLocationSettingsClick,
                            onBrowseMapsClick = onCloseLocationSharingDialog,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }

                ScreenState.Idle.DialogType.None -> Unit
            }
        }

        else -> Unit
    }
}

@Composable
private fun MapScreenBottomSheet(
    mapViewModel: MapViewModel,
    uiState: UiState,
    routeState: RouteState,
    myLocation: () -> LatLon?,
    onNavigationPointSelect: (NavigationPointSelectionType, NavigationItem) -> Unit,
    onNavigateClick: (NavigationItem) -> Unit,
    onNavigationModeChange: (NavigationModeItem) -> Unit,
    onSoundClick: (Boolean) -> Unit,
    onSaveMyPlace: (MyPlaceItem) -> Unit,
    onCloseClick: () -> Unit,
    onCloseLocationSharingDialog: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    modifier: Modifier,
) {
    when (val screenState = uiState.screenState) {
        is ScreenState.SearchResult -> {
            SearchResultBottomSheet(
                item = screenState.searchItem,
                myPlaceItem = routeState.myPlaceItem,
                modifier = modifier,
                onSaveMyPlace = onSaveMyPlace,
                onDeleteMyPlace = { mapViewModel.deleteMyPlace(it) },
                onNavigateClick = {
                    val searchItem = screenState.searchItem
                    val targetLatLon = searchItem.latLon
                    val navigationItem = NavigationItem(
                        startPoint = myLocation()?.let { currentLocation ->
                            NavigationPoint(
                                latLon = currentLocation,
                                address = "",
                                isActionActive = true,
                                isCurrentLocation = true,
                            )
                        },
                        endPoint = NavigationPoint(
                            address = (searchItem.address
                                ?: searchItem.localName)?.takeUnless { it.isBlank() }
                                ?: searchItem.latLon.formattedCoordinates(),
                            latLon = LatLon(targetLatLon.latitude, targetLatLon.longitude),
                            type = NavigationPointType.DESTINATION,
                            isActionActive = true,
                        )
                    )
                    mapViewModel.setNavigationItem(navigationItem)
                },
            )
        }

        is ScreenState.PlanningRoute -> {
            when (screenState.bottomSheetType) {
                RoutePlanningBottomSheetType.NavigationModeSelection -> {
                    if (screenState.shouldShowNavigationModeSelection) {
                        NavigationModeBottomSheet(
                            selectedNavigationModeItem = screenState.selectedNavigationMode,
                            canNavigate = screenState.canNavigate,
                            onNavigationModeChange = {
                                mapViewModel.setNavigationMode(it)
                                onNavigationModeChange(it)
                            },
                            onNavigateClick = {
                                val currentLocation = myLocation()
                                val item = if (currentLocation != null) {
                                    screenState.navigationItem.updateIfUsesCurrentLocation(currentLocation)
                                } else {
                                    screenState.navigationItem
                                }
                                if (screenState.canNavigate) {
                                    onNavigateClick(item)
                                }
                            },
                            modifier = modifier,
                        )
                    }
                }

                RoutePlanningBottomSheetType.NavigationPointSelection -> {
                    NavigationPointSelectionBottomSheet(
                        isIntermediatePoint = screenState.navigationItem.isIntermediatePointActive,
                        onSearchSelect = {
                            mapViewModel.activateNavigationItemPoints { item ->
                                onNavigationPointSelect(NavigationPointSelectionType.SEARCH, item)
                            }
                        },
                        onSavedLocationsSelect = {
                            mapViewModel.activateNavigationItemPoints { item ->
                                onNavigationPointSelect(
                                    NavigationPointSelectionType.SAVED_PLACES,
                                    item
                                )
                            }
                        },
                        onSelectOnMapSelect = {
                            mapViewModel.selectNavigationPoint(screenState.navigationItem)
                            mapViewModel.closeNavigationPointSelection()
                        },
                        onCancelClick = {
                            mapViewModel.activateNavigationItemPoints()
                            mapViewModel.closeNavigationPointSelection()
                        },
                        modifier = modifier,
                    )
                }

                RoutePlanningBottomSheetType.GPSError -> {
                    GPSErrorBottomSheet(
                        onCloseClick = { mapViewModel.activateNavigationItemPoints { } },
                        modifier = modifier
                    )
                }

                RoutePlanningBottomSheetType.LocationSharing -> {
                    LocationSharingDialog(
                        onOpenSettingsClick = onOpenLocationSettingsClick,
                        onBrowseMapsClick = onCloseLocationSharingDialog,
                        modifier = modifier,
                    )
                }
            }
        }

        is ScreenState.NavigationInProgress -> {
            RouteDetailsBottomSheet(
                isSoundEnabled = uiState.isSoundEnabled,
                isCommandsDisplayMode = uiState.navigationDisplayMode is NavigationDisplayMode.Commands,
                estimatedRouteDistance = routeState.estimatedRouteDistance.orEmpty(),
                estimatedRouteTime = routeState.estimatedRouteTime,
                onSoundClick = {
                    onSoundClick(uiState.isSoundEnabled.not())
                    mapViewModel.onSoundClicked()
                },
                onNavigationDisplayModeChange = {
                    mapViewModel.onNavigationDisplayModeChanged()
                },
                modifier = modifier
                    .conditional(uiState.navigationDisplayMode is NavigationDisplayMode.Map) {
                        background(colorWhite)
                            .padding(top = dividerThicknessMedium)
                    },
            )
        }

        is ScreenState.StopPoint -> {
            if (uiState.navigationDisplayMode is NavigationDisplayMode.Map) {
                val continueNavigation = { mapViewModel.onContinueRouteClick() }

                PointReachedBottomSheet(
                    text = stringResource(R.string.common_button_cta_continue),
                    onClick = continueNavigation,
                    modifier = modifier,
                )

                BackHandler(onBack = continueNavigation)
            } else {
                Box(modifier) // Empty `Box` ensures the `modifier` is notified about the bottom sheet's zero height.
            }
        }

        is ScreenState.DestinationReached -> {
            if (uiState.navigationDisplayMode is NavigationDisplayMode.Map) {
                PointReachedBottomSheet(
                    text = stringResource(R.string.common_label_done),
                    onClick = {
                        mapViewModel.resetState()
                        onCloseClick()
                    },
                    modifier = modifier,
                )
            } else {
                Box(modifier) // Empty `Box` ensures the `modifier` is notified about the bottom sheet's zero height.
            }
        }

        is ScreenState.MissingRegionOverlay -> {
            val missingRegion = (uiState.screenState as ScreenState.MissingRegionOverlay).missingRegion
            MissingRegionOverlayScreen(
                missingRegion = missingRegion,
                onDismissRequest = { mapViewModel.dismissMissingRegion() },
                modifier = modifier,
            )
        }

        else -> Box(modifier) // Empty `Box` ensures the `modifier` is notified about the bottom sheet's zero height.
    }
}