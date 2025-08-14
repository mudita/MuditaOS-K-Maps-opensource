package com.mudita.download.repository.models

data class DownloadQueueModel(
    val name: String,
    val description: String,
    val filename: String,
    val size: String,
    val targetSize: String,
    val parentName: String,
)
