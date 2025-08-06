package com.mudita.map.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ErrorBottomSheet(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    onCancelClick: () -> Unit = {},
) {
    VerticalConfirmationDialog(
        title = title,
        description = desc,
        onCloseClick = onCancelClick,
        modifier = modifier,
    )
}

@Composable
@Preview
fun ErrorBottomSheetPreview() {
    ErrorBottomSheet(
        title = "Download can't start",
        desc = "Connect to Wi-Fi or turn off Wi-Fi only mode in settings."
    ) {}
}