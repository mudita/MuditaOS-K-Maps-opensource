package com.mudita.menu.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.menu.repository.model.MenuItem
import com.mudita.map.common.R as commonR

@Composable
fun MenuItemView(
    menuItem: MenuItem,
    isLast: Boolean,
    showAlert: Boolean,
    onItemClicked: (MenuItem) -> Unit
) {
    val itemIcon = painterResource(id = menuItem.icon)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onItemClicked(menuItem) }
            .fillMaxWidth()
    ) {
        Icon(
            modifier = Modifier.padding(start = 16.dp),
            painter = itemIcon,
            contentDescription = null
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 1.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = menuItem.title),
                    style = KompaktTypography500.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showAlert) {
                    Icon(
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        painter = painterResource(id = commonR.drawable.ic_alert),
                        contentDescription = null
                    )
                }
                Icon(
                    modifier = Modifier.padding(end = 16.dp),
                    painter = painterResource(id = commonR.drawable.ic_arrow_right_small),
                    contentDescription = null
                )
            }
            if (isLast.not()) {
                DashedHorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorBlack
                )
            }
        }
    }
}

@Composable
@Preview
fun SettingItemViewPreview() {
    MenuItemView(
        menuItem = MenuItem.ManageMaps,
        isLast = false,
        showAlert = true,
        onItemClicked = {}
    )
}