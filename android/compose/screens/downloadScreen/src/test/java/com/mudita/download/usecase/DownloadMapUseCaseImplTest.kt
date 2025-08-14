package com.mudita.download.usecase

import app.cash.turbine.test
import com.mudita.download.domain.DownloadMapUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import com.mudita.maps.data.api.Resource
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class DownloadMapUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var downloadMapUseCase: DownloadMapUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        downloadMapUseCase = DownloadMapUseCaseImpl(
            downloadRepository = downloadRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given map item, when downloadMapUseCase is invoked, then should call downloadMap from repository`() = runTest {
        // Given
        val map = DownloadItem(
            name = "Mazowieckie",
            description = "297 MB",
            fileName = "Poland_mazowieckie_europe.zip",
            size = "297 MB",
            targetSize = "479 MB",
            downloaded = false
        )
        every { downloadRepository.downloadMap(any()) } returns flowOf(Resource.Success(Unit))

        // When
        downloadMapUseCase(map).test {
            // Then
            assertEquals(Resource.Success(Unit), awaitItem())
            awaitComplete()
        }
        verify(exactly = 1) { downloadRepository.downloadMap(map) }
    }
}