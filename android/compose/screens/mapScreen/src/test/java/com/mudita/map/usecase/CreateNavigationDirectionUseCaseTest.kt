package com.mudita.map.usecase

import com.mudita.map.common.model.navigation.NavigationPoint
import com.mudita.map.common.model.navigation.NavigationPointType
import io.mockk.junit5.MockKExtension
import net.osmand.data.LatLon
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CreateNavigationDirectionUseCaseTest {

    @Test
    fun `createNavigationDirection shouldn't enable intermediate points if there's no destination`() {
        // Given
        val createNavigationDirection = CreateNavigationDirectionUseCase()

        // When
        val result = createNavigationDirection(
            startPoint = NavigationPoint(
                latLon =  LatLon(0.0, 0.0),
                address = "",
                isActionActive = true,
                type = NavigationPointType.START
            ),
            intermediatePoints = null,
            destinationPoint = null,
            selectedPoint = null,
            showAllAsInactive = false,
        )

        // Then
        assertFalse(result.destination.isActionButtonActive)
    }

    @Test
    fun `when showAllAsInactive is true, createNavigationDirection should make all navigation points inactive`() {
        // Given
        val createNavigationDirection = CreateNavigationDirectionUseCase()

        // When
        val result = createNavigationDirection(
            startPoint = NavigationPoint(
                latLon =  LatLon(0.0, 0.0),
                address = "",
                isActive = true,
                isActionActive = true,
                type = NavigationPointType.START
            ),
            intermediatePoints = listOf(
                NavigationPoint(
                    latLon =  LatLon(0.0, 0.0),
                    address = "",
                    isActive = true,
                    isActionActive = true,
                    type = NavigationPointType.INTERMEDIATE
                )
            ),
            destinationPoint = null,
            selectedPoint = NavigationPoint(
                latLon =  LatLon(0.0, 0.0),
                address = "",
                isActive = true,
                isActionActive = true,
                type = NavigationPointType.START
            ),
            showAllAsInactive = true,
        )

        // Then
        assertFalse(result.start.isActive)
        assertFalse(result.intermediate.first().isActive)
        assertFalse(result.destination.isActive)
    }

    @Test
    fun `when startPoint is null, showAllAsInactive is false, selected point is START, createNavigationDirection should make only start point active`() {
        // Given
        val createNavigationDirection = CreateNavigationDirectionUseCase()

        // When
        val result = createNavigationDirection(
            startPoint = null,
            intermediatePoints = null,
            destinationPoint = NavigationPoint(
                latLon =  LatLon(0.0, 0.0),
                address = "",
                isActive = false,
                isActionActive = false,
                type = NavigationPointType.DESTINATION
            ),
            selectedPoint = NavigationPoint(
                latLon =  LatLon(0.0, 0.0),
                address = "",
                type = NavigationPointType.START
            ),
            showAllAsInactive = false,
        )

        // Then
        assertTrue(result.start.isActive)
        assertFalse(result.destination.isActive)
    }

    @Test
    fun `when destinationPoint is null, showAllAsInactive is false, selected point is DESTINATION createNavigationDirection should make only destination point active`() {
        // Given
        val createNavigationDirection = CreateNavigationDirectionUseCase()

        // When
        val result = createNavigationDirection(
            startPoint = NavigationPoint(
                latLon =  LatLon(0.0, 0.0),
                address = "",
                isActive = false,
                isActionActive = false,
                type = NavigationPointType.START
            ),
            intermediatePoints = null,
            destinationPoint = null,
            selectedPoint = NavigationPoint(
                latLon =  LatLon(0.0, 0.0),
                address = "",
                type = NavigationPointType.DESTINATION
            ),
            showAllAsInactive = false,
        )

        // Then
        assertTrue(result.destination.isActive)
        assertFalse(result.start.isActive)
    }

    @Test
    fun `when startPoint is null, destinationPoint is null, showAllAsInactive is false, selected point is null, then createNavigationDirection should make start and destination points active`() {
        // Given
        val createNavigationDirection = CreateNavigationDirectionUseCase()

        // When
        val result = createNavigationDirection(
            startPoint = null,
            intermediatePoints = null,
            destinationPoint = null,
            selectedPoint = null,
            showAllAsInactive = false,
        )

        // Then
        assertTrue(result.destination.isActive)
        assertTrue(result.start.isActive)
    }
}
