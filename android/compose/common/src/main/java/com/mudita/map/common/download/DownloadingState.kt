package com.mudita.map.common.download

import android.content.Context
import com.mudita.maps.frontitude.R

sealed class DownloadingState {
    val isError: Boolean get() = this is ErrorState

    val isMemoryOrIoError: Boolean get() =
        this is ErrorState.IoError || this is ErrorState.MemoryNotEnough

    val isInternetError: Boolean get() =
        this is ErrorState.InternetConnectionError || this is ErrorState.InternetConnectionRetryFailed

    val isCancelable: Boolean get() =
        this is Downloading || this is Queued || this is PreparingMap

    object Default : DownloadingState()
    object Queued : DownloadingState()
    object Downloading : DownloadingState()
    object PreparingMap : DownloadingState()

    sealed class ErrorState : DownloadingState() {
        object InternetConnectionError : ErrorState()
        object InternetConnectionRetryFailed : ErrorState()
        object IoError : ErrorState()
        object MemoryNotEnough : ErrorState()
    }
}

fun DownloadingState.getDownloadItemStateDescription(
    context: Context,
    downloadProgress: DownloadProgress,
): String = when (this) {
    DownloadingState.Default,
    DownloadingState.Downloading ->
        downloadProgress.formattedRemainingTime(context)

    DownloadingState.ErrorState.InternetConnectionError ->
        context.getString(R.string.common_status_tryingtoreconnect)

    DownloadingState.ErrorState.InternetConnectionRetryFailed ->
        context.getString(R.string.common_label_downloadinterrupted)

    DownloadingState.ErrorState.IoError,
    DownloadingState.ErrorState.MemoryNotEnough ->
        context.getString(R.string.common_label_downloadfailed)

    DownloadingState.PreparingMap ->
        context.getString(R.string.maps_managingmaps_status_preparingmap)

    DownloadingState.Queued ->
        context.getString(R.string.common_status_downloadqueued)
}
