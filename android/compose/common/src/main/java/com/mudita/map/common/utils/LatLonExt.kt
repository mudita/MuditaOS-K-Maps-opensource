package com.mudita.map.common.utils

import net.osmand.data.LatLon
import kotlin.math.abs

private const val DELIMITER_DEGREES = "°"
private const val NORTH = "N"
private const val SOUTH = "S"
private const val WEST = "W"
private const val EAST = "E"

fun LatLon.formattedCoordinates(): String {
    val latFormatted = formattedLatitude()
    val lonFormatted = formattedLongitude()

    return "$latFormatted, $lonFormatted"
}

private fun LatLon.formattedLatitude(): String {
    val lat = latitude.round(5)

    val latSuffix = when {
        lat > 0 -> "$DELIMITER_DEGREES $NORTH"
        lat < 0 -> "$DELIMITER_DEGREES $SOUTH"
        else -> DELIMITER_DEGREES
    }

    return if (lat == 0.0) return "0°" else "${abs(lat)}$latSuffix"
}

private fun LatLon.formattedLongitude(): String {
    val lon = longitude.round(5)

    val lonSuffix = when {
        lon > 0 -> "$DELIMITER_DEGREES $EAST"
        lon < 0 -> "$DELIMITER_DEGREES $WEST"
        else -> DELIMITER_DEGREES
    }

    return if (lon == 0.0) return "0°" else "${abs(lon)}$lonSuffix"
}
