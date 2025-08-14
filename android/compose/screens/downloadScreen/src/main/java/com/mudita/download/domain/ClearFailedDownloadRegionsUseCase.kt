package com.mudita.download.domain

import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadRegions
import javax.inject.Inject

class ClearFailedDownloadRegionsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    operator fun invoke(downloadRegions: DownloadRegions) {
        downloadRepository.clearFailedDownloadRegions(downloadRegions)
    }
}
