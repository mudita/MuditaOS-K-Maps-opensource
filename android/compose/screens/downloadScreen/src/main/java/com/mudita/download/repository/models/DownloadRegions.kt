package com.mudita.download.repository.models

data class DownloadRegions(
    override val id: String,
    val regions: List<DownloadItem>
) : Resource, Downloadable {

    val totalSize: Double = regions.getTotalSize()

    var currentDownloadingProvince: DownloadItem? = null
}
