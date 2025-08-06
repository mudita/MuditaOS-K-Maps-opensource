package com.mudita.download.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.LocalIndex
import com.mudita.download.repository.models.MapFile
import com.mudita.download.repository.models.Resource
import com.mudita.download.ui.DownloadViewModel
import com.mudita.download.ui.components.DownloadedMapItemView
import com.mudita.download.ui.dialogs.DeleteMapDialog
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorGray_5
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.kompakt.commonUi.components.fakeScroll.KompaktLazyFakeScroll
import com.mudita.kompakt.commonUi.components.fakeScroll.canScroll
import com.mudita.map.common.download.DownloadingState
import com.mudita.maps.frontitude.R

@Composable
fun DownloadedTab(
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    maps: List<MapFile> = emptyList(),
    getNameToDisplay: (fileName: String) -> String,
    updateMap: (localMap: LocalIndex) -> Unit,
    onItemClicked: (Resource) -> Unit,
    onGoToAllMapsClicked: () -> Unit,
    cancelDownload: (DownloadItem) -> Unit,
    onErrorClicked: (MapFile, DownloadingState.ErrorState) -> Unit,
    isEditMode: Boolean = false
) {
    val downloadState by downloadViewModel.uiState.collectAsState()
    var mapToDelete: MapFile? by remember { mutableStateOf(null) }
    val listState = rememberLazyListState()

    if (maps.isEmpty()) {
        DownloadedMapsEmptyScreen(
            onBrowseMapsClick = onGoToAllMapsClicked
        )
    } else {
        Box {
            KompaktLazyFakeScroll(
                listState = listState,
                content = {
                    items(maps) { mapItem ->
                        DownloadedMapItemView(
                            item = mapItem,
                            downloadViewModel = downloadViewModel,
                            downloadState = downloadState,
                            isEditMode = isEditMode,
                            canScroll = listState.canScroll,
                            getNameToDisplay = getNameToDisplay,
                            mapToDelete = { mapToDelete = it },
                            updateMap = { updateMap(it) },
                            onItemClicked = { onItemClicked(it) },
                            onErrorClicked = onErrorClicked
                        )
                        if (maps.indexOf(mapItem) != maps.lastIndex) {
                            DashedHorizontalDivider(
                                color = colorGray_5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            )
            mapToDelete?.let {
                DeleteMapDialog(
                    onKeepMap = { mapToDelete = null },
                    onRemoveMap = {
                        when (it) {
                            is DownloadItem -> {
                                cancelDownload(it)
                                downloadViewModel.onDownloadCanceled()
                            }
                            is LocalIndex -> {
                                downloadViewModel.deleteMap(it)
                            }
                        }
                        mapToDelete = null
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun DownloadedMapsEmptyScreen(
    onBrowseMapsClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 82.dp, end = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.maps_downloaded_emptystate_h1_theworldawaits),
            style = KompaktTypography900.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.maps_downloaded_emptystate_body_browseorsearch),
            style = KompaktTypography500.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        KompaktSecondaryButton(
            text = stringResource(id = R.string.maps_downloaded_button_cta_browsemaps),
            modifier = Modifier.padding(top = 24.dp),
            onClick = onBrowseMapsClick,
        )
    }
}

@Composable
@Preview(backgroundColor = 0xffffffff, showBackground = true)
fun DownloadedMapsEmptyScreenPreview() {
    DownloadedMapsEmptyScreen(
        onBrowseMapsClick = {}
    )
}
