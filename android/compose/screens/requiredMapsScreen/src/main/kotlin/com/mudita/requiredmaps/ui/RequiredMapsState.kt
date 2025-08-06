package com.mudita.requiredmaps.ui

import androidx.compose.runtime.Stable
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.MapFile
import com.mudita.download.ui.DownloadItemAction
import com.mudita.download.ui.DownloadItemInfo
import com.mudita.download.ui.ErrorType
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState

@Stable
data class RequiredMapsState(
    val downloadItems: List<DownloadItem> = emptyList(),
    val showNetworkError: Boolean = false,
    override val downloadProgressMap: Map<String, DownloadProgress> = emptyMap(),
    override val downloadingStateMap: Map<String, DownloadingState> = emptyMap(),
    override val downloadedItems: Set<MapFile> = emptySet(),
    val errorType: ErrorType? = null,
    val downloadItemAction: DownloadItemAction? = null
) : DownloadItemInfo {

    val showDownloadAllButton: Boolean =
        downloadItems.isNotEmpty() &&
                downloadItems.all { getDownloadingState(it) == DownloadingState.Default }

    val showDoneButton: Boolean = downloadItems.any { it.downloaded }

}
