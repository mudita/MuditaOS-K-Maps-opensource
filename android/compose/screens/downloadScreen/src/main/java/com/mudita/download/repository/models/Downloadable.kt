package com.mudita.download.repository.models

import com.mudita.download.repository.utils.DownloadManager

sealed interface Downloadable {
    val id: String
}

internal fun Downloadable.getDownloadQueue(): List<DownloadItem> =
    when (this) {
        is DownloadItem -> listOf(this)
        is DownloadRegions -> buildSet {
            currentDownloadingProvince?.also(::add)
            addAll(regions)
        }.filter { !DownloadManager.isProvinceSkipped(it) && !it.downloaded }
    }

internal fun Downloadable.getFullDownloadSize(): Double =
    when (this) {
        is DownloadItem -> getSizeAsMB()
        is DownloadRegions -> totalSize
    }

internal fun Downloadable.getDownloadedSize(): Double =
    when (this) {
        is DownloadItem -> if (downloaded) getSizeAsMB() else 0.0
        is DownloadRegions -> regions.getDownloadedSize()
    }
