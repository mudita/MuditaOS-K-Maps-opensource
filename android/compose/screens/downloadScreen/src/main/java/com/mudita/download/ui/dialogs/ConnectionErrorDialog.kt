package com.mudita.download.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.map.common.components.VerticalConfirmationDialog
import com.mudita.maps.frontitude.R

@Composable
@Preview
fun ConnectionErrorDialog(
    modifier: Modifier = Modifier,
    onResumeDownloadClick: () -> Unit = {},
    onCancelDownloadClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    VerticalConfirmationDialog(
        title = stringResource(id = R.string.common_label_downloadinterrupted),
        description = stringResource(id = R.string.common_error_dialog_body_wewilltrytoresume),
        onCloseClick = onCloseClick,
        modifier = modifier,
    ) {
        VerticalConfirmationDialog.PrimaryButton(
            label = stringResource(id = R.string.common_dialog_button_resumedownload),
            onClick = onResumeDownloadClick,
        )

        VerticalConfirmationDialog.SecondaryButton(
            label = stringResource(id = R.string.common_dialog_button_canceldownload),
            onClick = onCancelDownloadClick,
        )
    }
}
