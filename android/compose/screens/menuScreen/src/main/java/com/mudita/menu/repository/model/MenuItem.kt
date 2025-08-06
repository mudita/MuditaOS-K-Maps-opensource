package com.mudita.menu.repository.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mudita.maps.frontitude.R
import com.mudita.map.common.R as commonR

sealed class MenuItem(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
) {
    object SavedLocation : MenuItem(
        title = R.string.common_label_savedlocations,
        icon = commonR.drawable.icon_border_my_places
    )
    object ManageMaps : MenuItem(
        title = R.string.common_label_managemaps,
        icon = commonR.drawable.icon_border_download
    )
    object History : MenuItem(
        title = R.string.common_label_searchhistory,
        icon = commonR.drawable.icon_border_history
    )
    object Navigation : MenuItem(
        title = R.string.maps_menu_menuitem_planroute,
        icon = commonR.drawable.icon_border_navigation
    )
    object Settings : MenuItem(
        title = R.string.common_label_settings,
        icon = commonR.drawable.icon_border_settings
    )
    object About : MenuItem(
        title = R.string.common_label_about,
        icon = commonR.drawable.icon_border_about
    )
}
