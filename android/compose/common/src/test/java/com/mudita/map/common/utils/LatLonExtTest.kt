package com.mudita.map.common.utils

import net.osmand.data.LatLon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LatLonExtTest {

    @Test
    fun `given latitude is a negative number, longitude is a negative number, then formattedCoordinates method should return south and west coordinates`() {
        val latLon = LatLon(-10.45, -23.75)

        val result = latLon.formattedCoordinates()

        val expected = "10.45° S, 23.75° W"
        assertEquals(expected, result)
    }

    @Test
    fun `given latitude is a positive number, longitude is a positive number, then formattedCoordinates method should return north and east coordinates`() {
        val latLon = LatLon(10.45, 23.75)

        val result = latLon.formattedCoordinates()

        val expected = "10.45° N, 23.75° E"
        assertEquals(expected, result)
    }

    @Test
    fun `given latitude is equal to zero, longitude is equal to zero, then formattedCoordinates method should return coordinates without direction suffix`() {
        val latLon = LatLon(0.0, 0.0)

        val result = latLon.formattedCoordinates()

        val expected = "0°, 0°"
        assertEquals(expected, result)
    }

    @Test
    fun `given latitude and longitude have more than 5 digits after decimal point, then formattedCoordinates method should return rounded coordinates`() {
        val latLon = LatLon(2.137548, -50.746382)

        val result = latLon.formattedCoordinates()

        val expected = "2.13755° N, 50.74638° W"
        assertEquals(expected, result)
    }
}
