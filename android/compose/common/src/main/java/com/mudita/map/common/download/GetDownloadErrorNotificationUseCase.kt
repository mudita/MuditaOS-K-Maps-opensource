package com.mudita.map.common.download

import kotlinx.coroutines.flow.Flow

interface GetDownloadErrorNotificationUseCase {
    operator fun invoke(): Flow<Pair<String, DownloadingState.ErrorState?>>
}
