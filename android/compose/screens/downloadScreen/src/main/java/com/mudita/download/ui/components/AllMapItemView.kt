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
import com.mudita.download.repository.models.DownloadRegions
import com.mudita.download.repository.models.Downloadable
import com.mudita.download.repository.models.Resource
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.download.ui.DownloadItemInfo
import com.mudita.download.ui.progress.ProgressIndicator
import com.mudita.download.utils.isItemDownloaded
import com.mudita.download.utils.isItemsDownloaded
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorGray_11
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.download.DownloadingState.ErrorState
import com.mudita.map.common.download.formattedRemainingTime
import com.mudita.map.common.ui.listItemHeight
import com.mudita.map.common.utils.FileSize
import com.mudita.maps.frontitude.R.string

@Composable
fun AllMapItemView(
    item: Resource,
    downloadItemInfo: DownloadItemInfo,
    canScroll: Boolean,
    isSearchMode: Boolean,
    enabled: Boolean,
    onErrorClicked: (Downloadable, ErrorState) -> Unit,
    cancelDownload: (item: Downloadable) -> Unit,
    onItemClicked: (item: Resource) -> Unit,
    downloadProgressEnabled: Boolean = true,
) {
    val contentColor = if (enabled) colorBlack else colorGray_11
    when (item) {
        is DownloadItem -> {
            val isDownloading = downloadItemInfo.isDownloading(item)
            val isQueued = downloadItemInfo.isQueued(item)
            val downloadProgress = downloadItemInfo.getDownloadProgress(item)
            val downloadingState = downloadItemInfo.getDownloadingState(item)
            val showProgress = (isSearchMode || downloadProgressEnabled) &&
                    (isDownloading || isQueued || downloadingState.isError)
            val isDownloaded = downloadItemInfo.downloadedItems.isItemDownloaded(item) ||
                    item.downloaded
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(listItemHeight)
                    .padding(start = 16.dp)
                    .clickable {
                        if (downloadingState is ErrorState) {
                            onErrorClicked(item, downloadingState)
                        }
                    }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = KompaktTypography900.labelMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (showProgress) {
                        ProgressIndicator(
                            progress = downloadProgress.fraction.toFloat(),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    val description = when {
                        !isSearchMode && !downloadProgressEnabled ->
                            null

                        downloadingState == ErrorState.InternetConnectionError ->
                            stringResource(id = string.common_status_tryingtoreconnect)

                        downloadingState == DownloadingState.PreparingMap ->
                            stringResource(id = string.maps_managingmaps_status_preparingmap)

                        downloadingState.isInternetError ->
                            stringResource(id = string.common_label_downloadinterrupted)

                        downloadingState.isMemoryOrIoError ->
                            stringResource(id = string.common_label_downloadfailed)

                        isDownloading ->
                            downloadProgress.formattedRemainingTime()

                        isQueued ->
                            stringResource(id = string.common_status_downloadqueued)

                        else ->
                            item.description
                    }

                    MapDescription(
                        parentName = if (isSearchMode) item.parentName else null,
                        mapSize = item.size,
                        contentColor = contentColor,
                        isDownloading = downloadingState != DownloadingState.Default,
                        downloadStatus = description,
                    )
                }
                if (isSearchMode || downloadProgressEnabled)
                    Icon(
                        painter = when {
                            downloadingState.isError -> commonR.drawable.ic_alert
                            downloadingState.isCancelable -> commonR.drawable.ic_cancel
                            isDownloaded -> commonR.drawable.ic_success
                            else -> commonR.drawable.ic_download
                        }.let { painterResource(it) },
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .padding(end = if (canScroll) 0.dp else 16.dp, start = 16.dp)
                            .clickable {
                                when {
                                    downloadingState is ErrorState ->
                                        onErrorClicked(item, downloadingState)

                                    downloadingState.isCancelable ->
                                        cancelDownload(item)

                                    !isDownloaded ->
                                        onItemClicked(item)
                                }
                            }
                    )
            }
        }

        is DownloadRegions -> {
            val isDownloadInProgress = downloadItemInfo.isDownloading(item)
            val downloadProgress = downloadItemInfo.getDownloadProgress(item)
            val downloadingState = downloadItemInfo.getDownloadingState(item)
            val isDownloaded = downloadItemInfo.downloadedItems.isItemsDownloaded(item) ||
                    item.regions.all { it.downloaded }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(listItemHeight)
                    .padding(start = 16.dp)
                    .clickable {
                        if (downloadingState is ErrorState) {
                            onErrorClicked(item, downloadingState)
                        }
                    }
            ) {
                Icon(
                    painter = painterResource(id = downloadR.drawable.ic_all_regions),
                    contentDescription = null
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 24.dp)
                ) {
                    Text(
                        text = stringResource(id = string.maps_downloaded_label_placeholder_allregions),
                        style = KompaktTypography900.labelMedium,
                    )
                    if (isDownloadInProgress || downloadingState.isError) {
                        ProgressIndicator(
                            progress = downloadProgress.fraction.toFloat(),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    Text(
                        text = when {
                            downloadingState == ErrorState.InternetConnectionError ->
                                stringResource(string.common_status_tryingtoreconnect)

                            downloadingState == DownloadingState.PreparingMap ->
                                stringResource(string.maps_managingmaps_status_preparingmap)

                            downloadingState.isInternetError ->
                                stringResource(string.common_label_downloadinterrupted)

                            downloadingState.isMemoryOrIoError ->
                                stringResource(string.common_label_downloadfailed)

                            downloadingState == DownloadingState.Queued ->
                                stringResource(string.common_status_downloadqueued)

                            isDownloadInProgress ->
                                downloadProgress.formattedRemainingTime()

                            else ->
                                FileSize.formatMegabytes(item.totalSize)
                        },
                        style = KompaktTypography500.labelSmall,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    painter = when {
                        downloadingState.isError -> commonR.drawable.ic_alert
                        downloadingState.isCancelable -> commonR.drawable.ic_cancel
                        isDownloaded -> commonR.drawable.ic_success
                        else -> commonR.drawable.ic_download
                    }.let { painterResource(it) },
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable {
                            when {
                                downloadingState is ErrorState ->
                                    onErrorClicked(item, downloadingState)

                                downloadingState.isCancelable ->
                                    cancelDownload(item)

                                !isDownloaded ->
                                    onItemClicked(item)
                            }
                        }
                )
            }
        }

        is ResourceGroup -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(listItemHeight)
                    .padding(start = 16.dp)
                    .clickable(enabled = enabled) { onItemClicked(item) }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val description: String? = item.parents.lastOrNull()?.takeIf { it.isNotBlank() && isSearchMode }

                    Text(
                        text = item.name,
                        style = if (description != null) KompaktTypography900.labelMedium else KompaktTypography500.labelMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = KompaktTypography500.labelSmall,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    painter = painterResource(id = commonR.drawable.ic_arrow_right_small),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(end = if (canScroll) 6.dp else 16.dp, start = 12.dp)
                )
            }
        }
    }
}
