package com.mudita.myplaces.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.kompakt.commonUi.util.extensions.conditional
import com.mudita.map.common.R
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.utils.capitalize
import java.util.UUID

@Composable
fun MyPlaceItemView(
    myPlaceItem: MyPlaceItem,
    isLast: Boolean,
    isScrollable: Boolean,
    onInfoClicked: (MyPlaceItem) -> Unit,
    onItemClicked: (MyPlaceItem) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onItemClicked(myPlaceItem)
            }
            .padding(top = 11.dp, start = if (isScrollable) 4.dp else 8.dp)
            .conditional(isScrollable) {
                offset(x = 8.dp)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.conditional(isScrollable.not()) {
                padding(end = 16.dp)
            }
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                myPlaceItem.title?.let {
                    Text(
                        text = it.capitalize(),
                        style = KompaktTypography900.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                myPlaceItem.formattedDesc?.let {
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = it.capitalize(),
                        style = KompaktTypography500.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                modifier = Modifier
                    .clickable {
                        onInfoClicked(myPlaceItem)
                    },
                painter = painterResource(id = R.drawable.icon_info),
                contentDescription = null
            )
        }
        if (isLast.not()) DashedHorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        else Spacer(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
@Preview
private fun MyPlaceItemViewPreview() {
    MyPlaceItemView(
        myPlaceItem = MyPlaceItem(
            id = UUID.randomUUID(),
            title = "Title",
            desc = "Desc",
            address = "Address"
        ),
        isLast = false,
        isScrollable = true,
        onInfoClicked = {},
        onItemClicked = {},
    )
}