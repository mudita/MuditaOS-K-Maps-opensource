package com.mudita.map.common.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.map.common.R
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.map.common.ui.iconSize

object VerticalConfirmationDialog {

    @Composable
    fun PrimaryButton(
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        iconResId: Int? = null,
    ) {
        KompaktPrimaryButton(
            text = label,
            size = KompaktTheme.buttonStyle.large,
            modifier = modifier.fillMaxWidth(),
            iconResId = iconResId,
            onClick = onClick,
        )
    }

    @Composable
    fun SecondaryButton(
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        iconResId: Int? = null,
    ) {
        KompaktSecondaryButton(
            text = label,
            attributes = KompaktTheme.buttonStyle.large,
            modifier = modifier.fillMaxWidth(),
            iconResId = iconResId,
            onClick = onClick,
        )
    }
}

@Composable
fun VerticalConfirmationDialog(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    description: String? = null,
    onCloseClick: (() -> Unit)? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(enabled = false) {  }
            .fillMaxWidth()
            .background(colorWhite)
            .padding(top = dividerThicknessMedium)
            .background(colorBlack)
            .padding(top = dividerThicknessMedium)
            .background(colorWhite)
            .padding(start = 12.dp, end = 12.dp, top = 21.dp, bottom = 12.dp),
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(28.dp),
                painter = painterResource(id = icon),
                contentDescription = null,
            )
        }
        Row {
            Text(
                text = title,
                textAlign = if (icon != null) TextAlign.Center else TextAlign.Start,
                style = KompaktTypography900.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (onCloseClick != null) {
                KompaktIconButton(
                    iconResId = R.drawable.ic_cancel,
                    iconSize = iconSize,
                    onClick =  onCloseClick,
                    touchAreaPadding = PaddingValues(start = 8.dp),
                )

            }
        }
        if (description != null) {
            Text(
                text = description,
                textAlign = if (icon != null) TextAlign.Center else TextAlign.Start,
                style = KompaktTypography500.bodyMedium,
                modifier = Modifier
                    .padding(top = 9.dp)
                    .fillMaxWidth()
            )
        }
        if (actions != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                content = actions,
            )
        } else {
            Spacer(modifier = Modifier.height(11.dp))
        }
    }
}

@Composable
@Preview
fun VerticalConfirmationDialogPreview() {
    VerticalConfirmationDialog(
        title = "Dialog title",
        description = "Lorem ipsum dolor sit amet."
    ) {
        VerticalConfirmationDialog.PrimaryButton(
            label = "Perform action",
            onClick = {},
        )
        VerticalConfirmationDialog.SecondaryButton(
            label = "Close",
            onClick = {},
        )
    }
}

@Composable
@Preview
fun VerticalConfirmationDialogWithIconPreview() {
    VerticalConfirmationDialog(
        title = "Dialog title",
        icon = R.drawable.ic_location_pin,
        description = "Lorem ipsum dolor sit amet."
    ) {
        VerticalConfirmationDialog.PrimaryButton(
            label = "Perform action",
            onClick = {},
        )
        VerticalConfirmationDialog.SecondaryButton(
            label = "Close",
            onClick = {},
        )
    }
}

@Composable
@Preview
fun VerticalConfirmationDialogWithCloseButtonPreview() {
    VerticalConfirmationDialog(
        title = "Very very very long dialog title",
        description = "Lorem ipsum dolor sit amet.",
        onCloseClick = {},
    ) {
        VerticalConfirmationDialog.PrimaryButton(
            label = "Perform action",
            onClick = {},
        )
        VerticalConfirmationDialog.SecondaryButton(
            label = "Close",
            onClick = {},
        )
    }
}