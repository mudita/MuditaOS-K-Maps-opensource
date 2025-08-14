package com.mudita.map.common.download

import kotlinx.coroutines.flow.Flow

interface OnDownloadFinishUseCase {
    operator fun invoke(): Flow<String>
}
