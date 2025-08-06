package com.mudita.download.domain

import com.mudita.download.repository.models.DownloadRegions

interface DeleteDownloadFromDbUseCase {

    suspend operator fun invoke(filename: String)

    suspend operator fun invoke(downloadRegions: DownloadRegions) {
        downloadRegions.regions.forEach { invoke(it.fileName) }
    }
}