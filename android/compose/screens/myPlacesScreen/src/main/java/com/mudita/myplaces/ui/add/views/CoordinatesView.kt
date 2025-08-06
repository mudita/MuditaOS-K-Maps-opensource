package com.mudita.myplaces.ui.add.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.map.common.R

@Composable
fun CoordinatesView(
    modifier: Modifier = Modifier,
    latitude: Double?,
    longitude: Double?,
) {
    Row(
        modifier = modifier.padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 16.dp),
            painter = painterResource(id = R.drawable.ic_location),
            contentDescription = null
        )
        Text(
            text = "${latitude ?: 0.0}, ${longitude ?: 0.0}",
            style = KompaktTypography500.bodyMedium
        )
    }
}