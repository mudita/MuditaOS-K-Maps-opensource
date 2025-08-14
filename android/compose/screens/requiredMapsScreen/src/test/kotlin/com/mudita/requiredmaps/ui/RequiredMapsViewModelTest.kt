@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mudita.requiredmaps.ui

import app.cash.turbine.testIn
import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.domain.DownloadingStatePublisher
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.ui.Action
import com.mudita.download.ui.DownloadItemAction
import com.mudita.download.ui.ErrorType
import com.mudita.download.usecase.SetDownloadPausedUseCase
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkStatus
import com.mudita.map.common.utils.network.NetworkType
import com.mudita.maps.data.api.Resource
import com.mudita.requiredmaps.usecase.GetRequiredMapsUseCase
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class RequiredMapsViewModelTest {

    @MockK
    private lateinit var getRequiredMapsUseCase: GetRequiredMapsUseCase

    @MockK
    private lateinit var networkManager: NetworkManager

    @RelaxedMockK
    private lateinit var setDownloadPausedUseCase: SetDownloadPausedUseCase

    @RelaxedMockK
    private lateinit var cancelDownloadUseCase: CancelDownloadUseCase

    @RelaxedMockK
    private lateinit var memoryManager: MemoryManager

    @SpyK
    private var downloadingStatePublisher = DownloadingStatePublisher()

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private fun getViewModel(): RequiredMapsViewModel =
        RequiredMapsViewModel(
            getRequiredMapsUseCase,
            downloadingStatePublisher.getDownloadingStateUseCase,
            downloadingStatePublisher.getDownloadProgressUseCase,
            networkManager,
            setDownloadPausedUseCase,
            cancelDownloadUseCase,
            memoryManager,
            downloadingStatePublisher.errorNotificationUseCase,
            UnconfinedTestDispatcher(testCoroutineScheduler),
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        every { getRequiredMapsUseCase() } returns flowOf(Resource.Success(downloadItems))
        every { networkManager.networkStatus } returns flowOf(NetworkStatus.Available)
        every { downloadingStatePublisher.getDownloadProgressUseCase.invoke() } returns flowOf(emptyMap())
    }

    @AfterEach
    fun cleanUp() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `Given Wi-Fi only is not reachable, when download is called, then an appropriate error is set to the state`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)

            // Given
            every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns false
            every { networkManager.isNetworkReachable(NetworkType.ALL) } returns false

            // When
            viewModel.download(downloadItems.first())

            // Then
            Assertions.assertEquals(
                ErrorType.WifiNetwork,
                stateTurbine.expectMostRecentItem().errorType
            )
            stateTurbine.cancel()
        }

    @Test
    fun `Given the loaded state, when the map list is empty, then the 'Download all' button should be hidden`() =
        runTest {
            // Given
            unmockkObject(getRequiredMapsUseCase)
            every { getRequiredMapsUseCase() } returns flowOf(Resource.Error(RuntimeException()))
            val viewModel = getViewModel()

            // When
            val stateTurbine = viewModel.state.testIn(this)

            // Then
            val currentState = stateTurbine.expectMostRecentItem()
            Assertions.assertTrue(currentState.downloadItems.isEmpty())
            Assertions.assertFalse(currentState.showDownloadAllButton)
            stateTurbine.cancel()
        }

    @Test
    fun `Given no network is reachable, when download is called, then an appropriate error is set to the state`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)

            // Given
            every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns true
            every { networkManager.isNetworkReachable(NetworkType.ALL) } returns false

            // When
            viewModel.download(downloadItems.first())

            // Then
            Assertions.assertEquals(
                ErrorType.Network,
                stateTurbine.expectMostRecentItem().errorType
            )
            stateTurbine.cancel()
        }

    @Test
    fun `Given insufficient storage, when download is called, then an appropriate error is set to the state`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)

            // Given
            every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns true
            every { networkManager.isNetworkReachable(NetworkType.ALL) } returns true
            every { memoryManager.hasEnoughSpace(any()) } returns false

            // When
            viewModel.download(downloadItems.first())

            // Then
            Assertions.assertEquals(
                ErrorType.Memory,
                stateTurbine.expectMostRecentItem().errorType
            )
            stateTurbine.cancel()
        }

    @Test
    fun `Given all download requirements are met, when download is called, then download action is emitted`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.action.testIn(this)

            // Given
            every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns true
            every { networkManager.isNetworkReachable(NetworkType.ALL) } returns true
            every { memoryManager.hasEnoughSpace(any()) } returns true

            // When
            val downloadItem = downloadItems.first()
            viewModel.download(downloadItem)

            // Then
            Assertions.assertEquals(Action.Download(downloadItem), stateTurbine.awaitItem())
            stateTurbine.cancel()
        }

    @Test
    fun `Given memory check, when downloadAll is called, then the full size of all download items is checked`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)
            val state = stateTurbine.awaitItem()

            // Given
            every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns true
            every { networkManager.isNetworkReachable(NetworkType.ALL) } returns true
            every { memoryManager.hasEnoughSpace(any()) } returns true

            // When
            viewModel.downloadAll()

            // Then
            val totalDownloadItemSize = state.downloadItems.sumOf { it.getFullSize() }
            Assertions.assertTrue(totalDownloadItemSize > 0)
            verify { memoryManager.hasEnoughSpace(totalDownloadItemSize) }
            stateTurbine.cancel()
        }

    @Test
    fun `When a download is about to be cancelled, then a cancel action is emitted and the download is paused`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)

            // When
            val downloadItem = downloadItems.first()
            viewModel.handleCancelDownload(downloadItem)

            // Then
            verify { setDownloadPausedUseCase(true) }
            Assertions.assertEquals(
                DownloadItemAction.Cancel(downloadItem),
                stateTurbine.expectMostRecentItem().downloadItemAction,
            )
            stateTurbine.cancel()
        }

    @Test
    fun `When a download is interrupted, then an interruption action is emitted`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)

            // When
            val downloadItem = downloadItems.first()
            viewModel.handleDownloadError(downloadItem, DownloadingState.ErrorState.InternetConnectionError)

            // Then
            Assertions.assertEquals(
                DownloadItemAction.AlertDownloadInterrupted(downloadItem),
                stateTurbine.expectMostRecentItem().downloadItemAction,
            )
            stateTurbine.cancel()
        }

    @Test
    fun `When download is cancelled, then the downloadItemAction is null and the cancellation is propagated if the map was being downloaded`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)
            val downloadItem = downloadItems.first()
            downloadingStatePublisher.publishDownloadingState(downloadItem, DownloadingState.Downloading)
            viewModel.handleCancelDownload(downloadItem)
            Assertions.assertNotNull(stateTurbine.expectMostRecentItem().downloadItemAction)

            // When
            viewModel.cancelDownload(downloadItem)

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().downloadItemAction)
            verify(exactly = 1) { cancelDownloadUseCase() }
            verify { setDownloadPausedUseCase(false) }

            stateTurbine.cancel()
        }

    @Test
    fun `When download is cancelled, then the downloadItemAction is null and the cancellation is NOT propagated if the map was being downloaded`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)
            val downloadItem = downloadItems.first()
            viewModel.handleCancelDownload(downloadItem)
            Assertions.assertNotNull(stateTurbine.expectMostRecentItem().downloadItemAction)

            // When
            viewModel.cancelDownload(downloadItem)

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().downloadItemAction)
            verify(exactly = 0) { cancelDownloadUseCase() }
            verify { setDownloadPausedUseCase(false) }

            stateTurbine.cancel()
        }

    @Test
    fun `Given the state has an error, when the error is dismissed, then the state contains no errors`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)

            every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns true
            every { networkManager.isNetworkReachable(NetworkType.ALL) } returns false

            viewModel.download(downloadItems.first())

            // Given
            Assertions.assertEquals(
                ErrorType.Network,
                stateTurbine.expectMostRecentItem().errorType
            )

            // When
            viewModel.dismissError()

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().errorType)
            stateTurbine.cancel()
        }

    @Test
    fun `When download action is dismissed, then the downloadItemAction is null and the download is resumed`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)
            viewModel.handleCancelDownload(downloadItems.first())
            Assertions.assertNotNull(stateTurbine.expectMostRecentItem().downloadItemAction)

            // When
            viewModel.dismissDownloadAction()

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().downloadItemAction)
            verify(exactly = 0) { cancelDownloadUseCase() }
            verify { setDownloadPausedUseCase(false) }

            stateTurbine.cancel()
        }

    @Test
    fun `When error notification emits, then an appropriate action is emitted`() = runTest {
        val viewModel = getViewModel()
        val stateTurbine = viewModel.state.testIn(this)

        // When
        val downloadItem = downloadItems.first()
        downloadingStatePublisher.publishDownloadingStateWithNotification(
            downloadItem,
            DownloadingState.ErrorState.MemoryNotEnough
        )

        // Then
        Assertions.assertEquals(
            DownloadItemAction.AlertInsufficientMemory(downloadItem),
            stateTurbine.expectMostRecentItem().downloadItemAction,
        )
        stateTurbine.cancel()
    }

    @Test
    fun `When download action is cleared, then the downloadItemAction is null`() =
        runTest {
            val viewModel = getViewModel()
            val stateTurbine = viewModel.state.testIn(this)
            viewModel.handleDownloadError(downloadItems.first(), DownloadingState.ErrorState.MemoryNotEnough)
            Assertions.assertNotNull(stateTurbine.expectMostRecentItem().downloadItemAction)

            // When
            viewModel.clearDownloadItemAction()

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().downloadItemAction)
            verify(exactly = 0) { cancelDownloadUseCase() }
            verify(exactly = 0) { setDownloadPausedUseCase(false) }

            stateTurbine.cancel()
        }

    companion object {
        private val downloadItems = listOf(
            DownloadItem(
                name = "Poland_dolnoslaskie",
                description = "",
                fileName = "Poland_dolnoslaskie.zip",
                size = "123 MB",
                targetSize = "144 MB",
            ),
            DownloadItem(
                name = "Poland_wielkopolskie",
                description = "",
                fileName = "Poland_wielkopolskie.zip",
                size = "145 MB",
                targetSize = "168 MB",
            ),
        )
    }
}