package com.mudita.map.searchresult.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.service.bindDownloadService
import com.mudita.download.ui.DownloadItemAction
import com.mudita.download.ui.components.DownloadItem
import com.mudita.download.ui.dialogs.CancelDownloadDialog
import com.mudita.download.ui.dialogs.ConnectionErrorDialog
import com.mudita.download.ui.dialogs.MemoryErrorDialog
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktButtonAttributes
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.map.common.BuildConfig
import com.mudita.map.common.R.drawable
import com.mudita.map.common.components.ErrorBottomSheet
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.map.common.utils.firstNotNull
import com.mudita.maps.frontitude.R.string
import com.mudita.myplaces.repository.mapper.toMyPlaceItem
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import net.osmand.data.LatLon

@Composable
fun SearchResultBottomSheet(
    item: SearchItem,
    myPlaceItem: MyPlaceItem?,
    modifier: Modifier = Modifier,
    onSaveMyPlace: (MyPlaceItem) -> Unit,
    onDeleteMyPlace: (MyPlaceItem) -> Unit,
    onNavigateClick: () -> Unit,
) {
    val viewModel: SearchResultViewModel = hiltViewModel()
    viewModel.loadSearchItem(item)

    val state: SearchResultState = viewModel.searchResultState.collectAsState().value
    val downloadItemAction = state.downloadItemAction
    val errorType = state.errorType
    val downloadService = bindDownloadService(viewModel.action.filterNotNull())
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        SearchResultBottomSheetContent(
            item = item,
            myPlaceItem = myPlaceItem,
            state = state,
            onDownloadItemClick = viewModel::onDownloadItemClick,
            onSaveMyPlace = onSaveMyPlace,
            onDeleteMyPlace = onDeleteMyPlace,
            onNavigateClick = onNavigateClick,
        )

        when {
            downloadItemAction is DownloadItemAction.Cancel ->
                CancelDownloadDialog(
                    mapName = downloadItemAction.item.name,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onKeepMap = { viewModel.dismissDownloadAction() },
                    onCancelDownload = {
                        coroutineScope.launch {
                            viewModel.cancelDownload()
                            downloadService
                                .firstNotNull()
                                .cancelDownload(downloadItemAction.item)
                        }
                    }
                )

            downloadItemAction is DownloadItemAction.AlertDownloadInterrupted ->
                ConnectionErrorDialog(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onResumeDownloadClick = {
                        viewModel.dismissDownloadAction()
                        coroutineScope.launch {
                            downloadService.firstNotNull()
                                .tryResumeDownload(downloadItemAction.item)
                        }
                    },
                    onCancelDownloadClick = {
                        viewModel.cancelDownload()
                        coroutineScope.launch {
                            downloadService.firstNotNull().cancelDownload(downloadItemAction.item)
                        }
                    },
                    onCloseClick = {
                        viewModel.clearDownloadItemAction()
                    },
                )

            downloadItemAction is DownloadItemAction.AlertInsufficientMemory ->
                MemoryErrorDialog(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onTryAgainClick = {
                        viewModel.dismissDownloadAction()
                        coroutineScope.launch {
                            downloadService.firstNotNull()
                                .tryResumeDownload(downloadItemAction.item)
                        }
                    },
                    onCancelDownloadClick = {
                        viewModel.cancelDownload()
                        coroutineScope.launch {
                            downloadService.firstNotNull().cancelDownload(downloadItemAction.item)
                        }
                    },
                    onCloseClick = {
                        viewModel.clearDownloadItemAction()
                    },
                )

            errorType != null ->
                ErrorBottomSheet(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    title = stringResource(errorType.titleRes),
                    desc = stringResource(errorType.descriptionRes),
                    onCancelClick = { viewModel.dismissError() },
                )
        }
    }
}

@Composable
fun SearchResultBottomSheetContent(
    item: SearchItem,
    myPlaceItem: MyPlaceItem?,
    state: SearchResultState,
    modifier: Modifier = Modifier,
    onDownloadItemClick: (DownloadItem, DownloadingState) -> Unit,
    onSaveMyPlace: (MyPlaceItem) -> Unit = {},
    onDeleteMyPlace: (MyPlaceItem) -> Unit = {},
    onNavigateClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(colorWhite)
            .padding(top = dividerThicknessMedium)
            .clickable(enabled = false) { },
    ) {
        HorizontalDivider(thickness = dividerThicknessMedium, color = colorBlack)

        Row(
            modifier = Modifier
                .wrapContentSize()
                .padding(start = 12.dp, top = 13.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.formattedTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = KompaktTypography900.titleMedium,
                )
                Text(
                    text = state.formattedDescription,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = KompaktTypography500.labelSmall,
                )
            }
            if (!BuildConfig.IS_PREMIERE_VERSION) {
                Icon(
                    modifier = Modifier
                        .clickable {
                            if (myPlaceItem == null) {
                                onSaveMyPlace(item.toMyPlaceItem())
                            } else {
                                onDeleteMyPlace(myPlaceItem)
                            }
                        },
                    painter = painterResource(
                        id = if (myPlaceItem != null) drawable.icon_star_filled
                        else drawable.icon_star_outlined
                    ),
                    contentDescription = null,
                )
            }
        }

        if (state.downloadItem != null) {
            if (state.downloadingState == DownloadingState.Default) {
                KompaktSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = .5.dp, start = 12.dp, bottom = 10.dp, end = 12.dp),
                    text = stringResource(string.maps_search_dialog_button_downloadregion),
                    iconResId = drawable.ic_download,
                    attributes = KompaktButtonAttributes.Large,
                    onClick = { onDownloadItemClick(state.downloadItem, state.downloadingState) },
                )
            } else {
                DownloadItem(
                    downloadingState = state.downloadingState,
                    downloadProgress = state.downloadProgress,
                    downloadStateDescription = state.getDownloadItemStateDescription(),
                    downloadSize = state.downloadItem.size,
                    onClick = { onDownloadItemClick(state.downloadItem, it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (BuildConfig.IS_PLAN_ROUTE_ENABLED) {
            KompaktSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                text = stringResource(id = string.maps_common_button_planroute),
                iconResId = drawable.icon_navigate,
                attributes = KompaktButtonAttributes.Large,
                onClick = onNavigateClick,
            )
        }
    }
}

@Composable
private fun NavigationSearchResultBottomSheetPreview(state: SearchResultState) {
    SearchResultBottomSheetContent(
        item = SearchItem(
            itemType = SearchItemType.POI,
            latLon = LatLon(0.0, 0.0),
            localName = "Nowe ZOO"
        ),
        myPlaceItem = null,
        state = state,
        onDownloadItemClick = { _, _ -> },
        onSaveMyPlace = {},
        onDeleteMyPlace = {},
        onNavigateClick = {},
    )
}

@Composable
@Preview
private fun NavigationSearchResultBottomSheetPreview() {
    NavigationSearchResultBottomSheetPreview(
        SearchResultState(formattedDescription = "Zoo | 260 km"),
    )
}

@Composable
@Preview
fun NavigationSearchResultWithDownloadBottomSheetPreview() {
    NavigationSearchResultBottomSheetPreview(
        SearchResultState(
            formattedDescription = "Zoo | 260 km",
            downloadItem = DownloadItem("", "", "", "", ""),
            downloadingState = DownloadingState.Default,
            downloadProgress = DownloadProgress(50.0, 134.0)
        ),
    )
}

@Composable
@Preview
fun NavigationSearchResultWithDownloadDownloadingBottomSheetPreview() {
    NavigationSearchResultBottomSheetPreview(
        SearchResultState(
            formattedDescription = "Zoo | 260 km",
            downloadItem = DownloadItem("", "", "", "64 MB", ""),
            downloadingState = DownloadingState.Downloading,
            downloadProgress = DownloadProgress(50.0, 134.0)
        ),
    )
}

@Composable
@Preview
fun NavigationSearchResultWithDownloadErrorBottomSheetPreview() {
    NavigationSearchResultBottomSheetPreview(
        SearchResultState(
            formattedDescription = "Zoo | 260 km",
            downloadItem = DownloadItem("", "", "", "64 MB", ""),
            downloadingState = DownloadingState.ErrorState.MemoryNotEnough,
            downloadProgress = DownloadProgress(50.0, 20.0)
        ),
    )
}
