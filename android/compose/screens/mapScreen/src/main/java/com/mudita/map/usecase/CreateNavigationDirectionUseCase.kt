package com.mudita.map.usecase

import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.model.navigation.NavigationPoint
import com.mudita.map.common.model.navigation.NavigationPointType
import com.mudita.map.repository.NavigationDirection
import com.mudita.map.repository.NavigationPointItem
import net.osmand.Location

class CreateNavigationDirectionUseCase {
    operator fun invoke(
        startPoint: NavigationPoint?,
        intermediatePoints: List<NavigationPoint>?,
        destinationPoint: NavigationPoint?,
        selectedPoint: NavigationPoint?,
        showAllAsInactive: Boolean,
    ): NavigationDirection = NavigationDirection(
        start = if (startPoint != null) {
            NavigationPointItem.Start(
                startAddress = startPoint.address,
                startLocation = startPoint.latLon?.let { latLon -> Location("", latLon.latitude, latLon.longitude) },
                isCurrentLocation = startPoint.isCurrentLocation,
                isActive = if (showAllAsInactive) false else startPoint.isActive,
                isActionButtonActive = startPoint.isActionActive,
            )
        } else {
            NavigationPointItem.Start(
                startAddress = "",
                startLocation = null,
                isCurrentLocation = false,
                isActive = if (showAllAsInactive) false else selectedPoint?.type == NavigationPointType.START || selectedPoint == null,
                isActionButtonActive = false,
            )
        },
        intermediate = intermediatePoints?.map {
            NavigationPointItem.Intermediate(
                uuid = it.uuid,
                intermediateAddress = it.address,
                intermediateLocation = it.latLon?.let { latLon -> Location("", latLon.latitude, latLon.longitude) },
                isActive = if (showAllAsInactive) false else it.isActive,
                isActionButtonActive = it.isActionActive,
            )
        } ?: emptyList(),
        destination = if (destinationPoint != null) {
            NavigationPointItem.Destination(
                finishAddress = destinationPoint.address,
                finishLocation = destinationPoint.latLon?.let { latLon -> Location("", latLon.latitude, latLon.longitude) },
                isActive = if (showAllAsInactive) false else destinationPoint.isActive,
                isActionButtonActive = destinationPoint.isActionActive,
            )
        } else {
            NavigationPointItem.Destination(
                finishAddress = "",
                finishLocation = null,
                isActive = if (showAllAsInactive) false else selectedPoint?.type == NavigationPointType.DESTINATION || selectedPoint == null,
                isActionButtonActive = false
            )
        }
    )

    operator fun invoke(navigationItem: NavigationItem, showAllAsInactive: Boolean): NavigationDirection =
        invoke(
            startPoint = navigationItem.startPoint,
            intermediatePoints = navigationItem.intermediatePoints,
            destinationPoint = navigationItem.endPoint,
            selectedPoint = navigationItem.currentlySelectedPoint,
            showAllAsInactive = showAllAsInactive,
        )
}