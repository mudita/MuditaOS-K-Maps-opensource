package com.mudita.map.repository

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mudita.map.common.R
import com.mudita.map.common.enums.MapType
import com.mudita.maps.frontitude.R.string

enum class NavigationModeItem(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val desc: String? = null,
) {
    Walking(
        title = string.maps_common_button_walking,
        icon = R.drawable.ic_walking,
    ),
    Cycling(
        title = string.maps_common_button_cycling,
        icon = R.drawable.ic_cycling,
    ),
    Driving(
        title = string.common_label_driving,
        icon = R.drawable.ic_driving,
    ),
    ;

    fun toMapType() = when (this) {
        Driving -> MapType.DRIVING
        Walking -> MapType.WALKING
        Cycling -> MapType.CYCLING
    }

    companion object {
        fun fromMapType(mapType: MapType) = when (mapType) {
            MapType.DRIVING -> Driving
            MapType.WALKING -> Walking
            MapType.CYCLING -> Cycling
        }
    }
}

sealed class NavigationDisplayMode {
    object Map : NavigationDisplayMode()
    object Commands : NavigationDisplayMode()
}
