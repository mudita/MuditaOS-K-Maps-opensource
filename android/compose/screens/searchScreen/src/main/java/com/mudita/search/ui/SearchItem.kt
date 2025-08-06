package com.mudita.search.ui

import com.mudita.map.common.R as commonR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.map.common.model.SearchItemData
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.utils.conditional
import com.mudita.search.ui.utils.isCorrectDrawable

@Composable
fun SearchRowItem(
    searchItemData: SearchItemData,
    getFormattedDistance: (SearchItemData) -> String,
    isLast: Boolean,
    onItemClicked: (SearchItemData) -> Unit
) {
    val categoryIcon = if (isCorrectDrawable(LocalContext.current, searchItemData.iconRes))
        painterResource(id = searchItemData.iconRes)
    else
        painterResource(id = com.mudita.map.common.R.drawable.icon_search)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onItemClicked(searchItemData) }
    ) {
        Icon(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(28.dp),
            painter = categoryIcon,
            contentDescription = null
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.5.dp, bottom = 16.dp)
                        .weight(1F)
                ) {
                    val description = searchItemData.formattedDescription + getFormattedDistance(searchItemData)
                    Text(
                        modifier = Modifier
                            .conditional(description.isBlank()) {
                                padding(vertical = 12.dp)
                            },
                        text = searchItemData.formattedTitle,
                        style = KompaktTypography900.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (description.isNotBlank()) {
                        Text(
                            text = searchItemData.formattedDescription + getFormattedDistance(searchItemData),
                            style = KompaktTypography500.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                    painter = painterResource(id = commonR.drawable.ic_arrow_right_small),
                    contentDescription = null
                )
            }
            if (isLast.not()) {
                DashedHorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@Preview
private fun SearchItemsPreview() {
    KompaktTheme {
        Column {
            SearchRowItem(
                searchItemData = SearchItemData.History(
                    iconRes = com.mudita.map.common.R.drawable.mx_mudita_shop,
                    title = "Accommodation",
                    itemType = SearchItemType.HISTORY,
                    searchQuery = ""
                ),
                getFormattedDistance = { "7.09 km" },
                isLast = false,
                onItemClicked = {},
            )
            SearchRowItem(
                searchItemData = SearchItemData.History(
                    iconRes = com.mudita.map.common.R.drawable.mm_mudita_charging_station,
                    title = "Charging station",
                    desc = "Transportation",
                    itemType = SearchItemType.POI,
                    searchQuery = ""
                ),
                getFormattedDistance = { "10 km" },
                isLast = false,
                onItemClicked = {},
            )
            SearchRowItem(
                searchItemData = SearchItemData.Category(
                    iconRes = com.mudita.map.common.R.drawable.mx_mudita_sport,
                    title = "10",
                    desc = "Kraków",
                    address = "Worcella",
                    isPoiFilter = true,
                ),
                getFormattedDistance = { "" },
                isLast = false,
                onItemClicked = {},
            )
            SearchRowItem(
                searchItemData = SearchItemData.Category(
                    iconRes = com.mudita.map.common.R.drawable.mm_emergency,
                    title = "Emergency",
                    desc = null,
                    isPoiFilter = true,
                ),
                getFormattedDistance = { "" },
                isLast = true,
                onItemClicked = {},
            )
        }
    }
}