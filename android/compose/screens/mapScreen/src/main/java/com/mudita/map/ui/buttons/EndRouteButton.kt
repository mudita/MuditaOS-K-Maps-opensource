package com.mudita.map.ui.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.util.NoRippleInteractionSource
import com.mudita.map.common.ui.borderSize
import com.mudita.maps.frontitude.R

@Composable
fun EndRouteButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.maps_common_topbar_button_endroute),
        style = KompaktTypography900.labelMedium,
        modifier = modifier
            .border(borderSize, colorBlack, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 5.5.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { NoRippleInteractionSource() },
                indication = null,
            )
    )
}

@Preview
@Composable
private fun EndRouteButtonPreview() {
    Row {
        EndRouteButton(onClick = {})
    }
}
