package com.mudita.download.repository.models

data class ResourceGroup @JvmOverloads constructor(
    val name: String,
    val groups: List<ResourceGroup> = emptyList(),
    val individualDownloadItems: List<DownloadItem> = emptyList(),
    val groupDownloadItem: DownloadItem? = null,
    val parents: List<String> = emptyList()
) : Resource {
    val allDownloadItems: List<DownloadItem> = buildList {
        addAll(individualDownloadItems)
        groups.forEach { addAll(it.allDownloadItems) }
    }

    val isSingleCountry: Boolean =
        allDownloadItems.map { it.fileName.substringBefore("_") }.distinct().size == 1 || groupDownloadItem != null

    override val id: String
        get() = name
}
