package com.mudita.map.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.map.common.components.VerticalConfirmationDialog
import com.mudita.maps.frontitude.R
import com.mudita.map.common.R as commonR

@Composable
@Preview
fun LocationSharingDialog(
    modifier: Modifier = Modifier,
    onOpenSettingsClick: () -> Unit = {},
    onBrowseMapsClick: () -> Unit = {}
) {
    VerticalConfirmationDialog(
        title = stringResource(id = R.string.maps_permissions_dialog_h1_locationsharing),
        icon = commonR.drawable.ic_location_pin,
        description = stringResource(id = R.string.maps_permissions_dialog_body_eithersharelocationto),
        modifier = modifier,
    ) {
        VerticalConfirmationDialog.PrimaryButton(
            label = stringResource(id = R.string.common_dialog_button_opensettings),
            onClick = onOpenSettingsClick,
        )
        VerticalConfirmationDialog.SecondaryButton(
            label = stringResource(id = R.string.maps_downloaded_button_cta_browsemaps),
            onClick = onBrowseMapsClick,
        )
    }
}