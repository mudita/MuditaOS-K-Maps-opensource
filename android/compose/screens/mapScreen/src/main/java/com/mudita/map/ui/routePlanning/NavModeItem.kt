package com.mudita.map.ui.routePlanning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorGray_11
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.util.NoRippleInteractionSource
import com.mudita.map.repository.NavigationModeItem

@Composable
fun NavModeItem(
    item: NavigationModeItem,
    isChecked: Boolean,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            contentPadding = PaddingValues(8.dp),
            shape = RoundedCornerShape(40.dp),
            border = BorderStroke(width = 2.dp, color = colorBlack),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isChecked) colorBlack else colorWhite,
                contentColor = colorBlack,
                disabledContainerColor = colorWhite,
                disabledContentColor = colorGray_11,
            ),
            interactionSource = NoRippleInteractionSource(),
            onClick = { onClick() }
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(id = item.icon),
                contentDescription = item.desc,
                tint = if (isChecked) colorWhite else colorBlack
            )
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(id = item.title),
            style = KompaktTypography500.labelSmall,
        )
    }
}

@Composable
@Preview(backgroundColor = 0xffffffff, showBackground = true)
fun NavModeItemPreview() {
    Row(modifier = Modifier.padding(24.dp)) {
        NavModeItem(item = NavigationModeItem.Walking, isChecked = false)
        Spacer(modifier = Modifier.width(24.dp))
        NavModeItem(item = NavigationModeItem.Walking, isChecked = true)
    }
}