package com.mudita.download.usecase

import com.mudita.download.domain.GetDownloadsQueueUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadQueueModel
import javax.inject.Inject

class GetDownloadsQueueUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : GetDownloadsQueueUseCase {

    override suspend fun invoke(): Result<List<DownloadQueueModel>> {
        return downloadRepository.getDownloadsQueue()
    }

}