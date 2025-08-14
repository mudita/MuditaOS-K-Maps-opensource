package com.mudita.map.ui.routeCalculation

import net.osmand.router.errors.RouteCalculationError

sealed interface RouteCalculationState {
    object NotStarted : RouteCalculationState
    data class InProgress(val stage: Stage = Stage.Started) : RouteCalculationState {
        enum class Stage {
            Started, Alert, Continued
        }
    }
    data class Error(val error: RouteCalculationError) : RouteCalculationState
}