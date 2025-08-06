package com.mudita.download.usecase

import app.cash.turbine.test
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.map.common.download.OnDownloadFinishUseCase
import com.mudita.map.common.maps.GetMissingMapsUseCase
import com.mudita.map.common.region.GetRegionsIndexedEvents
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkStatus
import com.mudita.maps.data.api.Resource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.osmand.data.LatLon
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class GetDownloadItemUseCaseTest {

    @MockK
    private lateinit var getMissingMapsUseCase: GetMissingMapsUseCase

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    @MockK
    private lateinit var networkManager: NetworkManager

    @MockK
    private lateinit var onDownloadFinishUseCase: OnDownloadFinishUseCase

    @MockK
    private lateinit var getRegionsIndexedEvents: GetRegionsIndexedEvents

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var getDownloadItemUseCase: GetDownloadItemUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        getDownloadItemUseCase = GetDownloadItemUseCase(
            getMissingMapsUseCase = getMissingMapsUseCase,
            downloadRepository = downloadRepository,
            networkManager = networkManager,
            onDownloadFinishUseCase = onDownloadFinishUseCase,
            getRegionsIndexedEvents = getRegionsIndexedEvents,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given there is no missing map for given coordinates, when getDownloadItemUseCase invoked, should return success with null DownloadItem`() =
        runTest {
            // Given
            coEvery { getMissingMapsUseCase(any<LatLon>()) } returns Result.success(emptyList())

            // When
            getDownloadItemUseCase(LAT_LON).test {
                // Then
                assertEquals(Resource.Success(null), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `Given search for missing map failed, when getDownloadItemUseCase invoked, should return error result`() =
        runTest {
            // Given
            val exception = IllegalStateException()
            coEvery { getMissingMapsUseCase(any<LatLon>()) } returns Result.failure(exception)

            // When
            getDownloadItemUseCase(LAT_LON).test {
                // Then
                assertEquals(Resource.Error<DownloadItem?>(exception), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `Given there is a missing map for given coordinates, when getDownloadItemUseCase invoked, should return success with corresponding DownloadItem`() =
        runTest {
            // Given
            coEvery { getMissingMapsUseCase(any<LatLon>()) } returns Result.success(listOf(MISSING_MAP_NAME))
            every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
            every { downloadRepository.getIndexesList() } returns flowOf(Resource.Success(poland))
            every { onDownloadFinishUseCase() } returns flowOf()
            every { getRegionsIndexedEvents() } returns flowOf(Unit)

            // When
            getDownloadItemUseCase(LAT_LON).test {
                // Then
                assertEquals(Resource.Loading(), awaitItem())
                assertEquals(Resource.Success(masovian), awaitItem())
                awaitComplete()
            }
        }

    @Test
    @DisplayName("Given there is a missing map for given coordinates but it is currently downloaded, when getDownloadItemUseCase invoked and download is finished, should return success with corresponding DownloadItem and with downloaded field set to true")
    fun testMissingDownloadItemFinishedDownloadingWhenGetDownloadItemUseCaseIsInvoked() =
        runTest {
            // Given
            coEvery { getMissingMapsUseCase(any<LatLon>()) } returns Result.success(listOf(MISSING_MAP_NAME))
            every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
            every { downloadRepository.getIndexesList() } returns flowOf(Resource.Success(poland))
            every { onDownloadFinishUseCase() } returns flowOf(MISSING_MAP_NAME)
            every { getRegionsIndexedEvents() } returns flowOf(Unit)

            // When
            getDownloadItemUseCase(LAT_LON).test {
                // Then
                assertEquals(Resource.Loading(), awaitItem())
                assertEquals(Resource.Success(masovian.copy(downloaded = true)), awaitItem())
                awaitComplete()
            }
        }

    @Test
    @DisplayName("Given there is a missing map for given coordinates but getIndexesList failed, when getDownloadItemUseCase invoked and download is finished, should return success with corresponding DownloadItem and with downloaded field set to true")
    fun testGetIndexesListFailedWhenGetDownloadItemUseCaseIsInvoked() =
        runTest {
            // Given
            coEvery { getMissingMapsUseCase(any<LatLon>()) } returns Result.success(listOf(MISSING_MAP_NAME))
            every { networkManager.networkStatus } returns flowOf(NetworkStatus.Unavailable)
            val exception = UnknownHostException()
            every { downloadRepository.getIndexesList() } returns flowOf(Resource.Error(exception))
            every { onDownloadFinishUseCase() } returns flowOf()
            every { getRegionsIndexedEvents() } returns flowOf(Unit)

            // When
            getDownloadItemUseCase(LAT_LON).test {
                // Then
                assertEquals(Resource.Loading(), awaitItem())
                assertEquals(Resource.Error<DownloadItem?>(exception), awaitItem())
                awaitComplete()
            }
        }

    companion object {
        private val LAT_LON = LatLon(18.123, 52.321)
        private const val MISSING_MAP_NAME = "Poland_mazowieckie_europe"

        private val masovian = DownloadItem(
            name = "Mazowieckie",
            description = "297 MB",
            fileName = "Poland_mazowieckie_europe.zip",
            size = "297 MB",
            targetSize = "479 MB",
            downloaded = false
        )

        private val greaterPoland = DownloadItem(
            name = "Wielkopolskie",
            description = "163 MB",
            fileName = "Poland_wielkopolskie_europe.zip",
            size = "163 MB",
            targetSize = "273 MB",
            downloaded = true
        )

        private val poland = ResourceGroup(
            name = "Poland",
            parents = listOf("europe"),
            individualDownloadItems = mutableListOf(
                masovian,
                greaterPoland
            )
        )
    }
}