package com.mudita.download.domain

import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadRegions
import javax.inject.Inject

class GetFailedDownloadRegionsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    operator fun invoke(downloadItem: DownloadItem): DownloadRegions? =
        downloadRepository.getFailedDownloadRegions(downloadItem)
}
