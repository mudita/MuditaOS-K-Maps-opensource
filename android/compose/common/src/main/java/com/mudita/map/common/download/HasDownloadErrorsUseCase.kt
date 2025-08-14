package com.mudita.map.common.download

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HasDownloadErrorsUseCase @Inject constructor(
    private val getDownloadingStateUseCase: GetDownloadingStateUseCase
) {
    operator fun invoke(): Flow<Boolean> = getDownloadingStateUseCase()
        .map { stateMap -> stateMap.values.any { it.isError } }
}
