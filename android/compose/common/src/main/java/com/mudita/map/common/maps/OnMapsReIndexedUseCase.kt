package com.mudita.map.common.maps

import kotlinx.coroutines.flow.Flow

interface OnMapsReIndexedUseCase {
    operator fun invoke(): Flow<Unit>
}
