package com.mudita.map.common.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ReturnFromAwaitPointerEventScope")
suspend fun PointerInputScope.awaitVerticalDragUp(
    threshold: Float = 0.1f,
): Int = awaitPointerEventScope {
    val initial = awaitFirstDown(false)
    val last = waitForUpOrCancellation() ?: initial
    val dy = last.position.y - initial.position.y
    val scaledDy = dy / min(1, size.height)
    max(-1, min(1, (scaledDy / threshold).toInt()))
}
