package com.mudita.map.common.sharedPrefs

import com.mudita.map.common.enums.MapType

interface MapTypesPreference {
    fun getMapType(): MapType
    fun setMapType(mapType: MapType)
}