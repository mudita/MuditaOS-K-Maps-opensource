package com.mudita.download.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.Downloadable
import com.mudita.download.repository.models.Resource
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.download.repository.utils.DownloadManager
import com.mudita.download.ui.DownloadViewModel
import com.mudita.download.ui.components.AllMapItemView
import com.mudita.download.ui.components.NetworkNotAvailable
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorGray_11
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.kompakt.commonUi.components.fakeScroll.KompaktLazyFakeScroll
import com.mudita.kompakt.commonUi.components.fakeScroll.canScroll
import com.mudita.map.common.download.DownloadingState
import com.mudita.maps.frontitude.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AllMapsTab(
    downloadState: DownloadViewModel.DownloadState,
    isSearchMode: Boolean,
    listState: LazyListState,
    onErrorClicked: (Downloadable, DownloadingState.ErrorState) -> Unit,
    cancelDownload: (Downloadable) -> Unit,
    onItemClicked: (Resource) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val downloadIndexes = downloadState.downloadIndexes

    when {
        !downloadState.isNetworkAvailable && !downloadState.indexesFetched -> {
            NetworkNotAvailable()
        }

        downloadState.isSearchResultEmpty && isSearchMode -> {
            EmptySearch()
        }

        else -> {
            LaunchedEffect(downloadState.downloadIndexesHash) {
                listState.scrollToItem(downloadState.firstVisibleItemIndex)
            }

            Column {
                downloadState.downloadRegions?.also {
                    AllMapItemView(
                        item = it,
                        downloadItemInfo = downloadState,
                        canScroll = false,
                        isSearchMode = isSearchMode,
                        enabled = true,
                        onErrorClicked = onErrorClicked,
                        cancelDownload = cancelDownload,
                        onItemClicked = { resource ->
                            if (isSearchMode) keyboardController?.hide()
                            onItemClicked(resource)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorBlack)
                }
                KompaktLazyFakeScroll(listState = listState, content = {
                    itemsIndexed(
                        items = downloadIndexes,
                        key = { _, resource -> resource.id },
                        contentType = { _, resource -> resource::class }
                    ) { index, item ->
                        val enabled = item.isEnabled(isSearchMode, downloadState)
                        AllMapItemView(
                            item = item,
                            downloadItemInfo = downloadState,
                            canScroll = listState.canScroll,
                            isSearchMode = isSearchMode,
                            enabled = enabled,
                            onErrorClicked = onErrorClicked,
                            cancelDownload = cancelDownload,
                            onItemClicked = { resource ->
                                if (isSearchMode) keyboardController?.hide()
                                onItemClicked(resource)
                                if (resource is ResourceGroup) {
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
                                }
                            },
                            downloadProgressEnabled = (item as? DownloadItem)?.let {
                                (!DownloadManager.isMapRegionDownloading(it) || it.downloaded)
                                        && !DownloadManager.isMapRegionQueued(it)
                            } ?: true
                        )
                        if (index != downloadIndexes.lastIndex) {
                            DashedHorizontalDivider(
                                color = if (enabled) colorBlack else colorGray_11,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                })
            }
        }
    }
}

@Composable
@Preview(backgroundColor = 0xffffffff, showBackground = true)
private fun EmptySearch() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.maps_managingmaps_search_error_h1_nomapswiththatname),
            style = KompaktTypography900.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(id = R.string.maps_managingmaps_search_error_body_usedifferentkeywords),
            style = KompaktTypography500.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

private fun Resource.isEnabled(
    isSearchMode: Boolean,
    downloadState: DownloadViewModel.DownloadState,
): Boolean =
    isSearchMode || when (this) {
        is DownloadItem -> (!DownloadManager.isMapRegionDownloading(this)
                && !DownloadManager.isMapRegionQueued(this)) || this.downloaded
                || DownloadManager.isProvinceSkipped(this)

        is ResourceGroup -> parents.none { downloadState.getDownloadingState(it).isCancelable }
        else -> true
    }