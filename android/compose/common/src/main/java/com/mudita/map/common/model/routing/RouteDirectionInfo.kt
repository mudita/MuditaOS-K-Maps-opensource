package com.mudita.map.common.model.routing

import net.osmand.binary.RouteDataObject
import net.osmand.router.ExitInfo
import net.osmand.router.TurnType
import kotlin.math.roundToInt

class RouteDirectionInfo(averageSpeed: Float, turnType: TurnType) {
    // location when you should action (turn or go ahead)
	@JvmField
	var routePointOffset = 0

    // location where direction end. useful for roundabouts.
	@JvmField
	var routeEndPointOffset = 0

    // Type of action to take
    val turnType: TurnType

    // Description of the turn and route after
    var descriptionRoutePart = "" //$NON-NLS-1$
        private set

    // Speed after the action till next turn
    private var averageSpeed: Float
    var ref: String? = null
    var streetName: String? = null
    var destinationName: String? = null
    var routeDataObject: RouteDataObject? = null
    var exitInfo: ExitInfo? = null
    var isIntermediatePoint = false

    val roadDetails: RoadDetails
        get() = collectRoadDetails()

    // calculated vars
    // after action (excluding expectedTime)
    @JvmField
    var afterLeftTime = 0

    // distance after action (for i.e. after turn to next turn)
    var distance = 0

    // Constructor to verify average speed always > 0
    init {
        this.averageSpeed = if (averageSpeed == 0f) 1F else averageSpeed
        this.turnType = turnType
    }

    fun setDescriptionRoute(descriptionRoute: String) {
        descriptionRoutePart = descriptionRoute
    }

    fun getAverageSpeed(): Float {
        return averageSpeed
    }

    fun setAverageSpeed(averageSpeed: Float) {
        this.averageSpeed = if (averageSpeed == 0f) 1F else averageSpeed
    }

    private fun collectRoadDetails(): RoadDetails {

        fun getDetailsForStreet(ref: String?, streetName: String?): RoadDetails? {
            var roadNumber: String? = null

            if (!ref.isNullOrBlank()) {
                roadNumber = ref.split(DIRECTION_INFO_PART_ORIGINAL_DELIMITER).firstOrNull { it != EMPTY_ROAD_NUMBER }
            }

            if (streetName != null || roadNumber != null) {
                return RoadDetails(
                    roadNumber = roadNumber,
                    name = streetName
                )
            }

            return null
        }

        fun getDetailsForDestination(destinationName: String?): RoadDetails? {
            var roadNumber: String? = null
            var destination: String? = null

            if (!destinationName.isNullOrBlank()) {
                val destinationInfoParts: List<String> = destinationName.split(DIRECTION_INFO_PARTS_DIVIDER)
                when (destinationInfoParts.size) {
                    // eg. destinationName = "Poznań;Katowice" -> destination = "Poznań, Katowice"
                    1 -> destination = destinationInfoParts[0].replace(DIRECTION_INFO_PART_ORIGINAL_DELIMITER, DIRECTION_INFO_PART_DELIMITER)
                    // eg. destinationName = "A2;S11, Poznań;Katowice" -> destination = "Poznań, Katowice", destinationRoadNumber = "A2"
                    2 -> {
                        roadNumber = destinationInfoParts[0].split(DIRECTION_INFO_PART_ORIGINAL_DELIMITER).firstOrNull { it != EMPTY_ROAD_NUMBER }
                        destination = destinationInfoParts[1].replace(DIRECTION_INFO_PART_ORIGINAL_DELIMITER, DIRECTION_INFO_PART_DELIMITER)
                    }
                }
            }

            if (destination != null) {
                return RoadDetails(
                    roadNumber = roadNumber,
                    name = destination
                )
            }

            return null
        }

        val streetDetails: RoadDetails? = getDetailsForStreet(this.ref, this.streetName)
        if (streetDetails?.areDetailsComplete() == true) return streetDetails

        val destinationDetails: RoadDetails? = getDetailsForDestination(this.destinationName)
        if (destinationDetails?.areDetailsComplete() == true) return destinationDetails

        return streetDetails ?: (destinationDetails ?: RoadDetails())
    }

    // expected time after route point
    val expectedTime: Int
        get() = (distance / averageSpeed).roundToInt()

    companion object {
        private const val DIRECTION_INFO_PARTS_DIVIDER = ", "
        private const val DIRECTION_INFO_PART_ORIGINAL_DELIMITER = ";"
        private const val DIRECTION_INFO_PART_DELIMITER = ", "
        private const val EMPTY_ROAD_NUMBER = "none"
    }
}