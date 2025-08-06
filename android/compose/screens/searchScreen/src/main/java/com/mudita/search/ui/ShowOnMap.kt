package com.mudita.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.search.R

@Composable
fun ShowOnMap(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick.invoke() }
    ) {
        KompaktIconButton(
            modifier = Modifier.padding(start = 8.dp),
            com.mudita.map.common.R.drawable.ic_location
        )
        Text(
            text = stringResource(id = R.string.show_on_map).uppercase(),
            style = KompaktTypography900.bodyMedium.copy(),
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}