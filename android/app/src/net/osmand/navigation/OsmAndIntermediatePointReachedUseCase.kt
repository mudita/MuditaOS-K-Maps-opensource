package net.osmand.navigation

import com.mudita.map.common.navigation.IntermediatePointReachedUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.osmand.plus.routing.IntermediatePointReachedListener
import net.osmand.plus.routing.RoutingHelper

class OsmAndIntermediatePointReachedUseCase @Inject constructor(
    private val routingHelper: RoutingHelper,
): IntermediatePointReachedUseCase {
    override fun invoke(): Flow<Int> = callbackFlow {
        val listener = IntermediatePointReachedListener { index -> trySend(index) }
        routingHelper.addListener(listener)
        awaitClose { routingHelper.removeListener(listener) }
    }
}
