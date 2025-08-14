package com.mudita.search.repository.model

import net.osmand.search.core.SearchResult

data class SearchCityItem (
    val localName: String,
    val desc: String,
    val distance: Double,
    val lat: Double,
    val lon: Double,
    val sr: SearchResult
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchCityItem

        if (localName != other.localName) return false
        if (distance != other.distance) return false
        if (lat != other.lat) return false
        if (lon != other.lon) return false

        return true
    }

    override fun hashCode(): Int {
        var result = localName.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + lat.hashCode()
        result = 31 * result + lon.hashCode()
        return result
    }
}