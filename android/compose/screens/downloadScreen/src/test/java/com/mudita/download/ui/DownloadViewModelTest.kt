package com.mudita.download.ui

import app.cash.turbine.test
import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.domain.DeleteDownloadFromDbUseCase
import com.mudita.download.domain.DeleteMapUseCase
import com.mudita.download.domain.DownloadingStatePublisher
import com.mudita.download.domain.GetIndexesUseCase
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.LocalIndex
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.download.usecase.SetDownloadPausedUseCase
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkStatus
import com.mudita.maps.data.api.Resource
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import java.net.SocketTimeoutException
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class DownloadViewModelTest {

    @RelaxedMockK private lateinit var networkManager: NetworkManager
    @RelaxedMockK private lateinit var memoryManager: MemoryManager
    private val getIndexesUseCase: GetIndexesUseCase = mockk(relaxed = true) {
        every { this@mockk.invoke() } returns flowOf(Resource.Success(allRegions))
    }
    @MockK private lateinit var deleteMapUseCase: DeleteMapUseCase
    @MockK private lateinit var deleteDownloadFromDbUseCase: DeleteDownloadFromDbUseCase
    @MockK private lateinit var cancelDownloadUseCase: CancelDownloadUseCase
    @MockK(relaxed = true) private lateinit var setDownloadPausedUseCase: SetDownloadPausedUseCase

    @SpyK private var downloadingStatePublisher = DownloadingStatePublisher()
    private val testCoroutineScheduler = TestCoroutineScheduler()

    private val viewModel: DownloadViewModel by lazy {
        DownloadViewModel(
            networkManager,
            memoryManager,
            getIndexesUseCase,
            deleteMapUseCase,
            deleteDownloadFromDbUseCase,
            cancelDownloadUseCase,
            setDownloadPausedUseCase,
            downloadingStatePublisher.getDownloadingStateUseCase,
            downloadingStatePublisher.getDownloadProgressUseCase,
            downloadingStatePublisher.errorNotificationUseCase,
            UnconfinedTestDispatcher(testCoroutineScheduler),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        every { downloadingStatePublisher.getDownloadProgressUseCase.invoke() } returns flowOf(emptyMap())
    }

    @AfterEach
    fun cleanup() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `Given getIndexesUseCase returns result successfully, when view model is initialized, should get all maps`() = runTest {
        // Given
        every { getIndexesUseCase.invoke() } returns flowOf(Resource.Success(allRegions))
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)

        // When
        viewModel // Initializes `lazy`

        // Then
        viewModel.uiState.test {
            assertEquals(allRegions.groups, expectMostRecentItem().downloadIndexes)
        }
    }

    @Test
    fun `Given getIndexesUseCase returns network error, when view model is initialized, then should update state with error`() = runTest {
        // Given
        every { getIndexesUseCase.invoke() } returns flowOf(Resource.Error(SocketTimeoutException()))
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Unavailable)

        // When
        viewModel // Initializes `lazy`

        // Then
        viewModel.uiState.test {
            assertFalse(awaitItem().isNetworkAvailable)
        }
    }

    @Test
    fun `Given SearchQueryChange intent, when intent is obtained, then should update state with search query`() = runTest {
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)

        // Given
        val intent = DownloadViewModel.Intent.SearchQueryChange("query")

        // When
        viewModel.obtainIntent(intent)

        // Then
        viewModel.uiState.test {
            assertEquals(intent.value, awaitItem().searchQuery)
        }
    }

    @Test
    fun `Given SearchQueryChange intent with not matching query, when intent is obtained, then should update state with empty result`() = runTest {
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)

        // Given
        val intent = DownloadViewModel.Intent.SearchQueryChange("random query")

        // When
        viewModel.obtainIntent(intent)

        // Then
        viewModel.uiState.test {
            assertTrue(awaitItem().isSearchResultEmpty)
        }
    }

    @Test
    fun `Given SearchQueryChange intent with matching query, when intent is obtained, then should update state with non-empty result`() = runTest {
        every { getIndexesUseCase() } returns flowOf(Resource.Success(allRegions))
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)

        // Given
        val intent = DownloadViewModel.Intent.SearchQueryChange("al")

        // When
        viewModel.obtainIntent(intent)

        // Then
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isSearchResultEmpty)
            assertEquals(2, state.downloadIndexes.size)
            assertTrue(state.downloadIndexes.contains(algeria))
            assertTrue(state.downloadIndexes.contains(albania))
        }
    }

    @Test
    fun `Given DownloadItem is cancelled, downloads are paused and confirmation action is emitted`() = runTest {
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)

        // Given
        val downloadItem = algeria

        // When
        viewModel.cancelDownload(downloadItem)

        // Then
        verify(exactly = 1) { setDownloadPausedUseCase(true) }
        viewModel.action.test {
            val action = awaitItem()
            assertEquals(algeria, (action as DownloadViewModel.Action.CancelDownload).map)
        }
    }

    @Test
    fun `Given LocalIndex update is cancelled, downloads are paused and confirmation action is emitted`() = runTest {
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)

        // Given
        every { getIndexesUseCase() } returns flowOf(Resource.Success(allRegions))
        val localIndexToCancel = LocalIndex(DownloadViewModel.LocalIndexType.MAP_DATA, File(algeria.fileName.replace(".zip", ".obf")), "")

        // When
        viewModel.cancelUpdate(localIndexToCancel)

        // Then
        verify(exactly = 1) { setDownloadPausedUseCase(true) }
        viewModel.action.test {
            val action = awaitItem()
            assertEquals(algeria, (action as DownloadViewModel.Action.CancelDownload).map)
        }
    }

    @Test
    fun `When cancel download action is cancelled, downloads are resumed and an empty action is emitted`() = runTest {
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)

        // When
        viewModel.onActionCanceled()

        // Then
        verify(exactly = 1) { setDownloadPausedUseCase(false) }
        viewModel.action.test {
            val action = awaitItem()
            assertEquals(DownloadViewModel.Action.Empty, action)
        }
    }

    @Test
    fun `Given empty list of downloaded items, when deleteMap is called, then should cancel edit mode`() = runTest {
        // Given
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
        val localIndex = LocalIndex(DownloadViewModel.LocalIndexType.MAP_DATA, File("/path/Albania_europe.obf"), parentName = "Europe")
        coJustRun { deleteMapUseCase.invoke(any()) }

        // When
        viewModel.deleteMap(localIndex)

        // Then
        viewModel.action.test {
            val action = awaitItem()
            assertInstanceOf(DownloadViewModel.Action.CancelEditMode::class.java, action)
        }
    }

    @Test
    fun `Given localMap is updated, when user has no internet connection, then error is emitted`() = runTest {
        // Given
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
        every { networkManager.isNetworkReachable(any()) } returns false

        // When
        viewModel.updateMap(localMap)

        // Then
        viewModel.action.test {
            assertEquals(
                DownloadViewModel.Action.ShowWifiNetworkError(viewModel.findDownloadItem(localMap)!!),
                expectMostRecentItem(),
            )
        }
    }

    @Test
    fun `Given localMap is updated, when user has insufficient memory, then error is emitted`() = runTest {
        // Given
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
        every { networkManager.isNetworkReachable(any()) } returns true
        every { memoryManager.hasEnoughSpace(any()) } returns false

        // When
        viewModel.updateMap(localMap)

        // Then
        viewModel.action.test {
            assertEquals(
                DownloadViewModel.Action.ShowMemoryError(viewModel.findDownloadItem(localMap)!!),
                expectMostRecentItem(),
            )
        }
    }

    companion object {
        private val algeria = DownloadItem(
            name = "Algeria",
            description = "230 MB",
            fileName = "Algeria_africa.zip",
            size = "230 MB",
            targetSize = "352 MB",
            downloaded = false
        )

        private val afghanistan = DownloadItem(
            name = "Afganistan",
            description = "112 MB",
            fileName = "Afghanistan_asia.zip",
            size = "112 MB",
            targetSize = "164 MB",
            downloaded = false
        )

        private val albania = DownloadItem(
            name = "Albania",
            description = "48 MB",
            fileName = "Albania_europe.zip",
            size = "48 MB",
            targetSize = "73 MB",
            downloaded = false
        )

        private val austria = DownloadItem(
            name = "Austria",
            description = "836 MB",
            fileName = "Austria_europe.zip",
            size = "836 MB",
            targetSize = "1391 MB",
            downloaded = false
        )

        private val poland = ResourceGroup(
            name = "Poland",
            parents = listOf("europe"),
            individualDownloadItems = mutableListOf(
                DownloadItem(
                    name = "Mazowieckie",
                    description = "297 MB",
                    fileName = "Poland_mazowieckie_europe.zip",
                    size = "297 MB",
                    targetSize = "479 MB",
                    downloaded = false
                ),
                DownloadItem(
                    name = "Wielkopolskie",
                    description = "163 MB",
                    fileName = "Poland_wielkopolskie_europe.zip",
                    size = "163 MB",
                    targetSize = "273 MB",
                    downloaded = false
                )
            )
        )

        private val allRegions =
            ResourceGroup(
                name = "World",
                groups = mutableListOf(
                    ResourceGroup(
                        name = "Africa",
                        individualDownloadItems = mutableListOf(
                            algeria
                        )
                    ),
                    ResourceGroup(
                        name = "Asia",
                        individualDownloadItems = mutableListOf(
                            afghanistan
                        )
                    ),
                    ResourceGroup(
                        name = "Europe",
                        groups = mutableListOf(
                            poland
                        ),
                        individualDownloadItems = mutableListOf(
                            albania,
                            austria,
                        )
                    )
                )
            )

        private val localMap = LocalIndex(
            DownloadViewModel.LocalIndexType.MAP_DATA,
            File("Algeria_africa.obf"),
            "",
        )
    }
}