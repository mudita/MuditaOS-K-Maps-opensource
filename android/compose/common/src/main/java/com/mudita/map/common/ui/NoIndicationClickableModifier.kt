package com.mudita.map.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.noIndicationClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = this.composed {
    clickable(
        onClick = onClick,
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
    )
}
