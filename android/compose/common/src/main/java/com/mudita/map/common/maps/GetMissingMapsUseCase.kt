package com.mudita.map.common.maps

import net.osmand.data.LatLon

interface GetMissingMapsUseCase {
    suspend operator fun invoke(latLon: LatLon): Result<List<String>> = invoke(listOf(latLon))
    suspend operator fun invoke(latLons: List<LatLon>): Result<List<String>>
}
