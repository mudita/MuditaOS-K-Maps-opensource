package com.mudita.map.ui

import com.mudita.map.common.R
import com.mudita.map.common.model.routing.RoadDetails
import com.mudita.map.common.model.routing.RouteDirectionInfo
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.map.common.utils.route.getIcon
import net.osmand.router.TurnType

data class NavigationStep(
    val distance: String,
    val roadDetails: RoadDetails,
    val iconRes: Int,
    val exitOut: Int?,
    val isIntermediatePoint: Boolean,
) {
    companion object {
        fun createSteps(
            estimatedNextTurnDistance: Float,
            routeDirections: List<RouteDirectionInfo>,
            osmAndFormatter: OsmAndFormatter,
        ): List<NavigationStep> =
            routeDirections.mapIndexed { index, routeDirectionInfo ->
                val distance = if (index == 0) {
                    osmAndFormatter.getFormattedDistanceValue(estimatedNextTurnDistance)
                } else {
                    osmAndFormatter.getFormattedDistanceValue(routeDirections[index - 1].distance.toFloat())
                }.formattedValue

                val turnType: TurnType = routeDirectionInfo.turnType

                val iconRes = if (routeDirectionInfo.isIntermediatePoint) {
                    R.drawable.icon_stop_point
                } else {
                    turnType.getIcon()
                }

                val exitOut: Int? = turnType.exitOut.takeIf { turnType.isRoundAbout }

                NavigationStep(
                    distance = distance,
                    roadDetails = routeDirectionInfo.roadDetails,
                    iconRes = iconRes,
                    exitOut = exitOut,
                    isIntermediatePoint = routeDirectionInfo.isIntermediatePoint,
                )
            }
    }
}
