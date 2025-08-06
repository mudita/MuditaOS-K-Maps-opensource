package com.mudita.download.repository.models

import com.mudita.download.repository.utils.CHARS_LENGTH_IN_SIZE_TEXT
import com.mudita.download.repository.utils.DownloadManager

data class DownloadItem(
    var name: String = "",
    var description: String,
    val fileName: String,
    val size: String,
    val targetSize: String,
    val timestamp: Long? = null,
    var downloaded: Boolean = false,
    val parentName: String = "",
) : Resource, Downloadable, MapFile {

    override val id: String = "$parentNameTag${getBasename()}"

    val parentNameTag: String get() = "[$parentName]"

    fun getSizeAsMB(): Double {
        return size.dropLast(CHARS_LENGTH_IN_SIZE_TEXT).toDouble()
    }

    fun getTargetSizeAsMB(): Double {
        return targetSize.dropLast(CHARS_LENGTH_IN_SIZE_TEXT).toDouble()
    }

    fun getFullSize() = getTargetSizeAsMB() + getSizeAsMB()

    override fun getBasename(): String {
        return fileName.substringBeforeLast(".")
    }
}

internal fun List<DownloadItem>.getTotalSize(): Double = sumOf { it.getSizeAsMB() }

internal fun List<DownloadItem>.getDownloadedSize(): Double = sumOf {
    if (it.downloaded || DownloadManager.isProvinceSkipped(it)) {
        it.getSizeAsMB()
    } else {
        0.0
    }
}
