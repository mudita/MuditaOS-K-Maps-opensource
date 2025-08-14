package com.mudita.download.usecase

import com.mudita.download.domain.DeleteDownloadFromDbUseCase
import com.mudita.download.repository.DownloadRepository
import javax.inject.Inject

class DeleteDownloadFromDbUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : DeleteDownloadFromDbUseCase {

    override suspend fun invoke(filename: String) {
        downloadRepository.deleteFromDownloadsDb(filename)
    }
}