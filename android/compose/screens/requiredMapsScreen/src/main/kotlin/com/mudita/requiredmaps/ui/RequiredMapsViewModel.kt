package com.mudita.requiredmaps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.ui.Action
import com.mudita.download.ui.DownloadItemAction
import com.mudita.download.ui.ErrorType
import com.mudita.download.usecase.SetDownloadPausedUseCase
import com.mudita.map.common.di.DispatcherQualifier
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.download.GetDownloadErrorNotificationUseCase
import com.mudita.map.common.download.GetDownloadProgressUseCase
import com.mudita.map.common.download.GetDownloadingStateUseCase
import com.mudita.map.common.utils.combine
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkStatus
import com.mudita.map.common.utils.network.NetworkType
import com.mudita.maps.data.api.Resource
import com.mudita.maps.data.api.modelOrNull
import com.mudita.requiredmaps.usecase.GetRequiredMapsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RequiredMapsViewModel @Inject constructor(
    getRequiredMapsUseCase: GetRequiredMapsUseCase,
    getDownloadingStateUseCase: GetDownloadingStateUseCase,
    getDownloadProgressUseCase: GetDownloadProgressUseCase,
    private val networkManager: NetworkManager,
    private val setDownloadPausedUseCase: SetDownloadPausedUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val memoryManager: MemoryManager,
    private val getDownloadErrorNotificationUseCase: GetDownloadErrorNotificationUseCase,
    @DispatcherQualifier.IO private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val downloadItemAction = MutableStateFlow<DownloadItemAction?>(null)

    private val errorType = MutableStateFlow<ErrorType?>(null)

    private val _action = MutableSharedFlow<Action?>()

    val action: SharedFlow<Action?> = _action

    val state: StateFlow<RequiredMapsState> =
        combine(
            getRequiredMapsUseCase(),
            networkManager.networkStatus,
            getDownloadingStateUseCase(),
            getDownloadProgressUseCase(),
            downloadItemAction,
            errorType,
        ) { downloadItemsResource, networkStatus, downloadingState, downloadProgress,
            downloadItemAction, errorType ->
            RequiredMapsState(
                downloadItems = downloadItemsResource.modelOrNull ?: emptyList(),
                showNetworkError = networkStatus == NetworkStatus.Unavailable &&
                        downloadItemsResource !is Resource.Success,
                downloadingStateMap = downloadingState,
                downloadItemAction = downloadItemAction,
                downloadProgressMap = downloadProgress,
                errorType = errorType,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RequiredMapsState())

    init {
        handleDownloadErrors()
    }

    fun download(downloadItem: DownloadItem) {
        download(listOf(downloadItem))
    }

    fun downloadAll() {
        val itemsToDownload = state.value.downloadItems.filter { !it.downloaded }
        download(itemsToDownload)
    }

    private fun download(downloadItems: List<DownloadItem>) {
        viewModelScope.launch(dispatcher) {
            val fullSize = downloadItems.sumOf { it.getFullSize() }
            if (hasInternetConnection() && hasMemorySpace(fullSize)) {
                _action.emit(Action.Download(downloadItems))
            }
        }
    }

    private fun handleDownloadErrors() {
        viewModelScope.launch {
            val downloadItems = state.first { it.downloadItems.isNotEmpty() }.downloadItems
            getDownloadErrorNotificationUseCase()
                .collect { (id, errorState) ->
                    val downloadItem = downloadItems.find { it.id == id } ?: return@collect
                    handleDownloadError(downloadItem, errorState)
                }
        }
    }

    private fun hasInternetConnection(): Boolean = when {
        !networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) -> {
            errorType.value = ErrorType.WifiNetwork
            false
        }

        !networkManager.isNetworkReachable(NetworkType.ALL) -> {
            errorType.value = ErrorType.Network
            false
        }

        else -> true
    }

    private fun hasMemorySpace(downloadingMB: Double): Boolean = when {
        !memoryManager.hasEnoughSpace(downloadingMB) -> {
            errorType.value = ErrorType.Memory
            false
        }

        else -> true
    }

    fun handleCancelDownload(downloadItem: DownloadItem) {
        downloadItemAction.value = DownloadItemAction.Cancel(downloadItem)
        setDownloadPausedUseCase(true)
    }

    fun handleDownloadError(
        downloadItem: DownloadItem,
        downloadingState: DownloadingState.ErrorState?,
    ) {
        downloadItemAction.value = DownloadItemAction.get(downloadItem, downloadingState)
    }

    fun dismissDownloadAction() {
        downloadItemAction.value = null
        setDownloadPausedUseCase(false)
    }

    fun cancelDownload(downloadItem: DownloadItem) {
        downloadItemAction.value = null
        if (state.value.isDownloading(downloadItem)) cancelDownloadUseCase()
        setDownloadPausedUseCase(false)
    }

    fun clearDownloadItemAction() {
        downloadItemAction.value = null
    }

    fun dismissError() {
        errorType.value = null
    }
}
