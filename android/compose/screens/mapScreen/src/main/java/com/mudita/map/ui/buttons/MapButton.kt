package com.mudita.map.ui.buttons

import com.mudita.maps.frontitude.R as frontitudeR
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.map.common.R
import com.mudita.map.common.ui.iconSize
import com.mudita.map.common.ui.mapButtonsCornerInnerRadius
import com.mudita.map.common.ui.mapButtonsCornerOuterRadius
import com.mudita.map.common.ui.noIndicationClickable

@Composable
fun MapButton(
    modifier: Modifier = Modifier,
    @DrawableRes drawableRes: Int,
    buttonClick: () -> Unit = {}
) {
    Icon(
        modifier = modifier
            .clip(RoundedCornerShape(mapButtonsCornerOuterRadius))
            .noIndicationClickable(onClick = buttonClick)
            .background(colorWhite)
            .padding(2.dp)
            .border(2.dp, colorBlack, RoundedCornerShape(mapButtonsCornerInnerRadius))
            .padding(18.dp)
            .size(28.dp),
        painter = painterResource(id = drawableRes),
        contentDescription = null
    )
}

@Composable
fun MapButtonWithText(
    modifier: Modifier = Modifier,
    @StringRes textRes: Int,
    @DrawableRes drawableRes: Int,
    buttonClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .background(colorWhite, RoundedCornerShape(14.dp))
            .padding(2.dp)
            .border(2.dp, colorBlack, RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .clickable(onClick = buttonClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
            painter = painterResource(id = drawableRes),
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(id = textRes),
            style = KompaktTypography900.labelLarge
        )
    }
}

enum class ZoomType {
    ZOOM_IN,
    ZOOM_OUT
}

@Composable
fun MapZooms(modifier: Modifier = Modifier, zoomClick: (ZoomType) -> Unit = {}) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .background(colorWhite, RoundedCornerShape(mapButtonsCornerOuterRadius))
            .padding(2.dp)
            .border(2.dp, colorBlack, RoundedCornerShape(mapButtonsCornerInnerRadius))
    ) {
        Icon(
            modifier = Modifier
                .noIndicationClickable { zoomClick(ZoomType.ZOOM_IN) }
                .padding(18.dp)
                .size(28.dp),
            painter = painterResource(id = R.drawable.icon_plus),
            contentDescription = null
        )

        HorizontalDivider(thickness = 2.dp, color = colorBlack)

        Icon(
            modifier = Modifier
                .noIndicationClickable { zoomClick(ZoomType.ZOOM_OUT) }
                .padding(18.dp)
                .size(28.dp),
            painter = painterResource(id = R.drawable.icon_minus),
            contentDescription = null
        )
    }
}

@Composable
@Preview
fun MapButtonPreview() {
    MapButton(drawableRes = R.drawable.icon_my_location)
}

@Composable
@Preview
fun MapButtonWithTextPreview() {
    MapButtonWithText(
        textRes = frontitudeR.string.maps_navigation_label_center,
        drawableRes = R.drawable.ic_gps_location_compass
    )
}

@Composable
@Preview
fun MapZoomsPreview() {
    MapZooms()
}