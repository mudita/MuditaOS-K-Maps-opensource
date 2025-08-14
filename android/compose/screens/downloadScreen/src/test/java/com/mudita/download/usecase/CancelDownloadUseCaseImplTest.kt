package com.mudita.download.usecase

import com.mudita.download.domain.CancelDownloadUseCase
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
class CancelDownloadUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var cancelDownloadUseCase: CancelDownloadUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        coJustRun { downloadRepository.cancelDownload() }
        cancelDownloadUseCase = CancelDownloadUseCaseImpl(
            downloadRepository = downloadRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When cancelDownloadUseCase is invoked, then should call cancelDownload from repository`() = runTest {
        // When
        cancelDownloadUseCase()

        // Then
        coVerify(exactly = 1) { downloadRepository.cancelDownload() }
    }
}