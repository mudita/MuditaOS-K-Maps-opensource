package com.mudita.download.repository.utils

import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadRegions
import com.mudita.download.repository.models.Downloadable
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import net.osmand.IndexConstants
import net.osmand.map.OsmandRegions

object DownloadManager {

    private const val HALF_MINUTE_IN_SECONDS = 30
    private const val MINUTE_IN_SECONDS = 60
    private const val DAY_IN_SECONDS = 86400
    private const val HOUR_IN_SECONDS = 3600

    var appRegions = OsmandRegions()
    val downloadQueue = ArrayDeque<Downloadable>()
    val isDownloading = MutableStateFlow(false)
    var downloadCanceled = false
    var downloadSkipped = false
    val skippedProvinces = mutableMapOf<Downloadable, MutableList<DownloadItem>>()
    var downloadingMap: Downloadable? = null
    var getAppPath: ((indexConstant: String) -> File)? = null
    var downloadDirectories = emptyList<String>()
    var reindexMaps: ((File) -> Unit)? = null

    fun getQueuedRegions(): List<DownloadItem> = mutableListOf<DownloadItem>().apply {
        val map = downloadingMap
        if (map is DownloadRegions) {
            val queuedMaps = map.regions.filter { !it.downloaded && !isProvinceSkipped(it) }
            addAll(queuedMaps)
            map.currentDownloadingProvince?.let { remove(it) }
        }
    }

    fun reindexDownloadedMaps(file: File) {
        reindexMaps?.invoke(file)
    }

    fun isProvinceSkipped(province: DownloadItem): Boolean {
        skippedProvinces.values.forEach {
            if (it.contains(province)) return true
        }
        return false
    }

    fun getFileWithDownloadExtension(original: File): File {
        val folder = original.parentFile
        val fileName = original.name + IndexConstants.DOWNLOAD_EXT
        return File(folder, fileName)
    }

    fun getDownloadingMapSize() : Double {
        return when(val item = downloadingMap){
            is DownloadItem -> item.getFullSize()
            is DownloadRegions -> item.currentDownloadingProvince?.getFullSize() ?: 0.0
            else -> 0.0
        }
    }

    fun isMapRegionQueued(item: DownloadItem): Boolean {
        val queuedRegions = downloadQueue.filterIsInstance<DownloadRegions>()
        return queuedRegions.any { it.regions.contains(item) }
    }

    fun isMapRegionDownloading(item: DownloadItem): Boolean =
        (downloadingMap as? DownloadRegions)?.regions?.contains(item) ?: false
}