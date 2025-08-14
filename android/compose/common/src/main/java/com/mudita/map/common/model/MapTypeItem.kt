package com.mudita.map.common.model

import androidx.annotation.StringRes
import com.mudita.map.common.R
import com.mudita.map.common.enums.MapType

sealed class MapTypeItem(
    @StringRes open val title: Int,
    @StringRes open val desc: Int,
    open val mapType: MapType
) {
    data class Driving(
        @StringRes override val title: Int = R.string.driving,
        @StringRes override val desc: Int = R.string.driving_desc,
        override val mapType: MapType = MapType.DRIVING
    ) : MapTypeItem(title, desc, mapType)

    data class Walking(
        @StringRes override val title: Int = R.string.walking,
        @StringRes override val desc: Int = R.string.walking_desc,
        override val mapType: MapType = MapType.WALKING
    ) : MapTypeItem(title, desc, mapType)

    data class Cycling(
        @StringRes override val title: Int = R.string.cycling,
        @StringRes override val desc: Int = R.string.cycling_desc,
        override val mapType: MapType = MapType.CYCLING
    ) : MapTypeItem(title, desc, mapType)
}