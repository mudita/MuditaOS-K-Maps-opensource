package com.mudita.map.common.sharedPrefs

import net.osmand.data.LatLon

interface SetMapLastKnownLocationUseCase {
    suspend operator fun invoke(latLon: LatLon, zoom: Int)
}
