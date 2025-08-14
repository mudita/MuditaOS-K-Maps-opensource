package com.mudita.download.exception

import com.mudita.download.repository.models.DownloadItem

class InsufficientMemoryException(
    val downloadItem: DownloadItem
) : Exception("Insufficient memory to download ${downloadItem.id}")
