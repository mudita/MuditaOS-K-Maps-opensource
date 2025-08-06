package com.mudita.download.usecase

import com.mudita.download.repository.DownloadRepository
import javax.inject.Inject

class SetDownloadPausedUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    operator fun invoke(paused: Boolean) {
        downloadRepository.setDownloadPaused(paused)
    }
}
