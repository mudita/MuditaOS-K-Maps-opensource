package com.mudita.map.ui.routePlanning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.maps.frontitude.R

@Preview
@Composable
fun SelectLocation(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White)
            .padding(top = dividerThicknessMedium)
            .clickable(enabled = false) {  },
    ) {
        HorizontalDivider(thickness = dividerThicknessMedium, color = Color.Black)
        Column(
            modifier = Modifier
                .padding(bottom = 12.dp, top = 14.dp, start = 12.dp, end = 12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.maps_common_dialog_h1_selectlocationonmap),
                style = KompaktTypography900.titleMedium,
            )
            Text(
                text = stringResource(id = R.string.maps_common_dialog_body_taponthemapto),
                style = KompaktTypography500.labelSmall,
            )
        }
    }
}
