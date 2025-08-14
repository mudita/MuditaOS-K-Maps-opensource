package com.mudita.map.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.map.common.R

@Composable
fun Snackbar(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorWhite)
    ) {
        HorizontalDivider(thickness = 4.dp, color = colorBlack)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 17.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_success),
                contentDescription = null,
            )

            Text(
                text = text,
                color = colorBlack,
                style = KompaktTypography500.labelMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun SnackbarPreview() {
   KompaktTheme {
       Snackbar(text = "Information")
   } 
}

// endregion
