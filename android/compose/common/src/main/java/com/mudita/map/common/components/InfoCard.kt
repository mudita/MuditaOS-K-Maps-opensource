package com.mudita.map.common.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.colorBlack

@Composable
fun InfoCard(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
) {
    Text(
        text = text,
        style = KompaktTypography500.bodySmall,
        inlineContent = inlineContent,
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                onDrawBehind {
                    drawRoundRect(
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        color = colorBlack,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(
                                    2.dp.toPx(),
                                    2.dp.toPx(),
                                )
                            )
                        ),
                    )
                }
            }
            .padding(top = 8.dp, start = 12.dp, bottom = 10.dp, end = 12.dp)
    )
}

@Preview
@Composable
private fun InfoCardPreview() {
    InfoCard(
        text = buildAnnotatedString {
            append("Missing maps are based on a straight line from starting point to destination.")
        },
        modifier = Modifier.padding(16.dp)
    )
}
