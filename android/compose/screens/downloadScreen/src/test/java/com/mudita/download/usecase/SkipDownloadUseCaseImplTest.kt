package com.mudita.download.usecase

import com.mudita.download.domain.SkipDownloadUseCase
import com.mudita.download.repository.DownloadRepository
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SkipDownloadUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    private lateinit var skipDownloadUseCase: SkipDownloadUseCase

    @BeforeEach
    fun setup() {
        justRun { downloadRepository.skipDownload() }
        skipDownloadUseCase = SkipDownloadUseCaseImpl(
            downloadRepository = downloadRepository,
        )
    }

    @Test
    fun `When setDownloadPausedUseCase is invoked, then should call setDownloadPaused from repository`() {
        // When
        skipDownloadUseCase()

        // Then
        coVerify(exactly = 1) { downloadRepository.skipDownload() }
    }
}