package com.mudita.map.ui

import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.model.routing.RouteDirectionInfo
import com.mudita.map.repository.NavigationDisplayMode
import com.mudita.map.repository.NavigationModeItem
import com.mudita.map.ui.routeCalculation.RouteCalculationState
import com.mudita.map.ui.routePlanning.MissingMapsState
import com.mudita.map.ui.routePlanning.RoutePlanningBottomSheetType
import net.osmand.data.LatLon

data class RouteState(
    val currentLocation: LatLon? = null,
    val finalLocation: LatLon? = null,
    val routeDirections: List<RouteDirectionInfo> = emptyList(),
    val navigationSteps: List<NavigationStep> = emptyList(),
    val estimatedRouteTime: NavigationTime? = null,
    val estimatedRouteDistance: String? = null,
    val missingMaps: List<String> = emptyList(),
    val myPlaceItem: MyPlaceItem? = null,
    val searchItem: SearchItem? = null,
    val navigationItem: NavigationItem? = null,
)

data class UiState(
    val screenState: ScreenState = ScreenState.Idle(),
    val navigationDisplayMode: NavigationDisplayMode = NavigationDisplayMode.Map,
    val isSoundEnabled: Boolean = false,
    val isNavigationBackHandlerEnabled: Boolean = false,
) {
    val isNavigating: Boolean = screenState is ScreenState.NavigationInProgress
            || screenState is ScreenState.StopPoint
            || screenState is ScreenState.DestinationReached

    val showMapButtons: Boolean = screenState is ScreenState.Idle ||
            screenState is ScreenState.SearchResult ||
            screenState is ScreenState.MissingRegionOverlay

    val showCenterNavigationButton: Boolean = (screenState as? ScreenStateWithCenterButton)?.isCenterButtonVisible ?: false
}

sealed class ScreenState(val amenitySearchEnabled: Boolean = false)  {
    data class Idle(val dialogType: DialogType = DialogType.None) : ScreenState(amenitySearchEnabled = true) {
        enum class DialogType {
            None, GPSError, LocationSharing
        }
    }

    object WelcomeToMaps : ScreenState()
    data class NavigationInProgress(override val isCenterButtonVisible: Boolean) : ScreenState(), ScreenStateWithCenterButton
    data class StopPoint(val address: String, override val isCenterButtonVisible: Boolean) : ScreenState(), ScreenStateWithCenterButton
    data class DestinationReached(val address: String, override val isCenterButtonVisible: Boolean) : ScreenState(), ScreenStateWithCenterButton
    data class PlanningRoute(
        val navigationItem: NavigationItem,
        val selectedNavigationMode: NavigationModeItem,
        val bottomSheetType: RoutePlanningBottomSheetType,
        val missingMapsState: MissingMapsState,
        val routeCalculationState: RouteCalculationState = RouteCalculationState.NotStarted,
    ) : ScreenState() {
        val canNavigate: Boolean = navigationItem.canNavigate() && missingMapsState is MissingMapsState.Idle
        val shouldShowNavigationModeSelection: Boolean = missingMapsState !is MissingMapsState.MissingMapsFound && routeCalculationState is RouteCalculationState.NotStarted
        val isCalculating: Boolean = routeCalculationState is RouteCalculationState.InProgress
    }

    data class SearchResult(val searchItem: SearchItem) : ScreenState()
    data class SelectLocation(val navigationItem: NavigationItem) : ScreenState(amenitySearchEnabled = true)
    data class MissingRegionOverlay(val missingRegion: String) : ScreenState()
}

data class NextSteps(
    val first: NavigationStep,
    val second: NavigationStep?,
)

fun ScreenState.requiresHiddenStatusBar(): Boolean =
    this is ScreenState.Idle || this is ScreenState.SearchResult ||
            this is ScreenState.SelectLocation || this is ScreenState.MissingRegionOverlay

interface ScreenStateWithCenterButton {
    val isCenterButtonVisible: Boolean
}
