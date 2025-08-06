package com.mudita.download.ui

import com.mudita.download.repository.models.Downloadable
import com.mudita.download.repository.models.LocalIndex
import com.mudita.download.repository.models.MapFile
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState

interface DownloadItemInfo {
    val downloadingStateMap: Map<String, DownloadingState>
    val downloadProgressMap: Map<String, DownloadProgress>
    val downloadedItems: Set<MapFile>

    fun isDownloading(item: Downloadable): Boolean =
        getDownloadingState(item) == DownloadingState.Downloading

    fun isDownloading(index: LocalIndex): Boolean =
        getDownloadingState(index.getBasename()) == DownloadingState.Downloading

    fun isQueued(item: Downloadable): Boolean =
        getDownloadingState(item) == DownloadingState.Queued

    fun isQueued(index: LocalIndex): Boolean =
        getDownloadingState(index.getBasename()) == DownloadingState.Queued

    fun getDownloadingState(downloadable: Downloadable): DownloadingState =
        getDownloadingState(downloadable.id)

    fun getDownloadingState(index: LocalIndex): DownloadingState =
        getDownloadingState(index.getBasename())

    fun getDownloadingState(id: String): DownloadingState =
        downloadingStateMap[id] ?: DownloadingState.Default

    fun getDownloadProgress(downloadable: Downloadable): DownloadProgress =
        getDownloadProgress(downloadable.id)

    fun getDownloadProgress(index: LocalIndex): DownloadProgress =
        getDownloadProgress(index.getBasename())

    private fun getDownloadProgress(id: String): DownloadProgress =
        downloadProgressMap[id] ?: DownloadProgress.Empty
}
