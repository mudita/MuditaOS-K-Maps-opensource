package com.mudita.download.usecase

import com.mudita.download.domain.RemoveDownloadEntryUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class RemoveDownloadEntryUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var removeDownloadEntryUseCase: RemoveDownloadEntryUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        coJustRun { downloadRepository.removeDownloadEntry(any()) }
        removeDownloadEntryUseCase = RemoveDownloadEntryUseCaseImpl(
            downloadRepository = downloadRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given download item, when removeDownloadEntryUseCase is invoked, then should call removeDownloadEntry from repository`() = runTest {
        // Given
        val downloadItem = DownloadItem(
            name = "Mazowieckie",
            description = "297 MB",
            fileName = "Poland_mazowieckie_europe.zip",
            size = "297 MB",
            targetSize = "479 MB",
            downloaded = false
        )

        // When
        removeDownloadEntryUseCase(downloadItem)

        // Then
        coVerify(exactly = 1) { downloadRepository.removeDownloadEntry(downloadItem) }
    }
}