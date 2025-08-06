package com.mudita.download.repository.models

import com.mudita.download.ui.DownloadViewModel
import java.io.File

class LocalIndex(
    val type: DownloadViewModel.LocalIndexType,
    val file: File,
    val parentName: String,
) : Comparable<LocalIndex?>, MapFile {
    var description = ""

    var isCorrupted = false
        set(corrupted) {
            field = corrupted
            if (corrupted) {
                isLoaded = false
            }
        }
    var isNotSupported = false
        set(notSupported) {
            field = notSupported
            if (notSupported) {
                isLoaded = false
            }
        }
    var updateAvailable = false
    var isLoaded = false
    val fileName: String = file.name
    val size: Int = if (!file.isDirectory) (file.length() + 512 shr 10).toInt() else 0

    val originalType: DownloadViewModel.LocalIndexType
        get() = type

    override fun getBasename(): String = fileName.substringBeforeLast(".")

    override operator fun compareTo(other: LocalIndex?): Int = fileName.compareTo(other?.fileName.orEmpty())
}