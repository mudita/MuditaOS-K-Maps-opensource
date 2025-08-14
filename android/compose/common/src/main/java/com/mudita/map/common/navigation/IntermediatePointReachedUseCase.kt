package com.mudita.map.common.navigation

import kotlinx.coroutines.flow.Flow

interface IntermediatePointReachedUseCase {
    operator fun invoke(): Flow<Int>
}
