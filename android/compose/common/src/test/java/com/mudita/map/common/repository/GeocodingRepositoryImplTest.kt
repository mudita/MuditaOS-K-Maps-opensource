package com.mudita.map.common.repository

import com.mudita.map.common.geocode.GeocodingAddress
import com.mudita.map.common.geocode.GeocodingAddressNotFoundException
import com.mudita.map.common.geocode.GeocodingLookupService
import com.mudita.map.common.repository.geocoding.GeocodingRepository
import com.mudita.map.common.repository.geocoding.GeocodingRepositoryImpl
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.osmand.data.LatLon
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class GeocodingRepositoryImplTest {

    @MockK private lateinit var geocodingLookupService: GeocodingLookupService

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var geocodingRepository: GeocodingRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        geocodingRepository = GeocodingRepositoryImpl(
            geocodingLookupService,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given service returns success, when searchAddress called, should return address for given coordinates`() = runTest {
        // Given
        val address = GeocodingAddress(
            city = "City",
            street = "Street",
            buildingNumber = "5",
            postcode = "12-345",
            latLon = LAT_LON,
        )
        coEvery { geocodingLookupService.getFromLocation(any()) } returns Result.success(address)

        // When
        val result = geocodingRepository.searchAddress(latLon = LAT_LON)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(address, result.getOrNull())
    }

    @Test
    fun `Given service returns error, when searchAddress called, should return failed result`() = runTest {
        // Given
        val exception = GeocodingAddressNotFoundException(LAT_LON)
        coEvery { geocodingLookupService.getFromLocation(any()) } returns Result.failure(exception)

        // When
        val result = geocodingRepository.searchAddress(latLon = LAT_LON)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `When cancelSearch called, should cancel search in lookup service`() = runTest {
        // Given
        justRun { geocodingLookupService.cancel(any()) }

        // When
        geocodingRepository.cancelSearch(latLon = LAT_LON)

        // Then
        verify { geocodingLookupService.cancel(LAT_LON) }
    }

    companion object {
        private val LAT_LON = LatLon(18.123, 52.321)
    }
}