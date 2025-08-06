package com.mudita.map.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktButtonAttributes
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.map.common.ui.dividerThicknessMedium

@Composable
fun ConfirmBottomSheet(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    confirmText: String,
    cancelText: String,
    onConfirmClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    Column(
        modifier
            .background(colorWhite)
            .padding(top = dividerThicknessMedium)
            .background(colorBlack)
            .padding(top = dividerThicknessMedium)
            .background(colorWhite)
            .padding(bottom = 16.dp)
    ) {
        Text(
            modifier = Modifier.padding(top = 12.dp, start = 12.dp),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = KompaktTypography900.titleMedium,
        )
        Text(
            modifier = Modifier.padding(start = 12.dp, bottom = 16.dp),
            text = desc,
            style = KompaktTypography500.bodyMedium,
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KompaktSecondaryButton(
                modifier = Modifier.weight(1F),
                text = cancelText,
                attributes = KompaktButtonAttributes.Large,
                onClick = onCancelClick
            )
            KompaktPrimaryButton(
                modifier = Modifier.weight(1F),
                text = confirmText,
                size = KompaktButtonAttributes.Large,
                onClick = onConfirmClick
            )
        }
    }
}

@Composable
@Preview
fun ConfirmBottomSheetPreview() {
    ConfirmBottomSheet(
        title = "You sure?",
        desc = "Unsaved changes.",
        confirmText = "Confirm",
        cancelText = "Cancel",
        onConfirmClick = {}
    ) {}
}