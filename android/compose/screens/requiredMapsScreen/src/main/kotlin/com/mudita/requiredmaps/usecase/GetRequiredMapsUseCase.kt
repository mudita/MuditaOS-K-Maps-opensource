package com.mudita.requiredmaps.usecase

import androidx.lifecycle.SavedStateHandle
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import com.mudita.map.common.download.OnDownloadFinishUseCase
import com.mudita.map.common.region.GetRegionsIndexedEvents
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.maps.data.api.Resource
import com.mudita.maps.data.api.map
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart

class GetRequiredMapsUseCase @Inject constructor(
    private val missingMaps: List<String>,
    private val downloadRepository: DownloadRepository,
    private val networkManager: NetworkManager,
    private val onDownloadFinishUseCase: OnDownloadFinishUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getRegionsIndexedEvents: GetRegionsIndexedEvents,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Resource<List<DownloadItem>>> =
        networkManager
            .networkStatus
            .distinctUntilChanged()
            .flatMapLatest { getRegionsIndexedEvents() }
            .flatMapLatest { downloadRepository.getIndexesList() }
            .combine(
                flow {
                    emit(null)
                    emitAll(onDownloadFinishUseCase())
                }
            ) { resource, downloadedItem ->
                downloadedItem?.also { savedStateHandle.addDownloadedMap(it) }
                resource.map { resourceGroup ->
                    val missingMaps =
                        resourceGroup.allDownloadItems.filter { downloadItem ->
                            missingMaps.contains(downloadItem.getBasename())
                        }
                    val downloadedMaps = savedStateHandle.downloadedMaps
                    if (downloadedMaps.isNotEmpty()) {
                        missingMaps.map { item ->
                            item.copy(downloaded = downloadedMaps.contains(item.getBasename()))
                        }
                    } else {
                        missingMaps
                    }
                }
            }
            .onStart { emit(Resource.Loading()) }
            .flowOn(Dispatchers.Default)

    private val SavedStateHandle.downloadedMaps: List<String>
        get() = get(DOWNLOADED_MAPS_KEY) ?: emptyList()

    private fun SavedStateHandle.addDownloadedMap(mapName: String) {
        set(DOWNLOADED_MAPS_KEY, downloadedMaps + mapName)
    }

    companion object {
        private const val DOWNLOADED_MAPS_KEY = "downloaded_maps"
    }
}
