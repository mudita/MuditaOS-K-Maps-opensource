package net.osmand.navigation

import com.mudita.map.common.navigation.StopVoiceRouterUseCase
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import net.osmand.plus.routing.RoutingHelper
import net.osmand.plus.routing.VoiceRouter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OsmAndStopVoiceRouterUseCaseTest {

    @MockK
    private lateinit var routingHelper: RoutingHelper

    private lateinit var stopVoiceRouterUseCase: StopVoiceRouterUseCase

    @BeforeEach
    fun setup() {
        stopVoiceRouterUseCase = OsmAndStopVoiceRouterUseCase(
            routingHelper = routingHelper,
        )
    }

    @Test
    fun `When stopVoiceRouterUseCase is invoked, then should call interruptRouteCommands from voice router`() {
        // Given
        val voiceRouter: VoiceRouter = mockk() {
            justRun { interruptRouteCommands() }
        }
        every { routingHelper.voiceRouter } returns voiceRouter

        // When
        stopVoiceRouterUseCase()

        // Then
        verify { voiceRouter.interruptRouteCommands() }
    }
}