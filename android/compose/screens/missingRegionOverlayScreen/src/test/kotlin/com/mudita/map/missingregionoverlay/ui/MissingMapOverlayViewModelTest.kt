@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mudita.map.missingregionoverlay.ui

import app.cash.turbine.testIn
import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.domain.DownloadingStatePublisher
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.ui.Action
import com.mudita.download.ui.DownloadItemAction
import com.mudita.download.ui.ErrorType
import com.mudita.download.usecase.CheckDownloadRestrictionsUseCase
import com.mudita.download.usecase.GetDownloadItemUseCase
import com.mudita.download.usecase.SetDownloadPausedUseCase
import com.mudita.map.common.download.DownloadingState
import com.mudita.maps.data.api.Resource
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
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
class MissingMapOverlayViewModelTest {

    @MockK
    private lateinit var getDownloadItemUseCase: GetDownloadItemUseCase

    @MockK
    private lateinit var checkDownloadRestrictionsUseCase: CheckDownloadRestrictionsUseCase

    @RelaxedMockK
    private lateinit var setDownloadPausedUseCase: SetDownloadPausedUseCase

    @RelaxedMockK
    private lateinit var cancelDownloadUseCase: CancelDownloadUseCase

    @SpyK
    private var downloadingStatePublisher = DownloadingStatePublisher()

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private fun getViewModel() = MissingMapOverlayViewModel(
        getDownloadItemUseCase,
        downloadingStatePublisher.getDownloadingStateUseCase,
        downloadingStatePublisher.getDownloadProgressUseCase,
        downloadingStatePublisher.errorNotificationUseCase,
        checkDownloadRestrictionsUseCase,
        setDownloadPausedUseCase,
        cancelDownloadUseCase,
        UnconfinedTestDispatcher(testCoroutineScheduler),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        every { downloadingStatePublisher.getDownloadProgressUseCase.invoke() } returns flowOf(emptyMap())
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `Given a SearchItem is loaded, when a missing map is found, then the state has a DownloadItem`() =
        runTest {
            // Given
            val missingRegion = missingRegion
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)

            // When
            viewModel.setMissingRegion(missingRegion)

            // Then
            Assertions.assertEquals(downloadItem, stateTurbine.expectMostRecentItem().downloadItem)
            stateTurbine.cancel()
        }

    @Test
    fun `Given a SearchItem is loaded, when a missing map is NOT found, then the state has a null DownloadItem`() =
        runTest {
            // Given
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(null))
            val viewModel = getViewModel()
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)

            // When
            viewModel.setMissingRegion(missingRegion)

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().downloadItem)
            stateTurbine.cancel()
        }

    @Test
    fun `Given a SearchItem is loaded, when a missing map query returns an error, then the state has a null DownloadItem`() =
        runTest {
            // Given
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Error(Exception()))
            val viewModel = getViewModel()
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)

            // When
            viewModel.setMissingRegion(missingRegion)

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().downloadItem)
            stateTurbine.cancel()
        }

    @Test
    fun `Given a map is missing, when a download is started, then the DownloadingState is updated in the DownloadingState`() =
        runTest {
            // Given
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)
            viewModel.setMissingRegion(missingRegion)

            // When
            downloadingStatePublisher.publishDownloadingState(
                downloadItem,
                DownloadingState.Downloading
            )

            // Then
            Assertions.assertEquals(
                DownloadingState.Downloading,
                stateTurbine.expectMostRecentItem().downloadingState
            )
            stateTurbine.cancel()
        }

    @Test
    fun `Given download item click, when the DownloadingState is Default, then download checks are made`() =
        runTest {
            val viewModel = getViewModel()

            // Given
            every { checkDownloadRestrictionsUseCase(any()) } returns null

            // When
            viewModel.onDownloadItemClick(downloadItem, DownloadingState.Default)

            // Then
            verify(exactly = 1) { checkDownloadRestrictionsUseCase(downloadItem) }
        }

    @Test
    fun `Given download item click, when the DownloadingState is Error, then the state is updated with an action`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            viewModel.setMissingRegion(missingRegion)
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)
            val errorState = DownloadingState.ErrorState.InternetConnectionError

            // When
            viewModel.onDownloadItemClick(downloadItem, errorState)

            // Then
            Assertions.assertEquals(
                DownloadItemAction.get(downloadItem, errorState),
                stateTurbine.expectMostRecentItem().downloadItemAction,
            )

            stateTurbine.cancel()
        }

    @Test
    fun `Given download item click, when the DownloadingState is cancellable, then the state is updated with an action`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            viewModel.setMissingRegion(missingRegion)
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)

            // When
            viewModel.onDownloadItemClick(downloadItem, DownloadingState.Downloading)

            // Then
            Assertions.assertEquals(
                DownloadItemAction.Cancel(downloadItem),
                stateTurbine.expectMostRecentItem().downloadItemAction,
            )

            stateTurbine.cancel()
        }

    @Test
    fun `Given insufficient storage, when download is called, then an appropriate error is set to the state`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            viewModel.setMissingRegion(missingRegion)
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)

            // Given
            every { checkDownloadRestrictionsUseCase(any()) } returns ErrorType.Memory

            // When
            viewModel.onDownloadItemClick(downloadItem, DownloadingState.Default)

            // Then
            Assertions.assertEquals(ErrorType.Memory, stateTurbine.expectMostRecentItem().errorType)
            stateTurbine.cancel()
        }

    @Test
    fun `Given all download requirements are met, when download is called, then download action is emitted`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            viewModel.setMissingRegion(missingRegion)
            val stateTurbine = viewModel.action.testIn(this)

            // Given
            every { checkDownloadRestrictionsUseCase(any()) } returns null

            // When
            viewModel.onDownloadItemClick(downloadItem, DownloadingState.Default)

            // Then
            Assertions
                .assertEquals(Action.Download(downloadItem), stateTurbine.expectMostRecentItem())
            stateTurbine.cancel()
        }

    @Test
    fun `Given download error, when error notification emits, then the state is updated with an appropriate action`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)
            viewModel.setMissingRegion(missingRegion)

            // When
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
    fun `Given DownloadItem is downloading, when download action is dismissed, then the downloadItemAction is null and the download is resumed`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            viewModel.setMissingRegion(missingRegion)
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)
            viewModel.onDownloadItemClick(downloadItem, DownloadingState.Downloading)
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
    fun `Given DownloadItem is downloading, when download is cancelled, then the downloadItemAction is null and the cancellation is propagated`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            viewModel.setMissingRegion(missingRegion)
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)
            viewModel.onDownloadItemClick(downloadItem, DownloadingState.Downloading)
            Assertions.assertEquals(
                DownloadItemAction.Cancel(downloadItem),
                stateTurbine.expectMostRecentItem().downloadItemAction,
            )

            // When
            viewModel.cancelDownload()

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().downloadItemAction)
            verify { cancelDownloadUseCase() }
            verify { setDownloadPausedUseCase(false) }

            stateTurbine.cancel()
        }

    @Test
    fun `Given the state has an error, when the error is dismissed, then the state contains no errors`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            viewModel.setMissingRegion(missingRegion)
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)

            every { checkDownloadRestrictionsUseCase(any()) } returns ErrorType.Network

            viewModel.onDownloadItemClick(downloadItem, DownloadingState.Default)
            viewModel.onDownloadItemClick(
                downloadItem,
                DownloadingState.ErrorState.InternetConnectionError
            )

            // Given
            Assertions
                .assertEquals(ErrorType.Network, stateTurbine.expectMostRecentItem().errorType)

            // When
            viewModel.dismissError()

            // Then
            Assertions.assertNull(stateTurbine.expectMostRecentItem().errorType)
            stateTurbine.cancel()
        }


    @Test
    fun `When download action is cleared, then the downloadItemAction is null`() =
        runTest {
            every { getDownloadItemUseCase(missingRegion) } returns flowOf(Resource.Success(downloadItem))
            val viewModel = getViewModel()
            viewModel.setMissingRegion(missingRegion)
            val stateTurbine = viewModel.missingMapInfoState.testIn(this)
            viewModel.onDownloadItemClick(downloadItem, DownloadingState.ErrorState.MemoryNotEnough)
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
        private const val MAP_NAME = "Lower Silesian Voivodeship"
        private const val FILE_NAME = "Poland_dolnoslaskie.zip"

        private val missingRegion = FILE_NAME

        private val downloadItem = DownloadItem(
            name = MAP_NAME,
            description = "",
            fileName = FILE_NAME,
            size = "123 MB",
            targetSize = "144 MB",
        )
    }
}