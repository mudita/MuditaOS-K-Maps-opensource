package com.mudita.download.utils

import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadRegions
import com.mudita.download.repository.models.LocalIndex
import com.mudita.download.repository.models.MapFile

private const val SYMBOLS_AFTER_MAP_NAME = 10

fun Set<MapFile>.isItemDownloaded(item: DownloadItem): Boolean {
    val alreadyDownloaded = filterIsInstance<LocalIndex>()
    val names = alreadyDownloaded.map { it.fileName }
    for (name in names) {
        if (name.contains(item.fileName.dropLast(SYMBOLS_AFTER_MAP_NAME))) {
            return true
        }
    }
    return false
}

fun Set<MapFile>.isItemsDownloaded(regions: DownloadRegions): Boolean {
    return regions.regions.all { isItemDownloaded(it) }
}