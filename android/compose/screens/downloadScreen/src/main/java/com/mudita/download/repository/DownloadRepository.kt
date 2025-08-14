package com.mudita.download.repository

import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadQueueModel
import com.mudita.download.repository.models.DownloadRegions
import com.mudita.download.repository.models.Downloadable
import com.mudita.download.repository.models.MapFile
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.maps.data.api.Resource
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {

    fun getIndexesList(): Flow<Resource<ResourceGroup>>

    fun downloadMap(map: Downloadable): Flow<Resource<Unit>>

    fun cancelDownload()

    fun skipDownload()

    fun setDownloadPaused(paused: Boolean)

    fun getFailedDownloadRegions(downloadItem: DownloadItem): DownloadRegions?

    fun clearFailedDownloadRegions(downloadRegions: DownloadRegions)

    suspend fun removeDownloadEntry(downloadItem: DownloadItem)

    suspend fun deleteMap(map: MapFile)

    suspend fun getDownloadsQueue(): Result<List<DownloadQueueModel>>

    suspend fun deleteFromDownloadsDb(filename: String): Result<Unit>

    suspend fun addToDownloadsDb(item: DownloadQueueModel): Result<Unit>

    suspend fun clearDownloadsDb(): Result<Unit>

}