package com.mudita.download.usecase

import com.mudita.download.domain.AddDownloadToDbUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadQueueModel
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
class AddDownloadToDbUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var addDownloadToDbUseCase: AddDownloadToDbUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        coJustRun { downloadRepository.addToDownloadsDb(any()) }
        addDownloadToDbUseCase = AddDownloadToDbUseCaseImpl(
            downloadRepository = downloadRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given DownloadQueueModel, when addDownloadToDbUseCase is invoked, then should call addToDownloadsDb from repository`() = runTest {
        // Given
        val model = DownloadQueueModel(
            name = "Wielkopolskie",
            description = "163 MB",
            filename = "Poland_wielkopolskie_europe.zip",
            size = "163 MB",
            targetSize = "273 MB",
            parentName = "Poland",
        )

        // When
        addDownloadToDbUseCase(model)

        // Then
        coVerify(exactly = 1) { downloadRepository.addToDownloadsDb(model) }
    }
}