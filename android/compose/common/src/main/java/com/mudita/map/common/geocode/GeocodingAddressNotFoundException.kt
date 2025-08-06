package com.mudita.map.common.geocode

import net.osmand.data.LatLon

class GeocodingAddressNotFoundException(latLon: LatLon) : Exception("No GeocodingAddress found for $latLon.")
