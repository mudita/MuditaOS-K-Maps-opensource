package com.mudita.requiredmaps.usecase

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.map.common.download.OnDownloadFinishUseCase
import com.mudita.map.common.region.GetRegionsIndexedEvents
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkStatus
import com.mudita.maps.data.api.Resource
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class GetRequiredMapsUseCaseTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    @MockK
    private lateinit var networkManager: NetworkManager

    @MockK
    private lateinit var onDownloadFinishUseCase: OnDownloadFinishUseCase

    @MockK
    private lateinit var getRegionsIndexedEvents: GetRegionsIndexedEvents

    private val missingMaps = listOf(MISSING_MAP_NAME)

    private val savedStateHandle = SavedStateHandle()

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var getRequiredMapsUseCase: GetRequiredMapsUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        getRequiredMapsUseCase = GetRequiredMapsUseCase(
            missingMaps = missingMaps,
            downloadRepository = downloadRepository,
            networkManager = networkManager,
            onDownloadFinishUseCase = onDownloadFinishUseCase,
            savedStateHandle = savedStateHandle,
            getRegionsIndexedEvents = getRegionsIndexedEvents,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given there is a missing map, when getRequiredMapsUseCase invoked, should return success with corresponding DownloadItem`() =
        runTest {
            // Given
            every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
            every { downloadRepository.getIndexesList() } returns flowOf(Resource.Success(poland))
            every { onDownloadFinishUseCase() } returns flowOf()
            every { getRegionsIndexedEvents() } returns flowOf(Unit)

            // When
            getRequiredMapsUseCase().test {
                // Then
                assertEquals(Resource.Loading(), awaitItem())
                assertEquals(Resource.Success(listOf(masovian)), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `Given getIndexesList fails, when getRequiredMapsUseCase invoked, should return error`() =
        runTest {
            // Given
            every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
            val exception = UnknownHostException()
            every { downloadRepository.getIndexesList() } returns flowOf(Resource.Error(exception))
            every { onDownloadFinishUseCase() } returns flowOf()
            every { getRegionsIndexedEvents() } returns flowOf(Unit)

            // When
            getRequiredMapsUseCase().test {
                // Then
                assertEquals(Resource.Loading(), awaitItem())
                assertEquals(Resource.Error<DownloadItem?>(exception), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `Given missing map download finished, when getRequiredMapsUseCase invoked, should return success with corresponding DownloadItem and downloaded value set to true`() =
        runTest {
            // Given
            every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
            every { downloadRepository.getIndexesList() } returns flowOf(Resource.Success(poland))
            every { onDownloadFinishUseCase() } returns flowOf(MISSING_MAP_NAME)
            every { getRegionsIndexedEvents() } returns flowOf(Unit)

            // When
            getRequiredMapsUseCase().test {
                // Then
                assertEquals(Resource.Loading(), awaitItem())
                assertEquals(Resource.Success(listOf(masovian.copy(downloaded = true))), awaitItem())
                assertEquals(listOf(MISSING_MAP_NAME), savedStateHandle["downloaded_maps"])
                awaitComplete()
            }
        }

    companion object {
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