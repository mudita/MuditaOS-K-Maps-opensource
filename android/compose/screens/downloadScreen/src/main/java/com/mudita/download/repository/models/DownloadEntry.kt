package com.mudita.download.repository.models

import java.io.File

data class DownloadEntry(
    val urlToDownload: String,
    val targetFile: File,
    val fileToDownload: File
) {
    val tempTargetFile = File(targetFile.absolutePath + TEMP_FILE_SUFFIX)

    fun delete() {
        fileToDownload.delete()
        targetFile.delete()
        tempTargetFile.delete()
    }

    companion object {
        private const val TEMP_FILE_SUFFIX = ".temp"
    }
}
