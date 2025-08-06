package net.osmand.plus.settings.backend

import com.mudita.map.common.sharedPrefs.SetMapLastKnownLocationUseCase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.osmand.data.LatLon

class OsmAndSetMapLastKnownLocationUseCase @Inject constructor(
    private val settings: OsmandSettings,
) : SetMapLastKnownLocationUseCase {
    override suspend fun invoke(latLon: LatLon, zoom: Int) {
        withContext(Dispatchers.IO) {
            settings.setLastKnownMapLocation(latLon.latitude, latLon.longitude, zoom)
        }
    }
}
