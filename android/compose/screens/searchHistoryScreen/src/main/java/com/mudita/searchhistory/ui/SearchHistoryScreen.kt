package com.mudita.searchhistory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.kompakt.commonUi.components.fakeScroll.KompaktLazyFakeScroll
import com.mudita.map.common.model.SearchItemData
import com.mudita.searchhistory.R
import com.mudita.searchhistory.ui.dialogs.ClearHistoryDialog
import com.mudita.map.common.R as commonR

@Composable
fun SearchHistoryScreen(
    historyViewModel: SearchHistoryViewModel = hiltViewModel(),
    onBackClicked: () -> Unit = {},
    onItemClicked: (SearchItemData) -> Unit
) {

    val state by historyViewModel.uiState.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect("search_history") {
        historyViewModel.getHistory()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 8.dp),
            ) {
                KompaktIconButton(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 8.dp),
                    iconResId = if (showClearHistoryDialog) com.mudita.kompakt.commonUi.R.drawable.close
                        else commonR.drawable.ic_arrow_left_black
                ) {
                    if (showClearHistoryDialog)
                        showClearHistoryDialog = false
                    else
                        onBackClicked()
                }
                TitleView(
                    isClearDialogActive = showClearHistoryDialog,
                    itemsEmpty = state.items.isEmpty()
                ) { showClearHistoryDialog = true }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(2.dp)
                    .background(Color.Black)
            )

            if (state.items.isNotEmpty())
                HistoryItemsView(
                    items = state.items,
                    onItemClicked = onItemClicked,
                    formatDistance = { historyViewModel.formatDistance(it) }
                )
            else
                EmptyHistory()

        }
        if (showClearHistoryDialog)
            ClearHistoryDialog(
                modifier = Modifier.align(Alignment.BottomCenter),
                onBackClicked = { showClearHistoryDialog = false },
                onClearClicked = {
                    historyViewModel.clearHistory()
                    showClearHistoryDialog = false
                }
            )
    }

}

@Composable
private fun TitleView(
    isClearDialogActive: Boolean = false,
    itemsEmpty: Boolean = true,
    onClearClicked: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.wrapContentSize(),
            text = stringResource(id = R.string.search_history_title),
            style = KompaktTypography900.titleLarge,
        )
        if (!itemsEmpty && !isClearDialogActive)
            KompaktSecondaryButton(
                modifier = Modifier.padding(end = 16.dp),
                text = stringResource(id = R.string.clear_all),
                attributes = KompaktTheme.buttonStyle.small
            ) {
                onClearClicked()
            }
    }
}

@Composable
private fun HistoryItemsView(
    items: List<SearchItemData>,
    formatDistance: (SearchItemData) -> String,
    onItemClicked: (SearchItemData) -> Unit)
{
    KompaktLazyFakeScroll(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .fillMaxSize(),
    ) {
        itemsIndexed(items = items) { index, item ->
            SearchHistoryItemView(
                historyItem = item,
                isLast = index == items.lastIndex,
                formatDistance = { formatDistance(it) },
                onItemClicked = { onItemClicked.invoke(item) }
            )
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight(0.5f).padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.empty_history_title),
            style = KompaktTypography900.titleMedium
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text =stringResource(id = R.string.empty_history_text),
            style = KompaktTypography500.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}