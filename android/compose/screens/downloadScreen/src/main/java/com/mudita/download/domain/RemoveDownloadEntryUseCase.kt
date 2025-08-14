package com.mudita.download.domain

import com.mudita.download.repository.models.DownloadItem

interface RemoveDownloadEntryUseCase {

    suspend operator fun invoke(downloadItem: DownloadItem)

}