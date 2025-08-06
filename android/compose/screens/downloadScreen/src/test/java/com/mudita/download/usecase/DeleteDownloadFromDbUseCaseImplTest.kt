package com.mudita.download.usecase

import com.mudita.download.domain.DeleteDownloadFromDbUseCase
import com.mudita.download.repository.DownloadRepository
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
class DeleteDownloadFromDbUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var deleteDownloadFromDbUseCase: DeleteDownloadFromDbUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        coJustRun { downloadRepository.deleteFromDownloadsDb(any()) }
        deleteDownloadFromDbUseCase = DeleteDownloadFromDbUseCaseImpl(
            downloadRepository = downloadRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given file name, when deleteDownloadFromDbUseCase is invoked, then should call deleteFromDownloadsDb from repository`() = runTest {
        // Given
        val fileName = "Poland_wielkopolskie_europe.zip"

        // When
        deleteDownloadFromDbUseCase(fileName)

        // Then
        coVerify(exactly = 1) { downloadRepository.deleteFromDownloadsDb(fileName) }
    }
}