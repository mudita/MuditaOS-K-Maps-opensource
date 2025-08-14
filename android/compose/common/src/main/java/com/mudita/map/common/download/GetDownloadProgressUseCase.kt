package com.mudita.map.common.download

import kotlinx.coroutines.flow.Flow

interface GetDownloadProgressUseCase {
    operator fun invoke(): Flow<Map<String, DownloadProgress>>
}
