package com.mudita.map.ui.pointReached

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.maps.frontitude.R.string

@Composable
fun PointReachedBottomSheet(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(colorWhite),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        HorizontalDivider(
            color = colorWhite,
            thickness = dividerThicknessMedium,
        )
        HorizontalDivider(
            color = colorBlack,
            thickness = dividerThicknessMedium,
        )
        KompaktPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            text = text,
            size = KompaktTheme.buttonStyle.medium,
            onClick = onClick
        )
    }
}

@Composable
@Preview
fun DestinationReachedBottomSheetPreview() {
    PointReachedBottomSheet(stringResource(string.common_label_done))
}