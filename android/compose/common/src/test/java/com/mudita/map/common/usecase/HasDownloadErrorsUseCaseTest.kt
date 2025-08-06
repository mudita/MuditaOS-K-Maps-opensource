package com.mudita.map.common.usecase

import app.cash.turbine.test
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.download.GetDownloadingStateUseCase
import com.mudita.map.common.download.HasDownloadErrorsUseCase
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class HasDownloadErrorsUseCaseTest {

    @MockK
    private lateinit var getDownloadingStateUseCase: GetDownloadingStateUseCase

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var hasDownloadErrorsUseCase: HasDownloadErrorsUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        hasDownloadErrorsUseCase = HasDownloadErrorsUseCase(
            getDownloadingStateUseCase = getDownloadingStateUseCase,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given one item has download error, when hasDownloadErrorsUseCase is invoked, should return true`() = runTest {
        // Given
        val items = mapOf(
            "1" to DownloadingState.Queued,
            "2" to DownloadingState.ErrorState.InternetConnectionError,
            "3" to DownloadingState.Queued
        )
        every { getDownloadingStateUseCase.invoke() } returns MutableStateFlow(items)

        // When
        hasDownloadErrorsUseCase.invoke().test {
            // Then
            assertTrue(expectMostRecentItem())
        }
    }

    @Test
    fun `Given more than one item has download error, when hasDownloadErrorsUseCase is invoked, should return true`() = runTest {
        // Given
        val items = mapOf(
            "1" to DownloadingState.Queued,
            "2" to DownloadingState.ErrorState.InternetConnectionError,
            "3" to DownloadingState.Queued,
            "4" to DownloadingState.ErrorState.InternetConnectionRetryFailed,
        )
        every { getDownloadingStateUseCase.invoke() } returns MutableStateFlow(items)

        // When
        hasDownloadErrorsUseCase.invoke().test {
            // Then
            assertTrue(expectMostRecentItem())
        }
    }

    @Test
    fun `Given no item has download error, when hasDownloadErrorsUseCase is invoked, should return false`() = runTest {
        // Given
        val items = mapOf(
            "1" to DownloadingState.Queued,
            "2" to DownloadingState.Downloading,
            "3" to DownloadingState.Default,
            "4" to DownloadingState.PreparingMap,
        )
        every { getDownloadingStateUseCase.invoke() } returns MutableStateFlow(items)

        // When
        hasDownloadErrorsUseCase.invoke().test {
            // Then
            assertFalse(expectMostRecentItem())
        }
    }
}