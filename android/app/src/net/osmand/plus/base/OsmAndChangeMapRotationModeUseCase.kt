package net.osmand.plus.base

import com.mudita.map.common.di.DispatcherQualifier
import com.mudita.map.common.utils.ChangeMapRotationModeUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class OsmAndChangeMapRotationModeUseCase @Inject constructor(
    private val mapViewTrackingUtilities: MapViewTrackingUtilities,
    @DispatcherQualifier.IO private val dispatcher: CoroutineDispatcher,
) : ChangeMapRotationModeUseCase {
    override suspend fun invoke(mapRotationEnabled: Boolean, isWalkingNavigation: Boolean) {
        withContext(dispatcher) {
            mapViewTrackingUtilities.setMapRotationEnabled(mapRotationEnabled, isWalkingNavigation)
        }
    }
}
