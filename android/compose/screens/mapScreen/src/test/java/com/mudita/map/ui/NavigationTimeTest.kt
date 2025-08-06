package com.mudita.map.ui

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class NavigationTimeTest {

    @Test
    fun `Given the time is less or equal to 30 seconds, then NavigationTime is in seconds`() {
        // Given
        val seconds = 30

        // When
        val navigationTime = NavigationTime.create(seconds)

        // Then
        Assertions.assertEquals(NavigationTime.Seconds(seconds), navigationTime)
    }

    @Test
    fun `Given the time is more than 31 seconds, then NavigationTime is in minutes`() {
        // Given
        val seconds = 31

        // When
        val navigationTime = NavigationTime.create(seconds)

        // Then
        Assertions.assertEquals(NavigationTime.Minutes(1), navigationTime)
    }

    @Test
    fun `Given the time is in minutes and the remaining seconds are more than 30, then NavigationTime is in minutes`() {
        // Given
        val seconds = (2.minutes + 31.seconds).inWholeSeconds.toInt()

        // When
        val navigationTime = NavigationTime.create(seconds)

        // Then
        Assertions.assertEquals(NavigationTime.Minutes(3), navigationTime)
    }

    @Test
    fun `Given the time is more than 59 minutes, then NavigationTime is in hours and minutes`() {
        // Given
        val seconds = 60.minutes.inWholeSeconds.toInt()

        // When
        val navigationTime = NavigationTime.create(seconds)

        // Then
        Assertions.assertEquals(NavigationTime.HoursMinutes(1, 0), navigationTime)
    }

    @Test
    fun `Given the time is more than 59 minutes and the remaining seconds are more than 30, then NavigationTime is in hours and rounded minutes`() {
        // Given
        val seconds = (2.hours + 29.minutes + 31.seconds).inWholeSeconds.toInt()

        // When
        val navigationTime = NavigationTime.create(seconds)

        // Then
        Assertions.assertEquals(NavigationTime.HoursMinutes(2, 30), navigationTime)
    }
}
