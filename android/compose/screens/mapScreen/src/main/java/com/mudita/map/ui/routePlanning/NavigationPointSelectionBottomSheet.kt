package com.mudita.map.ui.routePlanning

import com.mudita.map.common.R as commonR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktButtonAttributes
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.map.common.BuildConfig
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.maps.frontitude.R.string

@Composable
fun NavigationPointSelectionBottomSheet(
    isIntermediatePoint: Boolean,
    onSearchSelect: () -> Unit,
    onSavedLocationsSelect: () -> Unit,
    onSelectOnMapSelect: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {}
            .background(colorWhite)
            .fillMaxWidth()
            .padding(top = dividerThicknessMedium, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = colorBlack,
            thickness = dividerThicknessMedium,
        )
        Text(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            text = if (isIntermediatePoint) {
                stringResource(string.map_common_dialog_h1_selectstopusing)
            } else {
                stringResource(string.map_common_dialog_h1_selectlocationusing)
            },
            textAlign = TextAlign.Center,
            style = KompaktTypography900.titleMedium
        )
        KompaktSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResource(string.common_label_search),
            iconResId = commonR.drawable.icon_search,
            attributes = KompaktButtonAttributes.Medium,
            onClick = onSearchSelect,
        )
        if (!BuildConfig.IS_PREMIERE_VERSION) {
            KompaktSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = stringResource(string.common_label_savedlocations),
                iconResId = commonR.drawable.icon_star_outlined,
                attributes = KompaktButtonAttributes.Medium,
                onClick = onSavedLocationsSelect
            )
        }
        KompaktSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResource(string.maps_common_dialog_button_selectonmap),
            iconResId = commonR.drawable.icon_maps,
            attributes = KompaktButtonAttributes.Medium,
            onClick = onSelectOnMapSelect
        )
        KompaktPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResource(string.common_dialog_button_cancel),
            size = KompaktButtonAttributes.Medium,
            onClick = onCancelClick
        )
    }
}

@Composable
@Preview
private fun NavigationPointSelectionBottomSheetPreview() {
    NavigationPointSelectionBottomSheet(
        isIntermediatePoint = false,
        onSearchSelect = {},
        onSelectOnMapSelect = {},
        onSavedLocationsSelect = {},
        onCancelClick = {},
    )
}

@Composable
@Preview
private fun NavigationPointSelectionBottomSheetIntermediatePointPreview() {
    NavigationPointSelectionBottomSheet(
        isIntermediatePoint = true,
        onSearchSelect = {},
        onSelectOnMapSelect = {},
        onSavedLocationsSelect = {},
        onCancelClick = {},
    )
}
