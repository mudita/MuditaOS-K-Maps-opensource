package com.mudita.map.missingregionoverlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mudita.map.common.R.drawable
import com.mudita.map.common.components.ErrorBottomSheet
import com.mudita.map.common.components.VerticalConfirmationDialog
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.map.common.utils.firstNotNull
import com.mudita.maps.frontitude.R.string
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun MissingRegionOverlayScreen(
    missingRegion: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MissingMapOverlayViewModel = hiltViewModel()
    viewModel.setMissingRegion(missingRegion)

    val state: MissingMapInfoState = viewModel.missingMapInfoState.collectAsState().value
    val downloadItem = state.downloadItem
    val downloadItemAction = state.downloadItemAction
    val errorType = state.errorType
    val downloadService = bindDownloadService(viewModel.action.filterNotNull())
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(onDismissRequest, state.mapDownloaded) {
        if (state.mapDownloaded) {
            viewModel.clearMissingMapInfo()
            onDismissRequest()
        }
    }

    DisposableEffect(missingRegion) {
        onDispose { viewModel.clearMissingMapInfo() }
    }

    Box(modifier = modifier) {
        if (downloadItem != null) {
            if (state.downloadingState == DownloadingState.Default) {
                IdleContent(
                    downloadItem = downloadItem,
                    onDownloadItemClick = viewModel::onDownloadItemClick,
                    onCancelClick = onDismissRequest,
                )
            } else {
                DownloadContent(
                    downloadItem = downloadItem,
                    state = state,
                    onDownloadItemClick = viewModel::onDownloadItemClick,
                )
            }
        }

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
private fun IdleContent(
    downloadItem: DownloadItem,
    onDownloadItemClick: (DownloadItem, DownloadingState) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VerticalConfirmationDialog(
        title = stringResource(string.maps_mapview_dialog_h1_missingregionmap),
        description = stringResource(
            string.maps_common_error_body_downloadtheregionmaptosee,
            downloadItem.name,
        ),
        onCloseClick = onCancelClick,
        modifier = modifier,
    ) {
        VerticalConfirmationDialog.SecondaryButton(
            label = stringResource(string.maps_search_dialog_button_downloadregion),
            onClick = { onDownloadItemClick(downloadItem, DownloadingState.Default) },
            iconResId = drawable.ic_download,
        )
    }
}

@Composable
private fun DownloadContent(
    downloadItem: DownloadItem,
    state: MissingMapInfoState,
    modifier: Modifier = Modifier,
    onDownloadItemClick: (DownloadItem, DownloadingState) -> Unit,
) {
    Column(
        modifier = modifier
            .background(colorWhite)
            .padding(top = dividerThicknessMedium)
            .background(colorBlack)
            .padding(top = dividerThicknessMedium)
            .background(colorWhite)
            .clickable(enabled = false) { },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 13.dp, end = 12.dp, bottom = 12.dp)
        ) {
            Text(
                text = downloadItem.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = KompaktTypography900.titleMedium,
            )
            Text(
                text = downloadItem.size,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = KompaktTypography500.labelSmall,
            )
        }

        DownloadItem(
            downloadingState = state.downloadingState,
            downloadProgress = state.downloadProgress,
            downloadStateDescription = state.getDownloadItemStateDescription(),
            onClick = { onDownloadItemClick(downloadItem, it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        )
    }
}

@Composable
@Preview
private fun MissingRegionOverlayIdlePreview() {
    IdleContent(
        downloadItem = DownloadItem("West Pomeranian Voivodeship", "", "", "123 MB", ""),
        onDownloadItemClick = { _, _ -> },
        onCancelClick = {},
    )
}

@Composable
private fun MissingRegionDownloadBottomSheetPreview(state: MissingMapInfoState) {
    DownloadContent(
        downloadItem = state.downloadItem!!,
        state = state,
        onDownloadItemClick = { _, _ -> },
    )
}

@Composable
@Preview
private fun MissingRegionDownloadingBottomSheetPreview() {
    MissingRegionDownloadBottomSheetPreview(
        MissingMapInfoState(
            downloadItem = DownloadItem("West Pomeranian Voivodeship", "", "", "123 MB", ""),
            downloadingState = DownloadingState.Downloading,
            downloadProgress = DownloadProgress(50.0, 134.0)
        ),
    )
}

@Composable
@Preview
private fun MissingRegionDownloadErrorBottomSheetPreview() {
    MissingRegionDownloadBottomSheetPreview(
        MissingMapInfoState(
            downloadItem = DownloadItem("West Pomeranian Voivodeship", "", "", "123 MB", ""),
            downloadingState = DownloadingState.ErrorState.MemoryNotEnough,
            downloadProgress = DownloadProgress(50.0, 20.0)
        ),
    )
}
