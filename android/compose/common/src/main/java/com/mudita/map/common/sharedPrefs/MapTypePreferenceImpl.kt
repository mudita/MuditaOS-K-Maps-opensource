package com.mudita.map.common.sharedPrefs

import android.content.SharedPreferences
import com.mudita.map.common.enums.MapType
import com.mudita.map.common.enums.MetricsConstants
import javax.inject.Inject

class MapTypePreferenceImpl @Inject constructor(private val sharedPreferences: SharedPreferences) : MapTypesPreference {

    companion object {
        const val MAP_TYPE_KEY = "map_type"
    }

    override fun getMapType(): MapType {
        val default = MapType.WALKING.key
        return MapType.fromKey(sharedPreferences.getString(MAP_TYPE_KEY, default) ?: default)
    }

    override fun setMapType(mapType: MapType) {
        sharedPreferences.edit().putString(MAP_TYPE_KEY, mapType.key).apply()
    }
}