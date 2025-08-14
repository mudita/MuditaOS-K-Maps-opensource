package net.osmand.plus.region

import com.mudita.map.common.region.GetRegionsIndexedEvents
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.osmand.map.OsmandRegions

class OsmAndGetRegionsIndexedEvents @Inject constructor(
    private val osmandRegions: OsmandRegions,
) : GetRegionsIndexedEvents {
    override fun invoke(): Flow<Unit> = callbackFlow {
        if (osmandRegions.isInitialRegionsIndexComplete) send(Unit)
        val listener = OsmandRegions.RegionsIndexedListener { trySend(Unit) }
        osmandRegions.registerRegionsIndexedListener(listener)
        awaitClose { osmandRegions.unregisterRegionsIndexedListener(listener) }
    }
}
