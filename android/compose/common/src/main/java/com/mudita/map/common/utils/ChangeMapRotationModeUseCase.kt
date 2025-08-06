package com.mudita.map.common.utils

interface ChangeMapRotationModeUseCase {
    suspend operator fun invoke(mapRotationEnabled: Boolean, isWalkingNavigation: Boolean)
}
