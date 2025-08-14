package com.mudita.map.missingregionoverlay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.ui.Action
import com.mudita.download.ui.DownloadItemAction
import com.mudita.download.ui.ErrorType
import com.mudita.download.usecase.CheckDownloadRestrictionsUseCase
import com.mudita.download.usecase.GetDownloadItemUseCase
import com.mudita.download.usecase.SetDownloadPausedUseCase
import com.mudita.map.common.di.DispatcherQualifier
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.download.GetDownloadErrorNotificationUseCase
import com.mudita.map.common.download.GetDownloadProgressUseCase
import com.mudita.map.common.download.GetDownloadingStateUseCase
import com.mudita.maps.data.api.modelOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MissingMapOverlayViewModel @Inject constructor(
    private val getDownloadItemUseCase: GetDownloadItemUseCase,
    getDownloadingStateUseCase: GetDownloadingStateUseCase,
    getDownloadProgressUseCase: GetDownloadProgressUseCase,
    private val getDownloadErrorNotificationUseCase: GetDownloadErrorNotificationUseCase,
    private val checkDownloadRestrictionsUseCase: CheckDownloadRestrictionsUseCase,
    private val setDownloadPausedUseCase: SetDownloadPausedUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    @DispatcherQualifier.IO private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val missingRegion: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _action = MutableSharedFlow<Action?>()

    private val downloadItemAction = MutableStateFlow<DownloadItemAction?>(null)

    private val errorType = MutableStateFlow<ErrorType?>(null)

    val action: SharedFlow<Action?> = _action

    @OptIn(ExperimentalCoroutinesApi::class)
    private val downloadItem = missingRegion
        .flatMapLatest { missingRegion ->
            if (missingRegion != null) {
                getDownloadItemUseCase(missingRegion).map { it.modelOrNull }
            } else {
                flowOf(null)
            }
        }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val missingMapInfoState: StateFlow<MissingMapInfoState> =
        combine(
            downloadItem,
            getDownloadingStateUseCase(),
            getDownloadProgressUseCase(),
            downloadItemAction,
            errorType,
        ) { downloadItem, downloadingState, downloadProgress, downloadItemAction,
            errorType ->
            MissingMapInfoState(
                downloadItem = downloadItem,
                downloadingState = downloadItem?.id?.let(downloadingState::get)
                    ?: DownloadingState.Default,
                downloadProgress = downloadItem?.id?.let(downloadProgress::get)
                    ?: DownloadProgress.Empty,
                downloadItemAction = downloadItemAction,
                errorType = errorType,
                mapDownloaded = downloadItem?.downloaded == true
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, MissingMapInfoState())

    init {
        handleDownloadErrors()
    }

    fun setMissingRegion(missingRegion: String) {
        this.missingRegion.value = missingRegion
    }

    fun clearMissingMapInfo() {
        missingRegion.value = null
    }

    private fun download(downloadItem: DownloadItem) {
        viewModelScope.launch(dispatcher) {
            if (canStartDownload(downloadItem)) {
                _action.emit(Action.Download(downloadItem))
            }
        }
    }

    private fun handleDownloadErrors() {
        viewModelScope.launch {
            downloadItem.filterNotNull().collectLatest { downloadItem ->
                getDownloadErrorNotificationUseCase()
                    .collect { (id, errorState) ->
                        if (downloadItem.id == id) {
                            handleDownloadError(downloadItem, errorState)
                        }
                    }
            }
        }
    }

    private fun canStartDownload(downloadItem: DownloadItem): Boolean {
        val internetConnectionError = checkDownloadRestrictionsUseCase(downloadItem)
        errorType.value = internetConnectionError
        return internetConnectionError == null
    }

    fun onDownloadItemClick(downloadItem: DownloadItem, downloadingState: DownloadingState) {
        when {
            downloadingState == DownloadingState.Default ->
                download(downloadItem)

            downloadingState is DownloadingState.ErrorState ->
                handleDownloadError(downloadItem, downloadingState)

            downloadingState.isCancelable ->
                handleCancelDownload(downloadItem)
        }
    }

    private fun handleCancelDownload(downloadItem: DownloadItem) {
        downloadItemAction.value = DownloadItemAction.Cancel(downloadItem)
        setDownloadPausedUseCase(true)
    }

    private fun handleDownloadError(
        downloadItem: DownloadItem,
        downloadingState: DownloadingState.ErrorState?,
    ) {
        downloadItemAction.value = DownloadItemAction.get(downloadItem, downloadingState)
    }

    fun dismissDownloadAction() {
        downloadItemAction.value = null
        setDownloadPausedUseCase(false)
    }

    fun cancelDownload() {
        downloadItemAction.value = null
        cancelDownloadUseCase()
        setDownloadPausedUseCase(false)
    }

    fun clearDownloadItemAction() {
        downloadItemAction.value = null
    }

    fun dismissError() {
        errorType.value = null
    }
}
