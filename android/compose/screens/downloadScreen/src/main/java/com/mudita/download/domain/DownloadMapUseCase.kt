package com.mudita.download.domain

import com.mudita.download.repository.models.Downloadable
import com.mudita.maps.data.api.Resource
import kotlinx.coroutines.flow.Flow

interface DownloadMapUseCase {
    operator fun invoke(map: Downloadable): Flow<Resource<Unit>>
}