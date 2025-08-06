package com.mudita.map.repository

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mudita.map.common.R
import com.mudita.map.common.model.navigation.NavigationPointType
import com.mudita.maps.frontitude.R.string
import java.util.UUID
import net.osmand.Location

data class NavigationDirection(
    val start: NavigationPointItem.Start = NavigationPointItem.Start(startAddress = "", startLocation = null, isCurrentLocation = false),
    val intermediate: List<NavigationPointItem.Intermediate> = emptyList(),
    val destination: NavigationPointItem.Destination = NavigationPointItem.Destination(finishAddress = "", finishLocation = null),
) {
    fun getPoints(): List<NavigationPointItem> = mutableListOf(
        start,
        *intermediate.toTypedArray(),
        destination
    )
}

sealed class NavigationPointItem(
    val address: String,
    val location: Location?,
    @DrawableRes val iconLeft: Int,
    @DrawableRes val iconRight: Int?,
) {
    abstract val type: NavigationPointType
    abstract val isActive: Boolean
    abstract val isActionButtonActive: Boolean

    data class Start(
        val startAddress: String,
        val startLocation: Location?,
        val isCurrentLocation: Boolean,
        override val isActive: Boolean = true,
        override val isActionButtonActive: Boolean = false,
        override val type: NavigationPointType = NavigationPointType.START
    ) : NavigationPointItem(
        address = startAddress,
        location = startLocation,
        iconLeft = R.drawable.ic_from_start,
        iconRight = null,
    )

    data class Intermediate(
        val uuid: UUID,
        val intermediateAddress: String,
        val intermediateLocation: Location?,
        override val isActive: Boolean = true,
        override val isActionButtonActive: Boolean = true,
        override val type: NavigationPointType = NavigationPointType.INTERMEDIATE
    ) : NavigationPointItem(
        address = intermediateAddress,
        location = intermediateLocation,
        iconLeft = R.drawable.ic_intermediate_flag,
        iconRight = R.drawable.ic_cancel
    )

    data class Destination(
        val finishAddress: String,
        val finishLocation: Location?,
        override val isActive: Boolean = true,
        override val isActionButtonActive: Boolean = true,
        override val type: NavigationPointType = NavigationPointType.DESTINATION
    ) : NavigationPointItem(
        address = finishAddress,
        location = finishLocation,
        iconLeft = R.drawable.ic_finish_flag,
        iconRight = R.drawable.ic_add
    )

}

@Composable
fun NavigationPointItem.displayableAddress(): String =
    if (location == null && address.isBlank()) {
        when (this) {
            is NavigationPointItem.Destination -> stringResource(id = string.maps_planningroute_placeholder_adddestination)
            is NavigationPointItem.Intermediate -> stringResource(id = string.maps_planningroute_placeholder_addstop)
            is NavigationPointItem.Start -> stringResource(id = string.common_label_currentlocation)
        }
    } else {
        when (this) {
            is NavigationPointItem.Start -> {
                if (isCurrentLocation) stringResource(id = string.common_label_currentlocation) else address
            }

            else -> address
        }
    }
