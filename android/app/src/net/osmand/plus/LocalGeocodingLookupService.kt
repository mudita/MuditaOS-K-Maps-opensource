package net.osmand.plus

import com.mudita.map.common.geocode.GeocodingAddress
import com.mudita.map.common.geocode.GeocodingAddressNotFoundException
import com.mudita.map.common.geocode.GeocodingLookupService
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import net.osmand.Location
import net.osmand.ResultMatcher
import net.osmand.binary.GeocodingUtilities.GeocodingResult
import net.osmand.data.LatLon
import net.osmand.plus.settings.backend.OsmandSettings

class LocalGeocodingLookupService @Inject constructor(
    private val locationProvider: OsmAndLocationProvider,
    private val settings: OsmandSettings,
) : GeocodingLookupService {
    private val lookUps = ConcurrentHashMap<LatLon, Deferred<GeocodingAddress>>()

    override suspend fun getFromLocation(latLon: LatLon): Result<GeocodingAddress> {
        val existingLookUp = lookUps[latLon]
        if (existingLookUp != null) return kotlin.runCatching { existingLookUp.await() }
        return kotlin.runCatching { geocode(latLon) }
    }

    override fun cancel(latLon: LatLon) {
        lookUps.remove(latLon)?.cancel()
    }

    @Suppress("DeferredResultUnused")
    private suspend fun geocode(latLon: LatLon): GeocodingAddress {
        currentCoroutineContext().job.invokeOnCompletion { cancel(latLon) }

        val location = Location("", latLon.latitude, latLon.longitude)
        val deferred = CompletableDeferred<GeocodingAddress>()
        lookUps[latLon] = deferred

        locationProvider
            .getGeocodingResult(location, object : ResultMatcher<GeocodingResult?> {
                override fun publish(geocodingResult: GeocodingResult?): Boolean {
                    if (geocodingResult != null && geocodingResult.location != null) {
                        val lang = settings.MAP_PREFERRED_LOCALE.get()

                        val address = GeocodingAddress(
                            city = geocodingResult.city?.getName(lang).orEmpty(),
                            street = geocodingResult.street?.getName(lang).orEmpty(),
                            buildingNumber = geocodingResult.building?.name.orEmpty(),
                            postcode = geocodingResult.city?.postcode.orEmpty(),
                            latLon = geocodingResult.location,
                        )
                        deferred.complete(address)
                    } else {
                        lookUps.remove(latLon)
                        deferred.completeExceptionally(GeocodingAddressNotFoundException(latLon))
                    }
                    return true
                }

                override fun isCancelled(): Boolean = lookUps[latLon]?.isCancelled ?: true
            })

        return deferred.await()
    }
}
