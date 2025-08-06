package com.mudita.download.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.colorBlack

@Composable
fun ProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        val shape = AbsoluteRoundedCornerShape(6.dp)
        LinearProgressIndicator(
            progress = { progress },
            strokeCap = StrokeCap.Butt,
            trackColor = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .border(BorderStroke(1.dp, colorBlack), shape)
                .clip(shape),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Preview
@Composable
private fun ProgressIndicatorPreview() {
    KompaktTheme {
        ProgressIndicator(progress = .5f, modifier = Modifier.padding(16.dp))
    }
}
