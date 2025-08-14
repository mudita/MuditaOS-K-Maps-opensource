package com.mudita.map.ui.routePlanning

import com.mudita.map.common.R as commonR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktButtonAttributes
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.map.repository.NavigationModeItem
import com.mudita.maps.frontitude.R

@Composable
fun NavigationModeBottomSheet(
    selectedNavigationModeItem: NavigationModeItem,
    canNavigate: Boolean,
    modifier: Modifier = Modifier,
    onNavigationModeChange: (NavigationModeItem) -> Unit = {},
    onNavigateClick: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {}
            .background(colorWhite)
            .padding(top = dividerThicknessMedium)
    ) {
        HorizontalDivider(thickness = dividerThicknessMedium, color = colorBlack)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp)
        ) {
            NavigationModeItem.entries.forEach {
                NavModeItem(
                    item = it,
                    isChecked = selectedNavigationModeItem == it,
                    onClick = { onNavigationModeChange(it) },
                )
            }
        }
        KompaktSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            text = stringResource(id = R.string.maps_common_button_cta_startroute),
            enabled = canNavigate,
            iconResId = commonR.drawable.icon_navigate,
            attributes = KompaktButtonAttributes.Large,
            onClick = onNavigateClick,
        )
    }
}

@Composable
@Preview
fun NavigationModeBottomSheetPreview() {
    NavigationModeBottomSheet(
        selectedNavigationModeItem = NavigationModeItem.Driving,
        canNavigate = true,
    )
}