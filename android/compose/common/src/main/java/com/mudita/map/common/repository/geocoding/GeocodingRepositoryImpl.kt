package com.mudita.map.common.repository.geocoding

import androidx.collection.LruCache
import com.mudita.map.common.geocode.GeocodingAddress
import com.mudita.map.common.geocode.GeocodingLookupService
import javax.inject.Inject
import javax.inject.Singleton
import net.osmand.data.LatLon

@Singleton
class GeocodingRepositoryImpl @Inject constructor(
    private val geocodingLookupService: GeocodingLookupService
) : GeocodingRepository {

    private val addressCache = LruCache<LatLon, GeocodingAddress>(ADDRESS_CACHE_SIZE)

    override suspend fun searchAddress(latLon: LatLon): Result<GeocodingAddress> =
        addressCache[latLon]?.let { Result.success(it) }
            ?: geocodingLookupService.getFromLocation(latLon)
                .onSuccess { addressCache.put(latLon, it) }

    override fun cancelSearch(latLon: LatLon) {
        geocodingLookupService.cancel(latLon)
    }

    private companion object {
        private const val ADDRESS_CACHE_SIZE = 50
    }
}