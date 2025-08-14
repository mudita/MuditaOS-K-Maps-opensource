package com.mudita.map.common.model.navigation

import android.os.Parcelable
import com.mudita.map.common.utils.INTERMEDIATE_POINTS_MAX
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import net.osmand.data.LatLon
import java.util.UUID

@Parcelize
data class NavigationItem(
    val uuid: UUID = UUID.randomUUID(),
    val startPoint: NavigationPoint? = null,
    val intermediatePoints : List<NavigationPoint> = emptyList(),
    val endPoint: NavigationPoint? = null,
    val currentlySelectedPoint: NavigationPoint? = null,
): Parcelable {
    fun canNavigate() = endPoint != null && intermediatePoints.none { it.latLon == null }

    @IgnoredOnParcel
    val isIntermediatePointActive: Boolean = intermediatePoints.any { it.isActive }
}

fun NavigationItem.activated(): NavigationItem =
    copy(
        startPoint = startPoint?.copy(isActive = true, isActionActive = endPoint != null),
        endPoint = endPoint?.copy(isActive = true, isActionActive = intermediatePoints.size < INTERMEDIATE_POINTS_MAX),
        intermediatePoints = intermediatePoints.map { ip -> ip.copy(isActive = true, isActionActive = true) },
    )

fun NavigationItem.getLatLons(): List<LatLon> = buildList {
    startPoint?.latLon?.also(::add)
    intermediatePoints.mapNotNull { it.latLon }.also(::addAll)
    endPoint?.latLon?.also(::add)
}

fun NavigationItem.topLeftMostLatLon(): LatLon? {
    val latLons = getLatLons()
    return if (latLons.isNotEmpty()) {
        LatLon(
            getLatLons().maxOf { it.latitude },
            getLatLons().minOf { it.longitude },
        )
    } else {
        null
    }
}

fun NavigationItem.bottomRightMostLatLon(): LatLon? {
    val latLons = getLatLons()
    return if (latLons.isNotEmpty()) {
        LatLon(
            getLatLons().minOf { it.latitude },
            getLatLons().maxOf { it.longitude },
        )
    } else {
        null
    }
}

fun NavigationItem.updateIfUsesCurrentLocation(
    currentLocation: LatLon,
): NavigationItem =
    if (startPoint?.isCurrentLocation == true) {
        copy(
            startPoint = NavigationPoint(
                latLon = currentLocation,
                address = "",
                isCurrentLocation = true,
            )
        )
    } else {
        this
    }
