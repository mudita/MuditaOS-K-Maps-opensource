package com.mudita.download.domain

import com.mudita.download.repository.models.ResourceGroup
import com.mudita.maps.data.api.Resource
import kotlinx.coroutines.flow.Flow

interface GetIndexesUseCase {
    operator fun invoke(): Flow<Resource<ResourceGroup>>
}