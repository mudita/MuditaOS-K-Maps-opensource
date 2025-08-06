package com.mudita.searchhistory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.map.common.model.SearchItemData
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider

@Composable
fun SearchHistoryItemView(
    historyItem: SearchItemData,
    isLast: Boolean,
    onItemClicked: (SearchItemData) -> Unit,
    formatDistance: (SearchItemData) -> String,
) {
    Column(
        modifier = Modifier.clickable {
            onItemClicked(historyItem)
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
            ) {
                Text(
                    text = historyItem.formattedTitle,
                    style = KompaktTypography900.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = historyItem.formattedDescription + formatDistance(historyItem),
                    style = KompaktTypography500.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isLast.not()) DashedHorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        }
    }
}
