package com.mudita.map.searchresult.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalContext
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.ui.DownloadItemAction
import com.mudita.download.ui.ErrorType
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.download.getDownloadItemStateDescription

@Immutable
data class SearchResultState(
    val formattedDescription: String = "",
    val downloadItem: DownloadItem? = null,
    val downloadingState: DownloadingState = DownloadingState.Default,
    val downloadProgress: DownloadProgress = DownloadProgress.Empty,
    val downloadItemAction: DownloadItemAction? = null,
    val errorType: ErrorType? = null,
)

@Composable
internal fun SearchResultState.getDownloadItemStateDescription(): String =
    downloadingState.getDownloadItemStateDescription(LocalContext.current, downloadProgress)
