package com.mudita.map.common.geocode

import net.osmand.data.LatLon

interface GeocodingLookupService {
    suspend fun getFromLocation(latLon: LatLon): Result<GeocodingAddress>

    fun cancel(latLon: LatLon)
}
