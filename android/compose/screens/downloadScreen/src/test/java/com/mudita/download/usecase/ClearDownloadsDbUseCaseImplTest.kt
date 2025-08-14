package com.mudita.download.usecase

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
class ClearDownloadsDbUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var clearDownloadsDbUseCase: ClearDownloadsDbUseCaseImpl

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        coJustRun { downloadRepository.clearDownloadsDb() }
        clearDownloadsDbUseCase = ClearDownloadsDbUseCaseImpl(
            downloadRepository = downloadRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When clearDownloadsDbUseCase is invoked, then should call clearDownloadsDb from repository`() = runTest {
        // When
        clearDownloadsDbUseCase()

        // Then
        coVerify(exactly = 1) { downloadRepository.clearDownloadsDb() }
    }
}