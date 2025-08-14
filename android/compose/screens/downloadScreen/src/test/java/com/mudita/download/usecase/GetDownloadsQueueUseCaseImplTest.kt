package com.mudita.download.usecase

import com.mudita.download.domain.GetDownloadsQueueUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadQueueModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class GetDownloadsQueueUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var getDownloadsQueueUseCase: GetDownloadsQueueUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        getDownloadsQueueUseCase = GetDownloadsQueueUseCaseImpl(
            downloadRepository = downloadRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given download queue items successfully fetched from repository, when getDownloadsQueueUseCase is invoked, then should return this list`() = runTest {
        // Given
        val model = DownloadQueueModel(
            name = "Wielkopolskie",
            description = "163 MB",
            filename = "Poland_wielkopolskie_europe.zip",
            size = "163 MB",
            targetSize = "273 MB",
            parentName = "Poland",
        )
        val downloadQueueSuccess = Result.success(listOf(model))
        coEvery { downloadRepository.getDownloadsQueue() } returns downloadQueueSuccess

        // When
        val result = getDownloadsQueueUseCase()

        // Then
        assertEquals(downloadQueueSuccess, result)
        coVerify(exactly = 1) { downloadRepository.getDownloadsQueue() }
    }

    @Test
    fun `Given download queue items fetch from repository failed, when getDownloadsQueueUseCase is invoked, then should return failed result`() = runTest {
        // Given
        val downloadQueueFailure = Result.failure<List<DownloadQueueModel>>(IOException())
        coEvery { downloadRepository.getDownloadsQueue() } returns downloadQueueFailure

        // When
        val result = getDownloadsQueueUseCase()

        // Then
        assertEquals(downloadQueueFailure, result)
        coVerify(exactly = 1) { downloadRepository.getDownloadsQueue() }
    }
}