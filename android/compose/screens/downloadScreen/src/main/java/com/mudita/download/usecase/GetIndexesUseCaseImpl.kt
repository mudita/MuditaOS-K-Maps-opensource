package com.mudita.download.usecase

import com.mudita.download.domain.GetIndexesUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.map.common.region.GetRegionsIndexedEvents
import com.mudita.maps.data.api.Resource
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class GetIndexesUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val getRegionsIndexedEvents: GetRegionsIndexedEvents,
) : GetIndexesUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<Resource<ResourceGroup>> =
        getRegionsIndexedEvents()
            .flatMapLatest { downloadRepository.getIndexesList() }
}