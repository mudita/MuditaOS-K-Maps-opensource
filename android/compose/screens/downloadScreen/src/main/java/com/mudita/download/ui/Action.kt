package com.mudita.download.ui

import com.mudita.download.repository.models.DownloadItem

sealed class Action {
    data class Download(val items: List<DownloadItem>) : Action() {
        constructor(item: DownloadItem) : this(listOf(item))
    }

    data class CancelDownload(val item: DownloadItem) : Action()
}
