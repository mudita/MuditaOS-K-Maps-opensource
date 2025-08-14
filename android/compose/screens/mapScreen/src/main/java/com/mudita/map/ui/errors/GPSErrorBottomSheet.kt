package com.mudita.map.ui.errors

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.map.common.components.VerticalConfirmationDialog
import com.mudita.maps.frontitude.R

@Composable
@Preview
fun GPSErrorBottomSheet(
    modifier: Modifier = Modifier,
    description: String = stringResource(R.string.maps_common_error_dialog_body_checkpsis),
    onCloseClick: () -> Unit = {},
) {
    VerticalConfirmationDialog(
        title = stringResource(id = R.string.maps_planningroute_error_dialog_h1_gpsunavalible),
        description = description,
        modifier = modifier,
    ) {
        VerticalConfirmationDialog.SecondaryButton(
            label = stringResource(id = R.string.common_dialog_button_close),
            onClick = onCloseClick,
        )
    }
}
