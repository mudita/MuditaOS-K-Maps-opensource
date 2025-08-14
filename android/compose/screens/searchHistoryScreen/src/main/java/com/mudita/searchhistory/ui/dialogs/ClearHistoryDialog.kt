package com.mudita.searchhistory.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.searchhistory.R

@Composable
@Preview
fun ClearHistoryDialog(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    onClearClicked: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorWhite)
    ) {
        HorizontalDivider(thickness = 3.dp, color = colorBlack)
        Text(
            text = stringResource(id = R.string.clear_history_dialog_title),
            style = KompaktTypography900.titleMedium,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp)
        )
        Text(
            text = stringResource(id = R.string.clear_history_dialog_text),
            style = KompaktTypography500.bodySmall,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            KompaktSecondaryButton(
                text = stringResource(id = R.string.back_history_btn),
                attributes = KompaktTheme.buttonStyle.medium,
                modifier = Modifier.padding(start = 12.dp, end = 6.dp).weight(0.5f)
            ) {
                onBackClicked()
            }
            KompaktPrimaryButton(
                text = stringResource(id = R.string.clear_history),
                size = KompaktTheme.buttonStyle.medium,
                modifier = Modifier.padding(end = 12.dp, start = 6.dp).weight(0.5f)
            ) {
                onClearClicked()
            }
        }
    }
}