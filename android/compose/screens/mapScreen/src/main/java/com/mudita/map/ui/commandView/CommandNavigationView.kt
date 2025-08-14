package com.mudita.map.ui.commandView

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.map.common.R
import com.mudita.map.common.model.routing.RoadDetails
import com.mudita.map.common.ui.borderSize
import com.mudita.map.common.ui.borderSizeBold
import com.mudita.map.common.ui.roadNumberCornerRadius
import com.mudita.map.common.ui.roadNumberHorizontalPadding
import com.mudita.map.common.ui.roadNumberVerticalPadding
import com.mudita.map.common.ui.routeStateIconSize
import com.mudita.map.common.ui.routeStateIconSizeLarge
import com.mudita.map.common.utils.route.getIcon
import com.mudita.map.ui.NavigationStep
import com.mudita.map.ui.buttons.EndRouteButton
import net.osmand.router.TurnType

@Composable
fun CommandNavigationView(
    navigationSteps: List<NavigationStep>,
    onEndRouteClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (navigationSteps.isEmpty()) return

        FirstStepView(
            modifier = Modifier.fillMaxWidth(),
            firstStep = navigationSteps.first(),
            onEndRouteClick = onEndRouteClick
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            navigationSteps.forEachIndexed { index, step ->
                if (index == 0) return@forEachIndexed
                val isLast = index == navigationSteps.lastIndex

                CommandItemView(
                    roadDetails = step.roadDetails,
                    distance = step.distance,
                    icon = if (isLast) R.drawable.icon_arrived else step.iconRes,
                    exitOut = step.exitOut,
                    isLast = isLast,
                )
            }
        }
    }
}

@Composable
fun FirstStepView(
    modifier: Modifier = Modifier,
    firstStep: NavigationStep,
    onEndRouteClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
    ) {
        EndRouteButton(
            onClick = onEndRouteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 41.dp, end = 16.dp, bottom = 24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(routeStateIconSizeLarge),
                    tint = Color.Unspecified,
                    painter = painterResource(id = firstStep.iconRes),
                    contentDescription = null,
                )

                val exitOut: Int? = firstStep.exitOut

                if (exitOut != null) {
                    Text(
                        modifier = Modifier
                            .width(44.dp),
                        text = exitOut.toString(),
                        textAlign = TextAlign.Center,
                        style = KompaktTypography900.labelLarge,
                        fontSize = 23.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            Text(
                text = firstStep.distance,
                style = KompaktTypography900.headlineLarge.copy(fontSize = 48.sp, lineHeight = 48.sp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .height(52.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (!firstStep.roadDetails.roadNumber.isNullOrBlank()) {
                    RoadNumber(firstStep.roadDetails.roadNumber.orEmpty())
                }
                Text(
                    text = firstStep.roadDetails.name.orEmpty(),
                    style = KompaktTypography500.titleMedium.copy(fontSize = 32.sp, lineHeight = 32.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color.Black,
            thickness = borderSizeBold,
        )
    }
}

@Composable
fun CommandItemView(
    roadDetails: RoadDetails,
    distance: String,
    icon: Int,
    exitOut: Int?,
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 80.dp)
                .padding(start = 8.dp, top = 10.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(routeStateIconSize),
                    painter = painterResource(id = icon),
                    tint = Color.Unspecified,
                    contentDescription = null,
                )

                if (exitOut != null) {
                    Text(
                        modifier = Modifier.width(22.dp),
                        text = exitOut.toString(),
                        textAlign = TextAlign.Center,
                        style = KompaktTypography900.displaySmall,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = distance,
                    style = KompaktTypography900.titleMedium
                )
                if (roadDetails.areAnyDetailsAvailable()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!roadDetails.roadNumber.isNullOrBlank()) {
                            RoadNumber(roadDetails.roadNumber.orEmpty())
                        }
                        if (!roadDetails.name.isNullOrBlank()) {
                            Text(
                                text = roadDetails.name.orEmpty(),
                                style = KompaktTypography500.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        if (isLast.not()) DashedHorizontalDivider(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoadNumber(roadNumber: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(end = 12.dp)
            .border(BorderStroke(borderSize, colorBlack), RoundedCornerShape(roadNumberCornerRadius))
            .padding(horizontal = roadNumberHorizontalPadding, vertical = roadNumberVerticalPadding),
        text = roadNumber,
        style = KompaktTypography900.titleSmall.copy(fontSize = 16.sp),
    )
}

// region Previews

@Preview(backgroundColor = 0xffffffff, showBackground = true)
@Composable
private fun FirstStepViewPreview() {
    KompaktTheme {
        FirstStepView(
            firstStep = NavigationStep(
                distance = "42 km",
                roadDetails = RoadDetails(roadNumber = "S7", name = "Strumykowa"),
                iconRes = TurnType.straight().getIcon(),
                exitOut = null,
                isIntermediatePoint = false,
            )
        )
    }
}

@Preview(backgroundColor = 0xffffffff, showBackground = true)
@Composable
private fun CommandItemViewPreview() {
    KompaktTheme {
        CommandNavigationView(
            navigationSteps = listOf(
                NavigationStep(
                    distance = "2 km",
                    roadDetails = RoadDetails(roadNumber = null, name = "Strumykowa"),
                    iconRes = TurnType.straight().getIcon(),
                    exitOut = null,
                    isIntermediatePoint = false,
                ),
                NavigationStep(
                    distance = "500 m",
                    roadDetails = RoadDetails(roadNumber = "S11", name = "Katowice"),
                    iconRes = TurnType.valueOf(2, false).getIcon(),
                    exitOut = null,
                    isIntermediatePoint = false,
                ),
                NavigationStep(
                    distance = "800 m",
                    roadDetails = RoadDetails(roadNumber = "S11", name = null),
                    iconRes = TurnType.valueOf(8, false).getIcon(),
                    exitOut = null,
                    isIntermediatePoint = false,
                ),
                NavigationStep(
                    distance = "220 m",
                    roadDetails = RoadDetails(roadNumber = null, name = null),
                    iconRes = TurnType.valueOf(4, false).getIcon(),
                    exitOut = null,
                    isIntermediatePoint = false,
                ),
                NavigationStep(
                    distance = "2 km",
                    roadDetails = RoadDetails(roadNumber = null, name = "Kolorowa"),
                    iconRes = TurnType.valueOf(1, false).getIcon(),
                    exitOut = null,
                    isIntermediatePoint = false,
                ),
            ),
            onEndRouteClick = {},
        )
    }
}

@Preview(backgroundColor = 0xffffffff, showBackground = true)
@Composable
private fun CommandItemViewRoundaboutPreview() {
    KompaktTheme {
        CommandNavigationView(
            navigationSteps = listOf(
                NavigationStep(
                    distance = "2 km",
                    roadDetails = RoadDetails(roadNumber = null, name = "Strumykowa"),
                    iconRes = R.drawable.icon_rondabout_forward,
                    exitOut = 2,
                    isIntermediatePoint = false,
                ),
                NavigationStep(
                    distance = "500 m",
                    roadDetails = RoadDetails(roadNumber = "S11", name = null),
                    iconRes = TurnType.valueOf(2, false).getIcon(),
                    exitOut = null,
                    isIntermediatePoint = false,
                ),
                NavigationStep(
                    distance = "800 m",
                    roadDetails = RoadDetails(roadNumber = "S11", name = "Katowice"),
                    iconRes = R.drawable.icon_rondabout_right,
                    exitOut = 1,
                    isIntermediatePoint = false,
                ),
                NavigationStep(
                    distance = "220 m",
                    roadDetails = RoadDetails(roadNumber = null, name = null),
                    iconRes = TurnType.valueOf(4, false).getIcon(),
                    exitOut = null,
                    isIntermediatePoint = false,
                ),
                NavigationStep(
                    distance = "2 km",
                    roadDetails = RoadDetails(roadNumber = null, name = "Kolorowa"),
                    iconRes = TurnType.valueOf(1, false).getIcon(),
                    exitOut = null,
                    isIntermediatePoint = false,
                ),
            ),
            onEndRouteClick = {},
        )
    }
}

// endregion
