package net.osmand.plus.views.listeners

import net.osmand.data.LatLon

interface MapGestureListener {
    fun onMapGestureDetected(latLon: LatLon, zoom: Int, moved: Boolean)
}
