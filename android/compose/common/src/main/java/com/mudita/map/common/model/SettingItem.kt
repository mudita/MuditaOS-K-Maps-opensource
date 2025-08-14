package com.mudita.map.common.model

import androidx.annotation.StringRes
import com.mudita.maps.frontitude.R

sealed interface SettingItemAction {

    sealed interface Switchable {
        var isChecked: Boolean
    }

    sealed interface Checkable {
        val options: List<CheckableItem>

        sealed class CheckableItem(
            @StringRes val name: Int,
            open var isChecked: Boolean,
            open val isAvailable: Boolean = true
        ) {
            data class Phone(
                override var isChecked: Boolean = true
            ) : CheckableItem(name = R.string.common_label_phone, isChecked = isChecked)
            data class Card(
                override val isAvailable: Boolean,
                override var isChecked: Boolean = false,
            ) : CheckableItem(name = R.string.common_label_sdcard, isChecked)
            data class Kilometers(
                override var isChecked: Boolean = true
            ) : CheckableItem(name = R.string.common_label_kilometers, isChecked = isChecked)
            data class Miles(
                override var isChecked: Boolean = false
            ) : CheckableItem(name = R.string.common_label_miles, isChecked = isChecked)
        }
    }

    sealed interface Selectable
}


sealed class SettingItem(
    @StringRes open val title: Int,
    @StringRes open val desc: Int? = null
) : SettingItemAction {

    data class Sound(
        @StringRes override val title: Int,
        @StringRes override val desc: Int? = null,
        override var isChecked: Boolean = false
    ) : SettingItem(title, desc), SettingItemAction.Switchable

    data class ScreenAlwaysOn(
        @StringRes override val title: Int,
        @StringRes override val desc: Int? = null,
        override var isChecked: Boolean = false
    ) : SettingItem(title, desc), SettingItemAction.Switchable

    data class WifiOnly(
        @StringRes override val title: Int,
        @StringRes override val desc: Int? = null,
        override var isChecked: Boolean = false
    ) : SettingItem(title, desc), SettingItemAction.Switchable

    data class Storage(
        @StringRes override val title: Int,
        @StringRes override val desc: Int? = null,
        override var options: List<SettingItemAction.Checkable.CheckableItem>
    ) : SettingItem(title, desc), SettingItemAction.Checkable

    data class DistanceUnits(
        @StringRes override val title: Int,
        @StringRes override val desc: Int? = null,
        override var options: List<SettingItemAction.Checkable.CheckableItem>
    ) : SettingItem(title, desc), SettingItemAction.Checkable
}
