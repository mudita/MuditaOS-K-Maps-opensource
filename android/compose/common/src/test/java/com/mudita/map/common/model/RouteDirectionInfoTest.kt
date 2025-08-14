package com.mudita.map.common.model

import com.mudita.map.common.model.routing.RoadDetails
import com.mudita.map.common.model.routing.RouteDirectionInfo
import net.osmand.router.TurnType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RouteDirectionInfoTest {

    @Test
    fun `given both street name and ref, then road details should consist of street name as name and ref as road number`() {
        val routeDirectionInfo = RouteDirectionInfo(DEFAULT_AVERAGE_SPEED, DEFAULT_TURN_TYPE).apply {
            ref = STREET_REF
            streetName = STREET_NAME
        }

        val result = routeDirectionInfo.roadDetails

        val expected = RoadDetails(roadNumber = STREET_REF, name = STREET_NAME)
        assertEquals(expected, result)
    }

    @Test
    fun `given destination name and no street name and ref, then road details should consist of name and first road number taken from destination name`() {
        val routeDirectionInfo = RouteDirectionInfo(DEFAULT_AVERAGE_SPEED, DEFAULT_TURN_TYPE).apply {
            destinationName = "A2;S11, Warszawa;Bydgoszcz;Katowice"
        }

        val result = routeDirectionInfo.roadDetails

        val expected = RoadDetails(roadNumber = ROAD_NUMBER, name = ROAD_DESTINATION_NAME)
        assertEquals(expected, result)
    }

    @Test
    fun `given destination name without road number and no street name and ref, then road details should consist of name taken from destination name`() {
        val routeDirectionInfo = RouteDirectionInfo(DEFAULT_AVERAGE_SPEED, DEFAULT_TURN_TYPE).apply {
            destinationName = "Warszawa;Bydgoszcz;Katowice"
        }

        val result = routeDirectionInfo.roadDetails

        val expected = RoadDetails(roadNumber = null, name = ROAD_DESTINATION_NAME)
        assertEquals(expected, result)
    }

    @Test
    fun `given street name without ref and destination name without road number, then road details should consist of street name as name`() {
        val routeDirectionInfo = RouteDirectionInfo(DEFAULT_AVERAGE_SPEED, DEFAULT_TURN_TYPE).apply {
            streetName = STREET_NAME
            destinationName = "Warszawa;Bydgoszcz;Katowice"
        }

        val result = routeDirectionInfo.roadDetails

        val expected = RoadDetails(roadNumber = null, name = STREET_NAME)
        assertEquals(expected, result)
    }

    @Test
    fun `given street name without ref and destination name with road number, then road details should consist of name and first road number taken from destination name`() {
        val routeDirectionInfo = RouteDirectionInfo(DEFAULT_AVERAGE_SPEED, DEFAULT_TURN_TYPE).apply {
            streetName = STREET_NAME
            destinationName = "A2;S11, Warszawa;Bydgoszcz;Katowice"
        }

        val result = routeDirectionInfo.roadDetails

        val expected = RoadDetails(roadNumber = ROAD_NUMBER, name = ROAD_DESTINATION_NAME)
        assertEquals(expected, result)
    }

    @Test
    fun `given ref without street name and destination name without road number, then road details should consist of ref as road number`() {
        val routeDirectionInfo = RouteDirectionInfo(DEFAULT_AVERAGE_SPEED, DEFAULT_TURN_TYPE).apply {
            ref = STREET_REF
            destinationName = "Warszawa;Bydgoszcz;Katowice"
        }

        val result = routeDirectionInfo.roadDetails

        val expected = RoadDetails(roadNumber = STREET_REF, name = null)
        assertEquals(expected, result)
    }

    @Test
    fun `given ref without street name and destination name with road number, then road details should consist of name and first road number taken from destination name`() {
        val routeDirectionInfo = RouteDirectionInfo(DEFAULT_AVERAGE_SPEED, DEFAULT_TURN_TYPE).apply {
            ref = STREET_REF
            destinationName = "A2;S11, Warszawa;Bydgoszcz;Katowice"
        }

        val result = routeDirectionInfo.roadDetails

        val expected = RoadDetails(roadNumber = ROAD_NUMBER, name = ROAD_DESTINATION_NAME)
        assertEquals(expected, result)
    }

    @Test
    fun `given no street name, ref and destination name, then road details should be empty`() {
        val routeDirectionInfo = RouteDirectionInfo(DEFAULT_AVERAGE_SPEED, DEFAULT_TURN_TYPE)

        val result = routeDirectionInfo.roadDetails

        val expected = RoadDetails(roadNumber = null, name = null)
        assertEquals(expected, result)
    }


    companion object {
        private const val DEFAULT_AVERAGE_SPEED = 35f
        private val DEFAULT_TURN_TYPE = TurnType.valueOf(2, false)

        private const val STREET_NAME = "Strumykowa"
        private const val STREET_REF = "120"
        private const val ROAD_NUMBER = "A2"
        private const val ROAD_DESTINATION_NAME = "Warszawa, Bydgoszcz, Katowice"
    }
}