package com.mudita.map.common.geocode

import net.osmand.data.LatLon

data class GeocodingAddress(
    val city: String,
    val street: String,
    val buildingNumber: String,
    val postcode: String,
    val latLon: LatLon,
) {
    val streetWithBuildingNumber: String =
        buildString {
            append(street)
            if (buildingNumber.isNotBlank()) append(" $buildingNumber")
        }
}
