package com.mudita.map.ui.pointReached

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.map.common.R
import com.mudita.map.common.ui.borderSize
import com.mudita.map.common.ui.borderSizeBold
import com.mudita.map.common.ui.routeDetailsBarBottomMargin
import com.mudita.map.common.ui.routeDetailsBarTopMargin
import com.mudita.map.common.ui.routeStateIconSize
import com.mudita.map.common.ui.screenMarginHalf
import com.mudita.map.ui.buttons.EndRouteButton
import com.mudita.maps.frontitude.R.string

@Composable
fun PointReachedTopSheet(
    icon: Painter,
    title: String,
    address: String,
    modifier: Modifier = Modifier,
    iconPadding: PaddingValues = PaddingValues(),
    onEndRouteClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .background(colorWhite)
            .padding(bottom = borderSize)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = routeDetailsBarTopMargin,
                    bottom = routeDetailsBarBottomMargin,
                    start = screenMarginHalf,
                    end = screenMarginHalf,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                modifier = Modifier
                    .size(routeStateIconSize)
                    .padding(iconPadding),
                painter = icon,
                contentDescription = null
            )

            Column(
                modifier = Modifier.padding(start = screenMarginHalf),
            ) {
                Row {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .weight(1f),
                        text = title,
                        style = KompaktTypography900.headlineLarge.copy(fontSize = 28.sp),
                        color = colorBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (onEndRouteClick != null) {
                        EndRouteButton(
                            onClick = onEndRouteClick,
                            modifier = Modifier.align(Alignment.Top),
                        )
                    }
                }

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = address,
                    style = KompaktTypography500.bodyMedium,
                    color = colorBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(thickness = borderSizeBold, color = colorBlack)
    }
}

@Composable
@Preview
fun DestinationReachedTopSheetPreview() {
    PointReachedTopSheet(
        icon = painterResource(R.drawable.icon_arrived),
        title = stringResource(string.maps_label_youhavearrived),
        address = "Jana Czeczota 6"
    )
}

@Composable
@Preview
fun IntermediatePointReachedTopSheetPreview() {
    PointReachedTopSheet(
        icon = painterResource(R.drawable.icon_stop_point),
        title = stringResource(string.maps_label_stoppoint),
        address = "Jana Czeczota 6",
        onEndRouteClick = {},
    )
}