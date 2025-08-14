package com.mudita.download.usecase

import com.mudita.download.domain.ClearDownloadsDbUseCase
import com.mudita.download.repository.DownloadRepository
import javax.inject.Inject

class ClearDownloadsDbUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ClearDownloadsDbUseCase {

    override suspend fun invoke() {
        downloadRepository.clearDownloadsDb()
    }

}