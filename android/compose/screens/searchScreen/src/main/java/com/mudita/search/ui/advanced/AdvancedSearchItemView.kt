package com.mudita.search.ui.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorGray_5
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.map.common.R
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.search.repository.model.SearchCityItem
import net.osmand.search.core.ObjectType

@Composable
fun AdvancedSearchItemView(
    item: SearchCityItem,
    index: Int,
    lastItemIndex: Int,
    onItemClick: () -> Unit,
    onIncreaseRadius: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 4.dp)
            .clickable { onItemClick() }
    ) {
        Icon(
            painter = when (item.sr.objectType) {
                ObjectType.POSTCODE -> painterResource(id = R.drawable.ic_post_code)
                else -> painterResource(id = R.drawable.mx_mudita_city)
            },
            contentDescription = null,
            modifier = Modifier.weight(0.75f)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f)
        ) {
            Text(
                text = item.localName,
                style = KompaktTypography900.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = if (item.distance >= 1)
                    stringResource(com.mudita.search.R.string.distance_km, item.distance)
                else
                    stringResource(com.mudita.search.R.string.distance_m, item.distance * OsmAndFormatter.METERS_IN_KILOMETER),
                style = KompaktTypography500.labelSmall
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right_small),
            contentDescription = null,
            modifier = Modifier.weight(1f),
        )
    }
    DashedHorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 52.dp),
        color = colorGray_5,
        strokeWidth = 1.dp
    )
    if (lastItemIndex == index) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IncreaseSearchRadius { onIncreaseRadius() }
        }
    }
}