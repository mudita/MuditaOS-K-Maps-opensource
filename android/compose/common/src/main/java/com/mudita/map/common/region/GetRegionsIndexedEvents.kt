package com.mudita.map.common.region

import kotlinx.coroutines.flow.Flow

interface GetRegionsIndexedEvents {
    operator fun invoke(): Flow<Unit>
}
