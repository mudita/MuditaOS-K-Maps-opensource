package com.mudita.download.usecase

import com.mudita.download.domain.AddDownloadToDbUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadQueueModel
import javax.inject.Inject

class AddDownloadToDbUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : AddDownloadToDbUseCase {

    override suspend fun invoke(item: DownloadQueueModel) {
        downloadRepository.addToDownloadsDb(item)
    }

}