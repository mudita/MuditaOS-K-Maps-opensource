package com.mudita.map.common.repository.geocoding

import com.mudita.map.common.geocode.GeocodingAddress
import net.osmand.data.LatLon

interface GeocodingRepository {

    suspend fun searchAddress(latLon: LatLon): Result<GeocodingAddress>

    fun cancelSearch(latLon: LatLon)
}
