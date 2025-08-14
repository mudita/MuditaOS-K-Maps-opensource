package com.mudita.download.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.map.common.components.VerticalConfirmationDialog
import com.mudita.maps.frontitude.R

@Composable
@Preview
fun MemoryErrorDialog(
    modifier: Modifier = Modifier,
    onTryAgainClick: () -> Unit = {},
    onCancelDownloadClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    VerticalConfirmationDialog(
        title = stringResource(id = R.string.common_label_downloadfailed),
        description = stringResource(id = R.string.common_error_dialog_body_memoryisfull),
        onCloseClick = onCloseClick,
        modifier = modifier,
    ) {
        VerticalConfirmationDialog.PrimaryButton(
            label = stringResource(id = R.string.common_dialog_button_canceldownload),
            onClick = onCancelDownloadClick,
        )

        VerticalConfirmationDialog.SecondaryButton(
            label = stringResource(id = R.string.common_dialog_button_tryagain),
            onClick = onTryAgainClick,
        )
    }
}
