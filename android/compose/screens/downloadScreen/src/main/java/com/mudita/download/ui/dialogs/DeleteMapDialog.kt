package com.mudita.download.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.map.common.components.VerticalConfirmationDialog
import com.mudita.maps.frontitude.R

@Composable
@Preview
fun DeleteMapDialog(
    modifier: Modifier = Modifier,
    onKeepMap: () -> Unit = {},
    onRemoveMap: () -> Unit = {}
) {
    VerticalConfirmationDialog(
        title = stringResource(id = R.string.map_common_dialog_h1_deletemap),
        description = stringResource(id = R.string.maps_common_dialog_body_youllneedtodownloadthe),
        modifier = modifier,
    ) {
        VerticalConfirmationDialog.PrimaryButton(
            label = stringResource(id = R.string.maps_common_dialog_button_deletemap),
            onClick = onRemoveMap,
        )

        VerticalConfirmationDialog.SecondaryButton(
            label = stringResource(id = R.string.common_dialog_button_cancel),
            onClick = onKeepMap,
        )
    }
}