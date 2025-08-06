package com.mudita.download.domain

import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadRegions
import com.mudita.download.repository.models.Downloadable
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.download.GetDownloadErrorNotificationUseCase
import com.mudita.map.common.download.GetDownloadProgressUseCase
import com.mudita.map.common.download.GetDownloadingStateUseCase
import com.mudita.map.common.download.OnDownloadFinishUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update

@Singleton
class DownloadingStatePublisher @Inject constructor() {

    private val downloadingState = MutableStateFlow<Map<String, DownloadingState>>(emptyMap())

    private val downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())

    private val onDownloadFinish = MutableSharedFlow<String>()

    private val errorNotification =
        MutableSharedFlow<Pair<String, DownloadingState.ErrorState?>>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val getDownloadingStateUseCase = object : GetDownloadingStateUseCase {
        override fun invoke(): StateFlow<Map<String, DownloadingState>> = downloadingState
    }

    val getDownloadProgressUseCase = object : GetDownloadProgressUseCase {
        @OptIn(FlowPreview::class)
        override fun invoke(): Flow<Map<String, DownloadProgress>> = downloadProgress
            .sample(DOWNLOAD_SPEED_SAMPLING_RATE)
    }

    val onDownloadFinishUseCase = object : OnDownloadFinishUseCase {
        override fun invoke(): Flow<String> = onDownloadFinish
    }

    val errorNotificationUseCase = object : GetDownloadErrorNotificationUseCase {
        override fun invoke(): Flow<Pair<String, DownloadingState.ErrorState?>> = errorNotification
    }

    fun publishDownloadingState(downloadable: Downloadable, state: DownloadingState) {
        downloadingState.update {
            buildMap {
                putAll(it)
                put(downloadable.id, state)
                (downloadable as? DownloadRegions)?.currentDownloadingProvince?.also { item ->
                    put(item.id, state)
                }
            }
        }
    }

    suspend fun publishDownloadingStateWithNotification(
        downloadable: Downloadable,
        state: DownloadingState,
    ) {
        publishDownloadingState(downloadable, state)
        errorNotification.emit(downloadable.id to (state as? DownloadingState.ErrorState))
    }

    fun publishDownloadProgress(
        downloadable: Downloadable,
        fraction: Double,
        downloadSpeed: Double,
    ) {
        downloadProgress.update {
            it + (downloadable.id to DownloadProgress.create(fraction, downloadSpeed))
        }
    }

    fun clearDownloadState(downloadable: Downloadable) {
        downloadProgress.update { it.remove(downloadable) }
        downloadingState.update { it.remove(downloadable) }
    }

    private fun <T> Map<String, T>.remove(downloadable: Downloadable) =
        buildMap {
            putAll(this@remove)
            remove(downloadable.id)
            when (downloadable) {
                is DownloadItem -> {
                    if (keys.none { (it as String).startsWith(downloadable.parentNameTag) }) {
                        remove(downloadable.parentName)
                    }
                }
                is DownloadRegions -> downloadable.regions.forEach { item -> remove(item.id) }
            }
        }

    suspend fun publishDownloadSuccess(downloadable: Downloadable) {
        when (downloadable) {
            is DownloadItem -> onDownloadFinish.emit(downloadable.getBasename())
            is DownloadRegions -> downloadable.currentDownloadingProvince
                ?.also { publishDownloadSuccess(it) }
        }
    }

    fun getCurrentDownloadProgress(downloadable: Downloadable): DownloadProgress =
        downloadProgress.value[downloadable.id] ?: DownloadProgress.Empty

    fun getCurrentDownloadingState(downloadable: Downloadable): DownloadingState =
        downloadingState.value[downloadable.id] ?: DownloadingState.Default

    companion object {
        private const val DOWNLOAD_SPEED_SAMPLING_RATE: Long = 1000
    }
}
