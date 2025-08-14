package com.mudita.map.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.colorWhite

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    text: String,
) {
    Column(
        modifier.background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(thickness = 3.dp, color = Color.Black)
        Text(
            modifier = Modifier.background(color = colorWhite).padding(24.dp),
            text = text,
            style = KompaktTypography500.titleMedium,
        )
    }
}

@Preview
@Composable
fun LoadingViewPreview() {
    LoadingView(Modifier, text = "Loading...")
}