package com.mudita.map.common.download

import kotlinx.coroutines.flow.StateFlow

interface GetDownloadingStateUseCase {
    operator fun invoke(): StateFlow<Map<String, DownloadingState>>
}
