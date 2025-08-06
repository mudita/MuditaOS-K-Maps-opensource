package com.mudita.map.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.map.common.R
import com.mudita.map.common.model.routing.RoadDetails
import com.mudita.map.common.ui.borderSize
import com.mudita.map.common.ui.borderSizeMediumBold
import com.mudita.map.common.ui.routeDetailsBarBottomMargin
import com.mudita.map.common.ui.routeDetailsBarTopMargin
import com.mudita.map.common.ui.routeStateIconSize
import com.mudita.map.common.ui.routeStateIconSizeSmall
import com.mudita.map.common.utils.route.getIcon
import com.mudita.map.ui.NavigationStep
import com.mudita.map.ui.NextSteps
import com.mudita.map.ui.buttons.EndRouteButton
import net.osmand.router.TurnType

@Composable
fun RouteDirectionsTopSheet(
    nextSteps: NextSteps?,
    onEndRouteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = false) { }
                .background(colorWhite)
                .fillMaxWidth()
                .padding(top = routeDetailsBarTopMargin, bottom = routeDetailsBarBottomMargin),
        ) {
            if (nextSteps != null) {
                Box(
                    modifier = Modifier.padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(routeStateIconSize),
                        painter = painterResource(id = nextSteps.first.iconRes),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )

                    if (nextSteps.first.exitOut != null) {
                        Text(
                            modifier = Modifier.width(22.dp),
                            text = nextSteps.first.exitOut.toString(),
                            textAlign = TextAlign.Center,
                            style = KompaktTypography900.displaySmall,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .weight(1f),
                ) {
                    Row {
                        Text(
                            text = nextSteps.first.distance,
                            style = KompaktTypography900.headlineLarge.copy(fontSize = 28.sp),
                            color = colorBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .weight(1f),
                        )

                        EndRouteButton(onClick = onEndRouteClick)
                    }

                    val roadName = nextSteps.first.roadDetails.name
                    if (!roadName.isNullOrBlank()) {
                        Text(
                            text = roadName,
                            style = KompaktTypography500.bodyMedium,
                            color = colorBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = borderSizeMediumBold, color = colorBlack)

        Row(modifier = Modifier.zIndex(-1f)) {
            if (nextSteps?.second != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(enabled = false) { }
                        .background(Color.White, RoundedCornerShape(bottomEnd = 10.dp))
                        .padding(bottom = borderSize, end = borderSize)
                        .topEndBorder(borderSize)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = nextSteps.second.iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(routeStateIconSizeSmall),
                    )

                    Text(
                        modifier = Modifier.height(23.dp),
                        text = nextSteps.second.distance,
                        style = KompaktTypography900.labelMedium,
                        color = colorBlack,
                        maxLines = 1,
                    )
                }
            }
            HorizontalDivider(thickness = borderSize, color = Color.White)
        }
    }
}

private fun Modifier.topEndBorder(
    borderSize: Dp,
    cornerRadius: Dp = 8.dp,
): Modifier =
    then(
        Modifier
            .drawWithCache {
                val cornerRadiusPx = cornerRadius.toPx()
                onDrawWithContent {
                    val borderSizeCorrection = cornerRadiusPx - borderSize.toPx() / 2
                    drawRoundRect(
                        color = Color.Black,
                        style = Stroke(borderSize.toPx()),
                        cornerRadius = CornerRadius(borderSizeCorrection, borderSizeCorrection),
                        topLeft = Offset(-cornerRadiusPx, -cornerRadiusPx),
                        size = Size(size.width + borderSizeCorrection, size.height + borderSizeCorrection),
                    )
                    drawContent()
                }
            }
    )

@Composable
@Preview
fun RouteDirectionsTopSheetPreview() {
    RouteDirectionsTopSheet(
        nextSteps = NextSteps(
            first = NavigationStep(
                distance = "100 m",
                roadDetails = RoadDetails(name = "Longway Road"),
                iconRes = TurnType.straight().getIcon(),
                exitOut = null,
                isIntermediatePoint = false,
            ),
            second = NavigationStep(
                distance = "15 m",
                roadDetails = RoadDetails(),
                iconRes = TurnType.valueOf(5, false).getIcon(),
                exitOut = null,
                isIntermediatePoint = false,
            ),
        ),
        onEndRouteClick = {},
        modifier = Modifier.background(Color.DarkGray),
    )
}

@Composable
@Preview
fun RouteDirectionsTopSheetRoundaboutPreview() {
    RouteDirectionsTopSheet(
        nextSteps = NextSteps(
            first = NavigationStep(
                distance = "220 m",
                roadDetails = RoadDetails(name = "Longway Road"),
                iconRes = R.drawable.icon_rondabout_forward,
                exitOut = 2,
                isIntermediatePoint = false,
            ),
            second = NavigationStep(
                distance = "500 m",
                roadDetails = RoadDetails(),
                iconRes = R.drawable.icon_rondabout_left,
                exitOut = 3,
                isIntermediatePoint = false,
            ),
        ),
        onEndRouteClick = {},
        modifier = Modifier.background(Color.DarkGray),
    )
}