package com.mudita.download.usecase

import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import com.mudita.map.common.download.OnDownloadFinishUseCase
import com.mudita.map.common.maps.GetMissingMapsUseCase
import com.mudita.map.common.region.GetRegionsIndexedEvents
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.maps.data.api.Resource
import com.mudita.maps.data.api.mapCatching
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import net.osmand.data.LatLon

class GetDownloadItemUseCase @Inject constructor(
    private val getMissingMapsUseCase: GetMissingMapsUseCase,
    private val downloadRepository: DownloadRepository,
    private val networkManager: NetworkManager,
    private val onDownloadFinishUseCase: OnDownloadFinishUseCase,
    private val getRegionsIndexedEvents: GetRegionsIndexedEvents,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(latLon: LatLon): Flow<Resource<DownloadItem?>> =
        flow { emit(getMissingMapsUseCase(latLon)) }
            .flatMapLatest { missingMapsResult ->
                missingMapsResult.fold(
                    onSuccess = { missingMaps ->
                        if (missingMaps.isNotEmpty()) {
                            invoke(missingMaps.first())
                        } else {
                            flowOf(Resource.Success(null))
                        }
                    },
                    onFailure = { flowOf(Resource.Error(it)) }
                )
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(missingMap: String): Flow<Resource<DownloadItem?>> =
        networkManager
            .networkStatus
            .flatMapLatest { getRegionsIndexedEvents() }
            .flatMapLatest { downloadRepository.getIndexesList() }
            .combine(
                flow {
                    emit(null)
                    emitAll(onDownloadFinishUseCase())
                }
            ) { resource, downloadedItem ->
                resource.mapCatching { resourceGroup ->
                    val downloadItem = resourceGroup
                        .allDownloadItems
                        .first { downloadItem -> missingMap == downloadItem.getBasename() }
                    if (downloadedItem == downloadItem.getBasename()) {
                        downloadItem.copy(downloaded = true)
                    } else {
                        downloadItem
                    }
                }
            }
            .onStart { emit(Resource.Loading()) }
            .flowOn(Dispatchers.Default)
}
