package net.osmand.navigation

import com.mudita.map.common.navigation.StopVoiceRouterUseCase
import javax.inject.Inject
import net.osmand.plus.routing.RoutingHelper

class OsmAndStopVoiceRouterUseCase @Inject constructor(
    private val routingHelper: RoutingHelper,
) : StopVoiceRouterUseCase {

    override fun invoke() {
        routingHelper.voiceRouter.interruptRouteCommands()
    }
}
