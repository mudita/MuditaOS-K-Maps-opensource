package com.mudita.map.common.model.navigation

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize
import net.osmand.data.LatLon

@Parcelize
data class NavigationPoint(
    val latLon: LatLon?,
    val address: String,
    val isActive: Boolean = true,
    val isActionActive: Boolean = false,
    val type: NavigationPointType = NavigationPointType.START,
    val uuid: UUID = UUID.randomUUID(),
    val city: String? = null,
    val isCurrentLocation: Boolean = false,
): Parcelable
