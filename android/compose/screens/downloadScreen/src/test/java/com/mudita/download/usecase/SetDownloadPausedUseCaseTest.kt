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
class SetDownloadPausedUseCaseTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var setDownloadPausedUseCase: SetDownloadPausedUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        coJustRun { downloadRepository.setDownloadPaused(any()) }
        setDownloadPausedUseCase = SetDownloadPausedUseCase(
            downloadRepository = downloadRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given paused set to false, when setDownloadPausedUseCase is invoked, then should call setDownloadPaused from repository`() = runTest {
        // Given
        val isPaused = false

        // When
        setDownloadPausedUseCase(isPaused)

        // Then
        coVerify(exactly = 1) { downloadRepository.setDownloadPaused(isPaused) }
    }

    @Test
    fun `Given paused set to true, when setDownloadPausedUseCase is invoked, then should call setDownloadPaused from repository`() = runTest {
        // Given
        val isPaused = true

        // When
        setDownloadPausedUseCase(isPaused)

        // Then
        coVerify(exactly = 1) { downloadRepository.setDownloadPaused(isPaused) }
    }
}