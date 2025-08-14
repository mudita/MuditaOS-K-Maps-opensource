package com.mudita.download.ui

import com.mudita.map.common.R as commonR
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.TabRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.mudita.download.R
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadRegions
import com.mudita.download.repository.models.Downloadable
import com.mudita.download.service.DownloadService
import com.mudita.download.ui.dialogs.CancelDownloadDialog
import com.mudita.download.ui.dialogs.ConnectionErrorDialog
import com.mudita.download.ui.dialogs.MemoryErrorDialog
import com.mudita.download.ui.tabs.AllMapsTab
import com.mudita.download.ui.tabs.DownloadedTab
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorGray_7
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.kompakt.commonUi.components.appBar.KompaktTopAppBar
import com.mudita.map.common.components.ErrorBottomSheet
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.ui.topAppBarHeight
import com.mudita.maps.frontitude.R.string
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DownloadScreen(
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    onBackClicked: () -> Unit = {},
    getIndexFileNames: () -> Map<String, String>,
    getIndexFiles: () -> Map<String, File>,
    getLocalPath: (String?) -> File,
    getSdCardPath: (String?) -> File?,
    getNameToDisplay: (fileName: String) -> String,
    reloadIndexFiles: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val downloadState by downloadViewModel.uiState.collectAsState()
    val action by downloadViewModel.action.collectAsState()
    var searchMode by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var startDownloadCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var connectionErrorDownloadable by remember { mutableStateOf<Downloadable?>(null) }
    var downloadingToCancel: Downloadable? by remember { mutableStateOf(null) }
    var memoryErrorDownloadable by remember { mutableStateOf<Downloadable?>(null) }
    val allMapsListState = rememberLazyListState()
    val isDownloadedTab = pagerState.currentPage == 0

    val context: Context = LocalContext.current
    var downloadService: DownloadService? by remember(context) { mutableStateOf(null) }
    var isDownloadServiceBound by remember { mutableStateOf(false) }
    val serviceConnection: ServiceConnection = remember(context) {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                downloadService = (service as DownloadService.LocalBinder).getService()
                startDownloadCallback?.invoke()
                startDownloadCallback = null
                downloadService?.downloadSuccessTrigger?.onEach {
                    reloadIndexFiles()
                    downloadViewModel.getLocalIndexData(
                        readFiles = true,
                        indexFileNames = getIndexFileNames(),
                        indexFiles = getIndexFiles(),
                        getLocalPath = getLocalPath,
                        getSdCardPath = getSdCardPath
                    )
                }?.launchIn(coroutineScope)
                isDownloadServiceBound = true
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                downloadService = null
                isDownloadServiceBound = false
            }
        }
    }

    val onError = { downloadable: Downloadable, errorState: DownloadingState.ErrorState? ->
        when (errorState) {
            is DownloadingState.ErrorState.InternetConnectionError,
            is DownloadingState.ErrorState.InternetConnectionRetryFailed ->
                connectionErrorDownloadable = downloadable

            is DownloadingState.ErrorState.IoError,
            is DownloadingState.ErrorState.MemoryNotEnough ->
                memoryErrorDownloadable = downloadable

            else -> {
                connectionErrorDownloadable = null
                memoryErrorDownloadable = null
            }

        }
    }

    LaunchedEffect(downloadViewModel) {
        reloadIndexFiles()
        downloadViewModel.errorNotifications
            .collect { (downloadable, errorState) -> onError(downloadable, errorState) }
    }

    DisposableEffect(context, serviceConnection) {
        onDispose { if (isDownloadServiceBound) context.unbindService(serviceConnection) }
    }

    LaunchedEffect(downloadState) {
        if (
            downloadState.downloadingStateMap
                .any { (_, value) -> value.isError || value.isCancelable } && !isDownloadServiceBound
        ) {
            context.bindDownloadService(serviceConnection)
        }
    }

    BackHandler(true) {
        when {
            action != DownloadViewModel.Action.Empty -> downloadViewModel.onActionCanceled()
            downloadingToCancel != null -> return@BackHandler
            searchMode -> handleSearchMode(downloadViewModel, downloadState.isSearchResultEmpty, downloadState.downloadIndexes.isEmpty())
            else -> handleNormalMode(pagerState, downloadViewModel, onBackClicked)
        }
    }

    when (action) {
        is DownloadViewModel.Action.Download -> {
            val act = action as DownloadViewModel.Action.Download
            val map = act.map
            if (downloadService == null) {
                context.startForegroundService(Intent(context, DownloadService::class.java))
                context.bindDownloadService(serviceConnection)
                startDownloadCallback = { downloadService?.addMapToDownloadQueue(map) }
            } else {
                downloadService?.addMapToDownloadQueue(map)
            }
            downloadViewModel.onDownloadStarted()
        }

        is DownloadViewModel.Action.CancelDownload -> {
            val act = action as DownloadViewModel.Action.CancelDownload
            val item = act.map
            downloadingToCancel = item
        }

        is DownloadViewModel.Action.CancelSearch -> {
            searchMode = false
            downloadViewModel.onActionCanceled()
        }

        is DownloadViewModel.Action.CancelEditMode -> {
            editMode = false
            downloadViewModel.onActionCanceled()
        }

        else -> {}
    }

    val tabs = listOf(
        TabRowItem(
            title = stringResource(id = string.maps_common_tabitem_downloaded),
            screen = {
                if (downloadState.isLoadingDownloadedItems) return@TabRowItem
                DownloadedTab(
                    maps = downloadState.downloadedItems.toList(),
                    getNameToDisplay = getNameToDisplay,
                    updateMap = { downloadViewModel.updateMap(it) },
                    onItemClicked = {
                        downloadViewModel.onItemClicked(
                            resource = it,
                            isSearchMode = false,
                            firstVisibleItemIndex = allMapsListState.firstVisibleItemIndexRounded,
                        )
                    },
                    onGoToAllMapsClicked = {
                        coroutineScope.launch { pagerState.scrollToPage(1) }
                        editMode = false
                    },
                    cancelDownload = { downloadService?.cancelDownload(it) },
                    isEditMode = editMode,
                    onErrorClicked = { mapFile, errorState ->
                        downloadViewModel.findDownloadItem(mapFile)?.also {
                            onError(it, errorState)
                        }
                    }
                )
            }
        ),
        TabRowItem(
            title = stringResource(id = string.maps_common_tabitem_allmaps),
            screen = {
                AllMapsTab(
                    downloadState = downloadState,
                    isSearchMode = searchMode,
                    listState = allMapsListState,
                    onErrorClicked = onError,
                    cancelDownload = { downloadViewModel.cancelDownload(it) },
                    onItemClicked = {
                        downloadViewModel.onItemClicked(
                            resource = it,
                            isSearchMode = searchMode,
                            firstVisibleItemIndex = allMapsListState.firstVisibleItemIndexRounded,
                        )
                    }
                )
            }
        ),
    )

    if (downloadState.downloadedItems.isEmpty()) {
        downloadViewModel.getLocalIndexData(
            readFiles = true,
            indexFileNames = getIndexFileNames(),
            indexFiles = getIndexFiles(),
            getLocalPath = getLocalPath,
            getSdCardPath = getSdCardPath
        )
    }

    Box(
        modifier = Modifier.imePadding().background(colorWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!searchMode) {
                KompaktTopAppBar(
                    title = if (isDownloadedTab) stringResource(id = string.common_label_managemaps) else downloadState.getScreenTitle(),
                    navigationIconResId = if (editMode) commonR.drawable.ic_cancel else commonR.drawable.ic_arrow_left_black,
                    barHeight = topAppBarHeight,
                    onNavigationIconClick = {
                        when {
                            action != DownloadViewModel.Action.Empty -> downloadViewModel.onActionCanceled()
                            editMode && downloadingToCancel == null -> {
                                editMode = false
                                downloadViewModel.refreshLocalIndexAfterEdit(
                                    indexFileNames = getIndexFileNames(),
                                    indexFiles = getIndexFiles(),
                                    getLocalPath = getLocalPath,
                                    getSdCardPath = getSdCardPath
                                )
                            }

                            downloadingToCancel != null -> Unit
                            searchMode -> handleSearchMode(
                                downloadViewModel,
                                downloadState.isSearchResultEmpty,
                                downloadState.downloadIndexes.isEmpty()
                            )

                            else -> handleNormalMode(
                                pagerState,
                                downloadViewModel,
                                onBackClicked
                            )
                        }
                    },
                    actionView = {
                        val isAllMapsTab = !isDownloadedTab
                        val notInEditMode = !editMode
                        val downloadedTabNotEmpty = downloadState.downloadedItems.toList().isNotEmpty()
                        val shouldShowIconInDownloadedTab = isDownloadedTab && notInEditMode && downloadedTabNotEmpty
                        val shouldShowIconInAllMapsTab = isAllMapsTab && downloadState.isRootIndex

                        if (shouldShowIconInDownloadedTab || shouldShowIconInAllMapsTab) {
                            KompaktIconButton(
                                modifier = Modifier.padding(end = 8.dp),
                                iconSize = 28.dp,
                                iconResId = if (isDownloadedTab) R.drawable.ic_edit else commonR.drawable.icon_search,
                                enabled = downloadingToCancel == null,
                            ) {
                                if (isDownloadedTab && downloadingToCancel == null) {
                                    editMode = true
                                } else if (!isDownloadedTab) {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(1)
                                        delay(200)
                                        searchFocusRequester.requestFocus()
                                    }
                                    searchMode = true
                                    downloadViewModel.obtainIntent(DownloadViewModel.Intent.SearchQueryChange(""))
                                }
                            }
                        }
                    }
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .height(65.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    KompaktIconButton(
                        iconResId = commonR.drawable.ic_arrow_left_black,
                        modifier = Modifier.padding(top = 3.dp),
                        onClick = { handleSearchMode(downloadViewModel, downloadState.isSearchResultEmpty, downloadState.downloadIndexes.isEmpty()) },
                    )

                    BasicTextField(
                        value = downloadState.searchQuery,
                        onValueChange = { downloadViewModel.obtainIntent(DownloadViewModel.Intent.SearchQueryChange(it)) },
                        textStyle = KompaktTypography500.titleMedium,
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            innerTextField()
                            if (downloadState.searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(id = string.maps_common_searchbar_placeholder_searchformaps),
                                    color = colorGray_7,
                                    style = KompaktTypography500.titleMedium,
                                )
                            }
                        },
                        modifier = Modifier
                            .focusRequester(searchFocusRequester)
                            .weight(1f),
                    )

                    if (downloadState.searchQuery.isNotEmpty()) {
                        KompaktIconButton(
                            iconResId = R.drawable.ic_cancel_small,
                            iconSize = 20.dp,
                        ) {
                            downloadViewModel.obtainIntent(DownloadViewModel.Intent.SearchQueryChange(""))
                            keyboardController?.show()
                        }
                    }
                }

                HorizontalDivider(thickness = 3.dp, color = colorBlack)
            }

            if (!searchMode && !editMode && downloadState.isRootIndex) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                            color = colorBlack
                        )
                    },
                    backgroundColor = colorWhite,
                ) {
                    tabs.forEachIndexed { index, item ->
                        Text(
                            text = item.title,
                            style = if (index == pagerState.currentPage) KompaktTypography900.labelMedium else KompaktTypography500.labelMedium,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    if (downloadingToCancel == null && connectionErrorDownloadable == null) {
                                        if (action != DownloadViewModel.Action.Empty) downloadViewModel.onActionCanceled()
                                        coroutineScope.launch { pagerState.scrollToPage(index) }
                                    }
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = colorBlack)
            }

            HorizontalPager(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                count = 2,
                state = pagerState,
                userScrollEnabled = false
            ) {
                tabs[pagerState.currentPage].screen()
            }
        }

        if (action != DownloadViewModel.Action.Empty) {
            val (title, desc) = when (action) {
                is DownloadViewModel.Action.ShowNetworkError -> Pair(
                    stringResource(id = string.common_error_dialog_h1_downloadcantstart),
                    stringResource(id = string.common_error_dialog_body_makesureyouareconnected)
                )

                is DownloadViewModel.Action.ShowWifiNetworkError -> Pair(
                    stringResource(id = string.common_error_dialog_h1_downloadcantstart),
                    stringResource(id = string.common_error_dialog_body_connecttowifi)
                )

                is DownloadViewModel.Action.ShowMemoryError -> Pair(
                    stringResource(id = string.common_error_dialog_h1_downloadcantstart),
                    stringResource(id = string.common_error_dialog_body_memoryisfull)
                )

                else -> null to null
            }
            if (title != null && desc != null) {
                ErrorBottomSheet(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    title = title,
                    desc = desc,
                    onCancelClick = { downloadViewModel.onActionCanceled() },
                )
            }
        }

        val cancelDownload = { downloadable: Downloadable ->
            downloadService?.cancelDownload(
                downloadable,
                downloadViewModel.isUpdatedAvailable(downloadable)
            )
            downloadViewModel.onDownloadCanceled()
        }

        connectionErrorDownloadable?.also { downloadable ->
            ConnectionErrorDialog(
                modifier = Modifier.align(Alignment.BottomCenter),
                onResumeDownloadClick = {
                    downloadService?.tryResumeDownload(downloadable)
                    connectionErrorDownloadable = null
                },
                onCancelDownloadClick = {
                    cancelDownload(downloadable)
                    connectionErrorDownloadable = null
                },
                onCloseClick = {
                    connectionErrorDownloadable = null
                },
            )
        }
        downloadingToCancel
            ?.also { downloadable ->
                CancelDownloadDialog(
                    mapName = downloadable.getName(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onKeepMap = {
                        downloadViewModel.onActionCanceled()
                        downloadingToCancel = null
                    },
                    onCancelDownload = {
                        cancelDownload(downloadable)
                        downloadingToCancel = null
                    }
                )
            }

        memoryErrorDownloadable?.also { downloadable ->
            MemoryErrorDialog(
                modifier = Modifier.align(Alignment.BottomCenter),
                onTryAgainClick = {
                    downloadService?.tryResumeDownload(downloadable)
                },
                onCancelDownloadClick = {
                    cancelDownload(downloadable)
                    memoryErrorDownloadable = null
                },
                onCloseClick = {
                    memoryErrorDownloadable = null
                },
            )
        }
    }
}

private fun handleSearchMode(
    downloadViewModel: DownloadViewModel,
    isSearchResultEmpty: Boolean,
    isDownloadIndexesEmpty: Boolean
) {
    if (!downloadViewModel.goToPreviousPage() || isSearchResultEmpty || isDownloadIndexesEmpty) {
        downloadViewModel.cancelSearchMode()
        downloadViewModel.clearSearchState()
    }
}

@OptIn(ExperimentalPagerApi::class)
private fun handleNormalMode(
    pagerState: PagerState,
    downloadViewModel: DownloadViewModel,
    onBackClicked: () -> Unit,
) {
    val isFirstPage = pagerState.currentPage == 1
    val cantGoBack = isFirstPage && !downloadViewModel.goToPreviousPage()
    if (cantGoBack || isFirstPage.not()) {
        onBackClicked()
    }
}

data class TabRowItem(
    val title: String,
    val screen: @Composable () -> Unit,
)

@Composable
private fun DownloadViewModel.DownloadState.getScreenTitle(): String =
    screenTitle ?: stringResource(id = string.common_label_managemaps)

@Composable
private fun Downloadable.getName(): String = when (this) {
    is DownloadItem -> name
    is DownloadRegions -> stringResource(id = string.maps_downloaded_label_placeholder_allregions)
}

fun Context.bindDownloadService(serviceConnection: ServiceConnection) {
    bindService(
        Intent(this, DownloadService::class.java),
        serviceConnection,
        Context.BIND_AUTO_CREATE
    )
}

private val LazyListState.firstVisibleItemIndexRounded: Int
    get() = firstVisibleItemIndex + if (firstVisibleItemScrollOffset > 0) 1 else 0
