package com.mudita.download.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.map.common.components.VerticalConfirmationDialog
import com.mudita.maps.frontitude.R

@Composable
fun CancelDownloadDialog(
    mapName: String,
    onKeepMap: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VerticalConfirmationDialog(
        title = stringResource(R.string.common_dialog_h1_canceldownload),
        description = stringResource(R.string.maps_downloaded_dialog_body_youcanalwaysdownload, mapName),
        modifier = modifier,
    ) {
        VerticalConfirmationDialog.PrimaryButton(
            label = stringResource(R.string.common_dialog_button_canceldownload),
            onClick = onCancelDownload
        )

        VerticalConfirmationDialog.SecondaryButton(
            label = stringResource(R.string.common_dialog_button_back),
            onClick = onKeepMap
        )
    }
}

@Preview
@Composable
private fun CancelDownloadDialogPreview() {
    KompaktTheme {
        CancelDownloadDialog(
            mapName = "Pomeranian Voivodeship",
            onKeepMap = { },
            onCancelDownload = { },
        )
    }
}