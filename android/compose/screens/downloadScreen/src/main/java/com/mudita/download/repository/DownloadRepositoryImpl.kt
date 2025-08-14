package com.mudita.download.repository

import com.mudita.download.domain.DownloadingStatePublisher
import com.mudita.download.exception.InsufficientMemoryException
import com.mudita.download.repository.mappers.createResource
import com.mudita.download.repository.mappers.toEntity
import com.mudita.download.repository.mappers.toQueueModels
import com.mudita.download.repository.models.DownloadEntry
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadQueueModel
import com.mudita.download.repository.models.DownloadRegions
import com.mudita.download.repository.models.Downloadable
import com.mudita.download.repository.models.LocalIndex
import com.mudita.download.repository.models.MapFile
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.download.repository.models.getDownloadQueue
import com.mudita.download.repository.models.getDownloadedSize
import com.mudita.download.repository.models.getFullDownloadSize
import com.mudita.download.repository.utils.DownloadManager
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkType
import com.mudita.maps.data.api.DownloadApiService
import com.mudita.maps.data.api.Resource
import com.mudita.maps.data.db.dao.DownloadsDao
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.osmand.IndexConstants
import retrofit2.HttpException

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadApiService: DownloadApiService,
    private val downloadsDao: DownloadsDao,
    private val networkManager: NetworkManager,
    private val memoryManager: MemoryManager,
    private val downloadingStatePublisher: DownloadingStatePublisher,
) : DownloadRepository {

    private val indexesList: MutableStateFlow<Resource<ResourceGroup>> =
        MutableStateFlow(Resource.Loading())

    private val downloadsPaused = MutableStateFlow(false)

    private val failedDownloadRegions = mutableListOf<DownloadRegions>()

    override suspend fun getDownloadsQueue() = runCatching {
        withContext(Dispatchers.IO) {
            downloadsDao.getDownloadsQueue().toQueueModels()
        }
    }

    override suspend fun deleteFromDownloadsDb(filename: String) = runCatching {
        downloadsDao.deleteFromDownloads(filename)
    }

    override suspend fun addToDownloadsDb(item: DownloadQueueModel) = runCatching {
        downloadsDao.addDownload(item.toEntity())
    }

    override suspend fun clearDownloadsDb() = runCatching {
        downloadsDao.clearDownloadsQueue()
    }

    override fun cancelDownload() {
        DownloadManager.downloadCanceled = true
    }

    override fun skipDownload() {
        DownloadManager.downloadSkipped = true
    }

    override fun setDownloadPaused(paused: Boolean) {
        downloadsPaused.value = paused
    }

    override fun getFailedDownloadRegions(downloadItem: DownloadItem): DownloadRegions? =
        failedDownloadRegions.find { it.regions.contains(downloadItem) }

    override fun clearFailedDownloadRegions(downloadRegions: DownloadRegions) {
        failedDownloadRegions.remove(downloadRegions)
    }

    override suspend fun deleteMap(map: MapFile) {
        when (map) {
            is DownloadItem -> {
                createDownloadEntry(map).delete()
                deleteFromDownloadsDb(map.fileName)
            }

            is LocalIndex -> map.file.delete()
        }
    }

    override fun downloadMap(map: Downloadable): Flow<Resource<Unit>> {
        DownloadManager.downloadCanceled = false
        (map as? DownloadRegions)?.also(::clearFailedDownloadRegions)
        return flow {
            var connectionError = false
            emit(Resource.Loading())
            val downloadItemsQueue = map.getDownloadQueue()
            val fullSize = map.getFullDownloadSize()
            var skipped = 0
            var filesDownloadProgress = map.getDownloadedSize()
            downloadItemsQueue.forEach {
                downloadingStatePublisher.publishDownloadingState(it, DownloadingState.Queued)
            }

            for (file in downloadItemsQueue) {
                val downloadEntry = createDownloadEntry(file)
                if (map is DownloadRegions && connectionError) {
                    break
                }
                if (DownloadManager.isProvinceSkipped(file)) continue
                if (map is DownloadRegions) map.currentDownloadingProvince = file
                try {
                    if (!isMemoryAvailable()) {
                        onInsufficientMemory(map)
                        throw InsufficientMemoryException(file)
                    }
                    downloadingStatePublisher.publishDownloadingState(file, DownloadingState.Downloading)
                    val destinationFile = downloadEntry.fileToDownload
                    val skippedBytes = if (destinationFile.exists()) destinationFile.length() else 0
                    val response = downloadApiService.downloadMap(downloadEntry.urlToDownload, "bytes=$skippedBytes-")
                    val totalBytes = response.contentLength() + skippedBytes
                    filesDownloadProgress += skippedBytes / totalBytes.toDouble() * file.getSizeAsMB()
                    response.byteStream().use { inputStream ->
                        FileOutputStream(destinationFile, true).use { outputStream ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var progressBytes = 0L
                            var bytes = inputStream.read(buffer)
                            var currentProgress = 0.0
                            while (bytes >= 0 && !DownloadManager.downloadCanceled && !DownloadManager.downloadSkipped) {
                                if (!checkInternetConnection()) break
                                awaitDownloadResumed()
                                outputStream.write(buffer, 0, bytes)
                                progressBytes += bytes
                                bytes = inputStream.read(buffer)
                                val progress = progressBytes * 100.0 / totalBytes
                                if (progress > currentProgress) {
                                    filesDownloadProgress -= currentProgress * file.getSizeAsMB() / 100
                                    currentProgress = progress
                                    filesDownloadProgress += currentProgress * file.getSizeAsMB() / 100
                                }
                                if (skipped < (DownloadManager.skippedProvinces[map]?.size ?: 0)) {
                                    skipped++
                                    filesDownloadProgress += DownloadManager.skippedProvinces[map]?.last()?.getSizeAsMB() ?: 0.0
                                }
                                emit(Resource.Loading(filesDownloadProgress / fullSize))
                            }
                            if (DownloadManager.downloadSkipped) {
                                if (DownloadManager.skippedProvinces[map] == null) DownloadManager.skippedProvinces[map] = mutableListOf()
                                DownloadManager.skippedProvinces[map]?.add(file)
                                skipped++
                                filesDownloadProgress -= currentProgress * file.getSizeAsMB() / 100
                                filesDownloadProgress += file.getSizeAsMB()
                            }
                        }
                    }
                    if (!checkInternetConnection()) {
                        throw UnknownHostException()
                    } else if (DownloadManager.downloadCanceled) {
                        deleteFromDownloadsDb(downloadEntry.urlToDownload)
                        destinationFile.delete()
                        emit(Resource.Success(Unit))
                    } else if (DownloadManager.downloadSkipped) {
                        deleteFromDownloadsDb(downloadEntry.urlToDownload)
                        destinationFile.delete()
                    } else {
                        if (downloadEntry.tempTargetFile.exists()) downloadEntry.tempTargetFile.delete()
                        emit(Resource.Loading(filesDownloadProgress / fullSize, true))
                        val inputStream = destinationFile.inputStream()
                        val zipIn = ZipInputStream(inputStream)
                        var entry: ZipEntry?
                        while (zipIn.nextEntry.also { entry = it } != null) {
                            if (entry?.isDirectory == true || entry?.name?.endsWith(IndexConstants.GEN_LOG_EXT) == true) {
                                continue
                            }
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                            val targetFileOutputStream = downloadEntry.tempTargetFile.outputStream()
                            var bytesRead: Int
                            while (zipIn.read(buffer, 0, DEFAULT_BUFFER_SIZE)
                                    .also { bytesRead = it } != -1
                            ) {
                                awaitDownloadResumed()
                                if (skipped < (DownloadManager.skippedProvinces[map]?.size ?: 0)) {
                                    skipped++
                                    filesDownloadProgress += DownloadManager.skippedProvinces[map]?.last()?.getSizeAsMB() ?: 0.0
                                }
                                if (DownloadManager.downloadCanceled) break
                                targetFileOutputStream.write(buffer, 0, bytesRead)
                            }
                            targetFileOutputStream.close()
                            if (DownloadManager.downloadCanceled) break
                        }
                        if (DownloadManager.downloadCanceled) {
                            downloadEntry.tempTargetFile.delete()
                        } else {
                            file.downloaded = true
                        }
                        deleteFromDownloadsDb(downloadEntry.urlToDownload)
                        downloadEntry.targetFile.delete()
                        downloadEntry.tempTargetFile.renameTo(downloadEntry.targetFile)
                        destinationFile.delete()
                        emit(Resource.Success(Unit))
                    }
                } catch (throwable: Throwable) {
                    // Needed for handling an unavailable network connection.
                    DownloadManager.isDownloading.value = false
                    when (throwable) {
                        is UnknownHostException, is SocketException, is SocketTimeoutException, is HttpException -> {
                            connectionError = true
                            emit(Resource.Error(throwable))
                        }

                        is IOException -> {
                            downloadEntry.delete()
                            val exception =
                                if (throwable.message?.contains(NOT_ENOUGH_SPACE_CODE) == true) {
                                    onInsufficientMemory(map)
                                    InsufficientMemoryException(file)
                                } else {
                                    throwable
                                }
                            emit(Resource.Error(exception))
                        }

                        else -> {
                            emit(Resource.Error(throwable))
                        }
                    }
                } finally {
                    if (DownloadManager.downloadCanceled) break
                    val downloadSkipped = DownloadManager.downloadSkipped
                    if (downloadSkipped) {
                        DownloadManager.downloadSkipped = false
                    }
                    if (map is DownloadRegions && connectionError.not()) {
                        val maps = map.regions.filter { !it.downloaded && !DownloadManager.isProvinceSkipped(it) }
                        map.currentDownloadingProvince = if (maps.isNotEmpty()) maps.first() else null
                        if (downloadSkipped) emit(Resource.Success(Unit))
                    }

                    DownloadManager.downloadCanceled = false
                    DownloadManager.reindexDownloadedMaps(downloadEntry.targetFile)
                }
            }
            // Handles download completion and cancellation.
            DownloadManager.isDownloading.value = false
            if (!connectionError && !failedDownloadRegions.contains(map)) {
                DownloadManager.skippedProvinces[map]?.clear()
            }
        }
    }

    private suspend fun awaitDownloadResumed() {
        downloadsPaused.filter { !it }.first()
    }

    private fun onInsufficientMemory(downloadable: Downloadable) {
        DownloadManager.downloadCanceled = true
        DownloadManager.downloadingMap = null
        DownloadManager.isDownloading.value = false
        if (downloadable is DownloadRegions) {
            failedDownloadRegions.add(downloadable)
        }
    }

    private suspend fun getResourceGroups(): Resource<ResourceGroup> {
        val response = downloadApiService.getIndexesList()
        return if (response.isSuccessful) {
            response.body()?.let { maps ->
                val resourceGroup = maps.createResource(DownloadManager.appRegions)
                Resource.Success(resourceGroup)
            } ?: Resource.Error(IOException("Invalid body"))
        } else {
            Resource.Error(Exception(response.errorBody()?.charStream()?.readText()))
        }
    }

    override fun getIndexesList(): Flow<Resource<ResourceGroup>> = flow {
        val isCurrentResourceSuccess = indexesList.value is Resource.Success
        if (isCurrentResourceSuccess) emit(indexesList.value)
        val resourceGroups = try {
            getResourceGroups()
        } catch (e: Exception) {
            Resource.Error<ResourceGroup>(e).also { if (!isCurrentResourceSuccess) emit(it) }
        }
        if (resourceGroups is Resource.Success || !isCurrentResourceSuccess) {
            indexesList.value = resourceGroups
        }
        emitAll(indexesList)
    }

    override suspend fun removeDownloadEntry(downloadItem: DownloadItem) {
        val downloadEntry = createDownloadEntry(item = downloadItem)
        downloadEntry.targetFile.delete()
        downloadEntry.fileToDownload.delete()
    }

    private fun checkInternetConnection(): Boolean = when {
        !networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) -> false
        !networkManager.isNetworkReachable(NetworkType.ALL) -> false
        else -> true
    }

    private fun createDownloadEntry(item: DownloadItem): DownloadEntry {
        val parent = getDownloadFolder(item)
        parent?.mkdirs()
        return DownloadEntry(
            urlToDownload = item.fileName,
            targetFile = getTargetFile(item),
            fileToDownload = DownloadManager.getFileWithDownloadExtension(getTargetFile(item))
        )
    }

    private fun getTargetFile(item: DownloadItem): File {
        return File(getDownloadFolder(item), item.getBasename().plus(".obf"))
    }

    private fun getDownloadFolder(item: DownloadItem): File? {
        return if (item.fileName.endsWith(IndexConstants.SQLITE_EXT)) {
            DownloadManager.getAppPath?.invoke(IndexConstants.TILES_INDEX_DIR)
        } else DownloadManager.getAppPath?.invoke(IndexConstants.MAPS_PATH)
    }

    private fun isMemoryAvailable() = memoryManager
        .hasEnoughSpace(DownloadManager.getDownloadingMapSize())

    companion object {
        private const val NOT_ENOUGH_SPACE_CODE = "ENOSPC"
    }
}