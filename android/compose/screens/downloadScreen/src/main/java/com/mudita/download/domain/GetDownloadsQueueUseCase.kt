package com.mudita.download.domain

import com.mudita.download.repository.models.DownloadQueueModel

interface GetDownloadsQueueUseCase {

    suspend operator fun invoke(): Result<List<DownloadQueueModel>>

}