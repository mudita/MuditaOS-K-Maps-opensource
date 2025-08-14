package net.osmand.plus.activities.viewmodels

import app.cash.turbine.test
import com.mudita.download.domain.GetDownloadsQueueUseCase
import com.mudita.download.repository.models.DownloadQueueModel
import com.mudita.download.ui.Action
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class MapActivityViewModelTest {

    @MockK
    lateinit var getDownloadsQueueUseCase: GetDownloadsQueueUseCase

    private lateinit var viewModel: MapActivityViewModel

    @BeforeEach
    fun setUp() {
        viewModel = MapActivityViewModel(getDownloadsQueueUseCase)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given GetDownloadsQueueUseCase fails, then downloadAction doesnt emit anything`() = runTest {
        // Given
        coEvery { getDownloadsQueueUseCase() } returns Result.failure(Exception(""))

        // When
        viewModel.downloadAction().test {
            // Then
            awaitComplete()
        }
    }

    @Test
    fun `Given GetDownloadsQueueUseCase succeeded with an empty list, then downloadAction doesnt emit anything`() = runTest {
        // Given
        coEvery { getDownloadsQueueUseCase() } returns Result.success(emptyList())

        // When
        viewModel.downloadAction().test {
            // Then
            awaitComplete()
        }
    }

    @Test
    fun `Given GetDownloadsQueueUseCase succeeded with a non-empty list, then downloadAction emits an action`() = runTest {
        // Given
        coEvery { getDownloadsQueueUseCase() } returns Result.success(downloadQueueModels)

        // When
        viewModel.downloadAction().test {
            // Then
            Assertions.assertInstanceOf(Action.Download::class.java, awaitItem())
            awaitComplete()
        }
    }

    private companion object {

        private val downloadQueueModels = listOf(DownloadQueueModel("", "", "", "", "", ""))
    }
}
