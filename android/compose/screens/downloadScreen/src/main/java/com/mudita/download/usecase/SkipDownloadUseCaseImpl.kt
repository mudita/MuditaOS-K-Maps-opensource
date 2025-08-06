package com.mudita.download.usecase

import com.mudita.download.domain.SkipDownloadUseCase
import com.mudita.download.repository.DownloadRepository
import javax.inject.Inject

class SkipDownloadUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : SkipDownloadUseCase {

    override fun invoke() {
        downloadRepository.skipDownload()
    }

}