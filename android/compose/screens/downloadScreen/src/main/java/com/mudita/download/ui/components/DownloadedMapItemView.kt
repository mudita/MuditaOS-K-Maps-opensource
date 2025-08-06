package com.mudita.download.ui.components

import com.mudita.download.R as downloadR
import com.mudita.map.common.R as commonR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.LocalIndex
import com.mudita.download.repository.models.MapFile
import com.mudita.download.repository.models.Resource
import com.mudita.download.ui.DownloadViewModel
import com.mudita.download.ui.progress.ProgressIndicator
import com.mudita.download.utils.isItemDownloaded
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.download.DownloadingState.ErrorState
import com.mudita.map.common.download.formattedRemainingTime
import com.mudita.map.common.ui.listItemHeight
import com.mudita.map.common.utils.FileSize
import com.mudita.maps.frontitude.R.string

@Composable
fun DownloadedMapItemView(
    item: MapFile,
    downloadViewModel: DownloadViewModel,
    downloadState: DownloadViewModel.DownloadState,
    isEditMode: Boolean,
    canScroll: Boolean,
    getNameToDisplay: (fileName: String) -> String,
    mapToDelete: (MapFile?) -> Unit,
    updateMap: (localMap: LocalIndex) -> Unit,
    onItemClicked: (Resource) -> Unit,
    onErrorClicked: (MapFile, ErrorState) -> Unit,
) {
    when (item) {
        is DownloadItem -> {
            val isDownloading = downloadState.isDownloading(item)
            val isQueued = downloadState.isQueued(item)
            val downloadProgress = downloadState.getDownloadProgress(item)
            val downloadingState = downloadState.getDownloadingState(item)
            val showProgress = isDownloading || isQueued || downloadingState.isError
            val isDownloaded = downloadState.downloadedItems.isItemDownloaded(item) ||
                    item.downloaded
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(listItemHeight)
                    .padding(start = 16.dp)
                    .clickable {
                        if (downloadingState is ErrorState) onErrorClicked(item, downloadingState)
                    }
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        style = KompaktTypography900.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (showProgress) {
                        ProgressIndicator(
                            progress = downloadProgress.fraction.toFloat(),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }

                    val downloadStatus = when {
                        downloadingState == ErrorState.InternetConnectionError ->
                            stringResource(id = string.common_status_tryingtoreconnect)

                        downloadingState == ErrorState.InternetConnectionRetryFailed ->
                            stringResource(id = string.common_label_downloadinterrupted)

                        downloadingState == DownloadingState.PreparingMap ->
                            stringResource(id = string.maps_managingmaps_status_preparingmap)

                        downloadState.isDownloading(item) ->
                            downloadProgress.formattedRemainingTime()

                        downloadingState.isMemoryOrIoError ->
                            stringResource(id = string.common_label_downloadfailed)

                        isQueued ->
                            stringResource(id = string.common_status_downloadqueued)

                        else ->
                            item.description
                    }

                    MapDescription(
                        parentName = item.parentName,
                        mapSize = item.size,
                        isDownloading = true,
                        downloadStatus = downloadStatus,
                    )
                }
                Icon(
                    painter = painterResource(
                        when {
                            downloadingState.isError -> commonR.drawable.ic_alert
                            downloadingState.isCancelable -> commonR.drawable.ic_cancel
                            isEditMode -> downloadR.drawable.ic_remove
                            isDownloaded -> commonR.drawable.ic_success
                            else -> commonR.drawable.ic_download
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = if (canScroll) 0.dp else 16.dp, start = 12.dp)
                        .clickable {
                            when {
                                downloadingState.isCancelable ->
                                    downloadViewModel.cancelDownload(item)

                                downloadingState is ErrorState ->
                                    onErrorClicked(item, downloadingState)

                                isEditMode -> {
                                    mapToDelete(item)
                                    downloadViewModel.onActionCanceled()
                                }

                                !isDownloaded ->
                                    onItemClicked(item)
                            }
                        }
                )
            }
        }

        is LocalIndex -> {
            val isDownloading = downloadState.isDownloading(item)
            val isQueued = downloadState.isQueued(item)
            val downloadProgress = downloadState.getDownloadProgress(item)
            val downloadingState = downloadState.getDownloadingState(item)
            val showProgress = isDownloading || isQueued || downloadingState.isError

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(listItemHeight)
                    .padding(start = 16.dp)
                    .clickable {
                        if (downloadingState is ErrorState) onErrorClicked(item, downloadingState)
                    }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getNameToDisplay(item.fileName),
                        style = KompaktTypography900.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showProgress) {
                        ProgressIndicator(
                            progress = downloadProgress.fraction.toFloat(),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    val downloadStatus = when (downloadingState) {
                        DownloadingState.Queued ->
                            stringResource(string.common_status_downloadqueued)

                        DownloadingState.PreparingMap ->
                            stringResource(string.maps_managingmaps_status_preparingmap)

                        ErrorState.InternetConnectionError ->
                            stringResource(string.common_status_tryingtoreconnect)

                        ErrorState.InternetConnectionRetryFailed ->
                            stringResource(string.common_label_downloadinterrupted)

                        ErrorState.IoError,
                        ErrorState.MemoryNotEnough ->
                            stringResource(id = string.common_label_downloadfailed)

                        DownloadingState.Downloading ->
                            downloadProgress.formattedRemainingTime()

                        DownloadingState.Default -> null
                    }

                    MapDescription(
                        parentName = item.parentName,
                        mapSize = FileSize.formatKilobytes(item.size.toDouble()),
                        updateAvailable = item.updateAvailable,
                        isDownloading = isDownloading,
                        downloadStatus = downloadStatus,
                    )
                }
                Icon(
                    painter = painterResource(
                        id = when {
                            isEditMode -> downloadR.drawable.ic_remove
                            downloadingState.isError -> commonR.drawable.ic_alert
                            downloadingState.isCancelable -> commonR.drawable.ic_cancel
                            !item.updateAvailable -> commonR.drawable.ic_success
                            else -> commonR.drawable.ic_download
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = if (canScroll) 0.dp else 16.dp, start = 12.dp)
                        .clickable {
                            when {
                                isEditMode -> {
                                    mapToDelete(item)
                                    downloadViewModel.onActionCanceled()
                                }

                                downloadingState.isCancelable ->
                                    downloadViewModel.cancelUpdate(item)

                                downloadingState is ErrorState ->
                                    onErrorClicked(item, downloadingState)

                                item.updateAvailable ->
                                    updateMap(item)
                            }
                        }
                )
            }
        }
    }
}
