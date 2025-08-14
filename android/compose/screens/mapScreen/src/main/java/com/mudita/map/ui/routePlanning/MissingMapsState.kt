package com.mudita.map.ui.routePlanning

sealed class MissingMapsState {
    object Idle : MissingMapsState()
    object Checking : MissingMapsState()
    data class MissingMapsFound(val missingMaps: List<String>) : MissingMapsState()
}
