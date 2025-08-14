package com.mudita.requiredmaps.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.service.bindDownloadService
import com.mudita.download.ui.DownloadItemAction
import com.mudita.download.ui.components.AllMapItemView
import com.mudita.download.ui.components.NetworkNotAvailable
import com.mudita.download.ui.dialogs.CancelDownloadDialog
import com.mudita.download.ui.dialogs.ConnectionErrorDialog
import com.mudita.download.ui.dialogs.MemoryErrorDialog
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.kompakt.commonUi.components.appBar.KompaktTopAppBar
import com.mudita.kompakt.commonUi.components.button.KompaktButtonAttributes
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.kompakt.commonUi.components.fakeScroll.KompaktLazyFakeScroll
import com.mudita.kompakt.commonUi.components.fakeScroll.canScroll
import com.mudita.map.common.R.drawable
import com.mudita.map.common.components.ErrorBottomSheet
import com.mudita.map.common.components.InfoCard
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.map.common.ui.topAppBarHeight
import com.mudita.map.common.utils.firstNotNull
import com.mudita.maps.frontitude.R.string
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun RequiredMapsScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: RequiredMapsViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value
    val downloadService = bindDownloadService(viewModel.action.filterNotNull())
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        Scaffold(
            topBar = {
                KompaktTopAppBar(
                    title = stringResource(string.maps_planningroute_screentitle_missingmaps),
                    titleTextStyle = KompaktTypography900.titleMedium,
                    navigationIconResId = drawable.ic_arrow_left_black,
                    barHeight = topAppBarHeight,
                    onNavigationIconClick = onBackClicked,
                    actionView = {
                        if (state.showDoneButton) {
                            KompaktSecondaryButton(
                                text = stringResource(string.common_label_done).uppercase(),
                                attributes = KompaktButtonAttributes.Small,
                                onClick = onBackClicked,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (state.showDownloadAllButton) {
                    Box(modifier = Modifier.background(colorWhite)) {
                        HorizontalDivider(thickness = dividerThicknessMedium, color = colorBlack)

                        KompaktPrimaryButton(
                            text = stringResource(string.common_button_cta_downloadall),
                            size = KompaktButtonAttributes.Large,
                            onClick = { viewModel.downloadAll() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            },
            containerColor = colorWhite,
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when {
                    state.showNetworkError ->
                        NetworkNotAvailable()

                    else ->
                        DownloadItems(
                            state = state,
                            onDownloadItem = { viewModel.download(it) },
                            onErrorClick = viewModel::handleDownloadError,
                            cancelDownload = { viewModel.handleCancelDownload(it) },
                            modifier = Modifier,
                        )
                }
            }
        }

        val errorType = state.errorType
        val downloadItemAction = state.downloadItemAction

        when {
            downloadItemAction is DownloadItemAction.Cancel ->
                CancelDownloadDialog(
                    mapName = downloadItemAction.item.name,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onKeepMap = { viewModel.dismissDownloadAction() },
                    onCancelDownload = {
                        coroutineScope.launch {
                            viewModel.cancelDownload(downloadItemAction.item)
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
                        viewModel.cancelDownload(downloadItemAction.item)
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
                        viewModel.cancelDownload(downloadItemAction.item)
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

    LaunchedEffect(state) {
        if (state.downloadItems.isNotEmpty() && state.downloadItems.all { it.downloaded }) {
            onBackClicked()
        }
    }
}

@Composable
fun DownloadItems(
    state: RequiredMapsState,
    onDownloadItem: (DownloadItem) -> Unit,
    onErrorClick: (DownloadItem, DownloadingState.ErrorState) -> Unit,
    cancelDownload: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier) {
        InfoCard(
            text = MissingMapsText.getText(),
            inlineContent = MissingMapsText.getInlineContent(),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        KompaktLazyFakeScroll(
            listState = listState,
        ) {
            itemsIndexed(
                items = state.downloadItems,
                key = { _, downloadItem -> downloadItem.fileName },
            ) { index, downloadItem ->
                AllMapItemView(
                    item = downloadItem,
                    downloadItemInfo = state,
                    canScroll = listState.canScroll,
                    isSearchMode = false,
                    enabled = true,
                    cancelDownload = { cancelDownload(downloadItem) },
                    onErrorClicked = { _, errorState -> onErrorClick(downloadItem, errorState) },
                    onItemClicked = { onDownloadItem(downloadItem) },
                )

                if (index != state.downloadItems.lastIndex) {
                    DashedHorizontalDivider(
                        color = colorBlack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private object MissingMapsText {
    private const val ID_INFO = "info"

    @Composable
    fun getText(): AnnotatedString =
        buildAnnotatedString {
            appendInlineContent(ID_INFO)
            append(stringResource(string.maps_routeplanning_notification_missingmapsarebased))
        }

    @Composable
    fun getInlineContent(): Map<String, InlineTextContent> = mapOf(
        ID_INFO to InlineTextContent(
            Placeholder(
                width = 28.sp,
                height = 18.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom,
            )
        ) {
            Image(painterResource(drawable.ic_info_circle), null)
        }
    )
}
