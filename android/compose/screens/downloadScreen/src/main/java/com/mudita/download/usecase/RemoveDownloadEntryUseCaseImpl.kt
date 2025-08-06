package com.mudita.download.usecase

import com.mudita.download.domain.RemoveDownloadEntryUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import javax.inject.Inject

class RemoveDownloadEntryUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : RemoveDownloadEntryUseCase {

    override suspend fun invoke(downloadItem: DownloadItem) {
        downloadRepository.removeDownloadEntry(downloadItem)
    }

}