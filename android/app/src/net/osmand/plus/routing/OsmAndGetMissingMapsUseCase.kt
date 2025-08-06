package net.osmand.plus.routing

import com.mudita.map.common.maps.GetMissingMapsUseCase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication

class OsmAndGetMissingMapsUseCase @Inject constructor(
    private val application: OsmandApplication,
) : GetMissingMapsUseCase {

    override suspend fun invoke(latLons: List<LatLon>) = withContext(Dispatchers.Default) {
        runCatching {
            val locations: List<Location> = latLons.map { latLon -> Location("", latLon.latitude, latLon.longitude) }
            val straightLineRoutePoints = MissingMapsChecker.getDistributedPathPoints(locations)

            MissingMapsChecker.getMissingMaps(application, straightLineRoutePoints)
        }
    }
}
