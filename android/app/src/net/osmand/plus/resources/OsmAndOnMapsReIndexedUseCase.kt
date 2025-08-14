package net.osmand.plus.resources

import com.mudita.map.common.maps.OnMapsReIndexedUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.osmand.plus.OsmandApplication

class OsmAndOnMapsReIndexedUseCase @Inject constructor(
    private val application: OsmandApplication,
) : OnMapsReIndexedUseCase {
    override fun invoke(): Flow<Unit> = callbackFlow {
        val listener = object : ResourceManager.ResourceListener {
            override fun onMapsIndexed() {
                trySend(Unit)
            }

            override fun onMapClosed(fileName: String?) = Unit
        }

        val resourceManager = application.resourceManager
        resourceManager?.addResourceListener(listener)
        awaitClose { resourceManager?.removeResourceListener(listener) }
    }
}
