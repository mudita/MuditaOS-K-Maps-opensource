package com.mudita.download.ui

import com.mudita.download.repository.models.DownloadItem
import com.mudita.map.common.download.DownloadingState

sealed class DownloadItemAction {
    abstract val item: DownloadItem

    data class Cancel(override val item: DownloadItem) : DownloadItemAction()

    data class AlertDownloadInterrupted(override val item: DownloadItem) : DownloadItemAction()

    data class AlertInsufficientMemory(override val item: DownloadItem) : DownloadItemAction()

    companion object {
        fun get(
            downloadItem: DownloadItem,
            downloadingState: DownloadingState.ErrorState?,
        ): DownloadItemAction? =
            when (downloadingState) {
                is DownloadingState.ErrorState.InternetConnectionError,
                is DownloadingState.ErrorState.InternetConnectionRetryFailed ->
                    AlertDownloadInterrupted(downloadItem)

                is DownloadingState.ErrorState.IoError,
                is DownloadingState.ErrorState.MemoryNotEnough ->
                    AlertInsufficientMemory(downloadItem)

                else -> null
            }
    }
}
