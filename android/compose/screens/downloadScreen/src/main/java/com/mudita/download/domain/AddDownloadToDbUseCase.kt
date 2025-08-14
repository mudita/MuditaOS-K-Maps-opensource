package com.mudita.download.domain

import com.mudita.download.repository.models.DownloadQueueModel

interface AddDownloadToDbUseCase {

    suspend operator fun invoke(item: DownloadQueueModel)

}