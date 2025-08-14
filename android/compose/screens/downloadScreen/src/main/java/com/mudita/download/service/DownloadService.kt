package com.mudita.download.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import com.mudita.download.R
import com.mudita.download.domain.AddDownloadToDbUseCase
import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.domain.ClearDownloadsDbUseCase
import com.mudita.download.domain.ClearFailedDownloadRegionsUseCase
import com.mudita.download.domain.DeleteDownloadFromDbUseCase
import com.mudita.download.domain.DeleteMapUseCase
import com.mudita.download.domain.DownloadMapUseCase
import com.mudita.download.domain.DownloadingStatePublisher
import com.mudita.download.domain.GetFailedDownloadRegionsUseCase
import com.mudita.download.domain.RemoveDownloadEntryUseCase
import com.mudita.download.domain.SkipDownloadUseCase
import com.mudita.download.exception.InsufficientMemoryException
import com.mudita.download.repository.mappers.toQueueModel
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadRegions
import com.mudita.download.repository.models.Downloadable
import com.mudita.download.repository.models.getDownloadedSize
import com.mudita.download.repository.utils.DownloadManager
import com.mudita.download.ui.Action
import com.mudita.download.ui.bindDownloadService
import com.mudita.map.common.collection.FixedSizeQueue
import com.mudita.map.common.collection.average
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.utils.firstNotNull
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkType
import com.mudita.maps.data.api.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadMapUseCase: DownloadMapUseCase

    @Inject
    lateinit var cancelDownloadUseCase: CancelDownloadUseCase

    @Inject
    lateinit var skipDownloadUseCase: SkipDownloadUseCase

    @Inject
    lateinit var addDownloadToDbUseCase: AddDownloadToDbUseCase

    @Inject
    lateinit var deleteDownloadFromDbUseCase: DeleteDownloadFromDbUseCase

    @Inject
    lateinit var clearDownloadsDbUseCase: ClearDownloadsDbUseCase

    @Inject
    lateinit var removeDownloadEntryUseCase: RemoveDownloadEntryUseCase

    @Inject
    lateinit var deleteMapUseCase: DeleteMapUseCase

    @Inject
    lateinit var networkManager: NetworkManager

    @Inject
    lateinit var memoryManager: MemoryManager

    @Inject
    lateinit var downloadingStatePublisher: DownloadingStatePublisher

    @Inject
    lateinit var getFailedDownloadRegionsUseCase: GetFailedDownloadRegionsUseCase

    @Inject
    lateinit var clearFailedDownloadRegionsUseCase: ClearFailedDownloadRegionsUseCase

    private val _downloadSuccessTrigger = MutableStateFlow(true)
    val downloadSuccessTrigger: StateFlow<Boolean> = _downloadSuccessTrigger.asStateFlow()

    private var isConnected = true
    private var downloadSpeed: Double = 0.0
    private var isDownloadStarting = false

    private val downloadScope = CoroutineScope(Dispatchers.IO)
    private var speedJob: Job? = null

    private var downloadJob: Job? = null
    private var isForeground = false

    private val binder: IBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun startForeground() {
        if (isForeground) return
        isForeground = false
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DownloadManager.downloadQueue.isNotEmpty()) {
            startForeground()
            startDownload()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun startDownload() {
        val isActive = downloadJob?.isActive == true
        if (isActive) return
        downloadJob = downloadScope.launch { downloadMap() }
    }

    fun tryResumeDownload(downloadable: Downloadable) {
        isConnected = checkInternetConnection()
        val originalDownloadingState = downloadingStatePublisher
            .getCurrentDownloadingState(downloadable)
        downloadScope.launch {
            downloadingStatePublisher
                .publishDownloadingStateWithNotification(downloadable, DownloadingState.Downloading)
            delay(RETRY_ERROR_DEBOUNCE)
            when {
                isConnected && !DownloadManager.isDownloading.value ->
                    addMapToDownloadQueue(downloadable)

                !isConnected -> {
                    if (originalDownloadingState ==
                        DownloadingState.ErrorState.InternetConnectionRetryFailed
                    ) {
                        downloadingStatePublisher.publishDownloadingStateWithNotification(
                            downloadable,
                            DownloadingState.ErrorState.InternetConnectionRetryFailed,
                        )
                    } else {
                        downloadingStatePublisher.publishDownloadingState(
                            downloadable,
                            DownloadingState.ErrorState.InternetConnectionError,
                        )
                    }
                }

                else ->
                    downloadingStatePublisher
                        .publishDownloadingState(downloadable, DownloadingState.Default)
            }
        }
    }

    fun addMapToDownloadQueue(map: Downloadable) {
        if (DownloadManager.downloadQueue.contains(map) || DownloadManager.downloadingMap == map) return
        startForeground()
        when (map) {
            is DownloadItem -> {
                DownloadManager.downloadQueue.addLast(map)
                downloadScope.launch {
                    addDownloadToDbUseCase(map.toQueueModel())
                }
                if (!DownloadManager.isDownloading.value && !isDownloadStarting) {
                    isDownloadStarting = true
                    startDownload()
                } else {
                    downloadingStatePublisher.publishDownloadingState(map, DownloadingState.Queued)
                }
            }

            is DownloadRegions -> {
                val availableMaps =
                    if (DownloadManager.skippedProvinces[map].isNullOrEmpty().not()) {
                        map.regions.filter { !DownloadManager.isProvinceSkipped(it) }
                    } else {
                        map.regions
                    }.filter { !it.downloaded }
                if (availableMaps.isEmpty()) {
                    DownloadManager.skippedProvinces[map]?.clear()
                    return
                }
                DownloadManager.downloadQueue.removeIf { map.regions.contains(it) }
                DownloadManager.downloadingMap?.let {
                    if (map.regions.contains(DownloadManager.downloadingMap)) cancelDownload(it)
                }
                DownloadManager.downloadQueue.addLast(map)
                downloadScope.launch {
                    availableMaps.forEach {
                        addDownloadToDbUseCase(it.toQueueModel())
                    }
                }
                if (!DownloadManager.isDownloading.value && !isDownloadStarting) {
                    isDownloadStarting = true
                    startDownload()
                } else {
                    downloadingStatePublisher.publishDownloadingState(map, DownloadingState.Queued)
                    availableMaps.forEach {
                        downloadingStatePublisher.publishDownloadingState(it, DownloadingState.Queued)
                    }
                }
            }
        }
    }

    fun cancelDownload(map: Downloadable, isUpdate: Boolean = false) {
        downloadingStatePublisher.clearDownloadState(map)
        if (DownloadManager.downloadQueue.contains(map)) {
            when (map) {
                is DownloadItem -> {
                    downloadScope.launch { deleteDownloadFromDbUseCase(map.fileName) }
                }

                is DownloadRegions -> {
                    downloadScope.launch { deleteDownloadFromDbUseCase(map) }
                }
            }
            DownloadManager.downloadQueue.remove(map)
        } else if (map is DownloadItem && DownloadManager.isMapRegionQueued(map) && (DownloadManager.downloadingMap as? DownloadRegions)?.currentDownloadingProvince != map) {
            DownloadManager.downloadQueue.filterIsInstance<DownloadRegions>().forEach {
                if (it.regions.contains(map)) {
                    if (DownloadManager.skippedProvinces[it] == null) DownloadManager.skippedProvinces[it] = mutableListOf()
                    DownloadManager.skippedProvinces[it]?.add(map)
                }
            }
        } else if ((DownloadManager.downloadingMap as? DownloadRegions)?.currentDownloadingProvince == map) {
            skipCurrentProvinceDownloading()
        } else if (DownloadManager.getQueuedRegions().contains(map)) {
            if (map is DownloadItem) {
                downloadScope.launch {
                    deleteDownloadFromDbUseCase(map.fileName)
                }
                if (DownloadManager.skippedProvinces[DownloadManager.downloadingMap] == null) DownloadManager.skippedProvinces[DownloadManager.downloadingMap!!] = mutableListOf()
                DownloadManager.skippedProvinces[DownloadManager.downloadingMap]?.add(map)
                _downloadSuccessTrigger.update { !it }
            }
        } else if (DownloadManager.downloadingMap?.id == map.id) {
            if (map is DownloadRegions) {
                map.regions.forEach {
                    DownloadManager.downloadQueue.remove(it)
                    downloadScope.launch {
                        deleteDownloadFromDbUseCase(it.fileName)
                    }
                }
            }
            cancelCurrentDownload()
        } else if (map is DownloadItem) {
            val failedDownloadRegions = getFailedDownloadRegionsUseCase(map)
            if (failedDownloadRegions != null) {
                DownloadManager.skippedProvinces
                    .getOrPut(failedDownloadRegions) { mutableListOf() }
                    .add(map)
                downloadScope.launch {
                    delay(RETRY_ERROR_DEBOUNCE)
                    addMapToDownloadQueue(failedDownloadRegions)
                }
            }
            if (!isUpdate) downloadScope.launch { deleteMapUseCase(map) }
        } else if (map is DownloadRegions) {
            DownloadManager.skippedProvinces[map]?.clear()
            downloadScope.launch { deleteDownloadFromDbUseCase(map) }
            clearFailedDownloadRegionsUseCase(map)
        }
    }

    private suspend fun calculateDownloadSpeed(downloadable: Downloadable) {
        var previousFraction = 0.0
        val samplingQueue = FixedSizeQueue<Double>(30)

        currentCoroutineContext().job.invokeOnCompletion { downloadSpeed = 0.0 }
        downloadingStatePublisher
            .getDownloadProgressUseCase()
            .collect {
                val newFraction = it[downloadable.id]?.fraction ?: 0.0
                if (newFraction > 0.0 && previousFraction > 0.0) {
                    samplingQueue.add(newFraction - previousFraction)
                    downloadSpeed = samplingQueue.average()
                }
                previousFraction = newFraction
            }
    }

    private suspend fun checkAndShowConnectionError(map: Downloadable) {
        suspend fun publishDownloadingState(downloadingState: DownloadingState) {
            downloadingStatePublisher.publishDownloadingStateWithNotification(map, downloadingState)
        }

        isConnected = withTimeoutOrNull(RETRY_WAITING_TIME) {
            publishDownloadingState(DownloadingState.ErrorState.InternetConnectionError)
            networkManager.awaitNetworkAvailable()
            true
        } ?: withTimeoutOrNull(BACKGROUND_WAITING_TIME) {
            publishDownloadingState(DownloadingState.ErrorState.InternetConnectionRetryFailed)
            networkManager.awaitNetworkAvailable()
            true
        } ?: false

        if (isConnected) {
            publishDownloadingState(DownloadingState.Downloading)
        } else {
            DownloadManager.downloadingMap?.also { downloadable ->
                downloadingStatePublisher.clearDownloadState(downloadable)
            }
            DownloadManager.downloadingMap = null
            stopServiceIfFinished()
        }
    }

    private fun checkInternetConnection(): Boolean = when {
        !networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) -> false
        !networkManager.isNetworkReachable(NetworkType.ALL) -> false
        else -> true
    }

    private fun cancelCurrentDownload() {
        DownloadManager.downloadingMap = null
        cancelDownloadUseCase()
    }

    private fun skipCurrentProvinceDownloading() {
        speedJob?.cancel()
        speedJob = null
        skipDownloadUseCase()
    }

    private suspend fun downloadMap() {
        while (DownloadManager.downloadQueue.isNotEmpty()) {
            val map = DownloadManager.downloadQueue.removeFirst()
            downloadingStatePublisher.publishDownloadingState(map, DownloadingState.Downloading)
            DownloadManager.downloadingMap = map
            isDownloadStarting = false
            DownloadManager.isDownloading.value = true
            downloadMapUseCase(map)
                .collect { data ->
                    when (data) {
                        is Resource.Error -> {
                            when (val throwable = data.throwable) {
                                is UnknownHostException, is SocketException, is SocketTimeoutException, is HttpException -> {
                                    isConnected = false
                                    val connectionJob = downloadScope.launch { checkAndShowConnectionError(map) }
                                    while (!isConnected && !DownloadManager.downloadCanceled && !DownloadManager.downloadSkipped) {
                                        delay(1000)
                                    }
                                    connectionJob.cancel()
                                    downloadingStatePublisher
                                        .publishDownloadingState(map, DownloadingState.Default)
                                    when {
                                        DownloadManager.downloadSkipped -> {
                                            DownloadManager.downloadSkipped = false
                                            val dr = map as? DownloadRegions
                                            val currentProvince = dr?.currentDownloadingProvince ?: return@collect
                                            removeDownloadEntryUseCase(currentProvince)
                                            val skipped = DownloadManager.skippedProvinces[dr]?.toMutableList() ?: mutableListOf()
                                            skipped.add(currentProvince)
                                            cancelDownload(dr)
                                            DownloadManager.downloadCanceled = false
                                            DownloadManager.skippedProvinces[dr] = skipped
                                            _downloadSuccessTrigger.update { !it }
                                            addMapToDownloadQueue(dr)
                                        }

                                        DownloadManager.downloadCanceled -> {
                                            DownloadManager.downloadCanceled = false
                                            when (map) {
                                                is DownloadItem -> {
                                                    deleteDownloadFromDbUseCase(map.fileName)
                                                    removeDownloadEntryUseCase(map)
                                                }

                                                is DownloadRegions -> {
                                                    val dr = map as? DownloadRegions
                                                    val currentProvince = dr?.currentDownloadingProvince ?: return@collect
                                                    removeDownloadEntryUseCase(currentProvince)
                                                }
                                            }
                                        }

                                        (400 until 600).contains((throwable as? HttpException)?.code() ?: 0) -> {
                                            DownloadManager.downloadingMap = null
                                            downloadingStatePublisher
                                                .publishDownloadingState(
                                                    map,
                                                    DownloadingState.ErrorState.InternetConnectionRetryFailed
                                                )
                                            stopServiceIfFinished()
                                        }

                                        isConnected -> {
                                            if (DownloadManager.downloadingMap?.id == map.id &&
                                                !DownloadManager.isDownloading.value
                                            ) {
                                                DownloadManager.downloadQueue.addFirst(map)
                                            } else {
                                                DownloadManager.downloadQueue.addLast(map)
                                                downloadingStatePublisher.publishDownloadingState(
                                                    map,
                                                    DownloadingState.Queued,
                                                )
                                            }
                                        }
                                    }
                                }

                                is InsufficientMemoryException -> {
                                    downloadingStatePublisher
                                        .publishDownloadingStateWithNotification(
                                            map,
                                            DownloadingState.ErrorState.MemoryNotEnough
                                        )
                                    (map as? DownloadRegions)?.currentDownloadingProvince = null
                                    stopServiceIfFinished()
                                }

                                else -> {
                                    DownloadManager.downloadingMap = null
                                    downloadingStatePublisher
                                        .publishDownloadingState(
                                            map,
                                            DownloadingState.ErrorState.IoError
                                        )
                                    stopServiceIfFinished()
                                }
                            }
                        }

                        is Resource.Loading -> {
                            if (data.chunkFinished) {
                                downloadingStatePublisher
                                    .publishDownloadingState(map, DownloadingState.PreparingMap)
                            } else {
                                if (speedJob == null) {
                                    speedJob = downloadScope.launch { calculateDownloadSpeed(map) }
                                }
                                val currentDownloadingProvince = (map as? DownloadRegions)?.currentDownloadingProvince
                                if (currentDownloadingProvince != null && !DownloadManager.downloadCanceled) {
                                    val downloadedSize = map.regions.getDownloadedSize()
                                    // data.progressFraction here means the fraction of all maps that are being downloaded.
                                    val currentProvinceFraction = map.totalSize *
                                            (data.progressFraction - downloadedSize / map.totalSize)
                                    // downloadSpeed is a fraction based on `map.totalSize`.
                                    downloadingStatePublisher.publishDownloadProgress(
                                        currentDownloadingProvince,
                                        currentProvinceFraction / currentDownloadingProvince.getSizeAsMB(),
                                        downloadSpeed * map.totalSize / currentDownloadingProvince.getSizeAsMB()
                                    )
                                }

                                if (!DownloadManager.downloadCanceled) {
                                    downloadingStatePublisher
                                        .publishDownloadProgress(map, data.progressFraction, downloadSpeed)
                                    downloadingStatePublisher
                                        .publishDownloadingState(map, DownloadingState.Downloading)
                                }
                            }
                        }

                        is Resource.Success -> {
                            val downloadingMap = DownloadManager.downloadingMap
                            (downloadingMap as? DownloadItem)?.downloaded = true
                            downloadingMap?.also {
                                downloadingStatePublisher.publishDownloadSuccess(it)
                            }
                            val downloadFinished = when (downloadingMap) {
                                is DownloadItem -> {
                                    DownloadManager.downloadingMap = null
                                    downloadingStatePublisher.clearDownloadState(map)
                                    true
                                }

                                is DownloadRegions -> {
                                    val downloadedOrSkippedProvinces =
                                        downloadingMap.regions.filter {
                                            it.downloaded || DownloadManager.isProvinceSkipped(it)
                                        }
                                    val allRegionsDownloaded =
                                        downloadingMap.regions.size == downloadedOrSkippedProvinces.size
                                    downloadedOrSkippedProvinces.forEach {
                                        deleteDownloadFromDbUseCase(it.fileName)
                                        downloadingStatePublisher.clearDownloadState(it)
                                    }
                                    if (allRegionsDownloaded || downloadingMap.currentDownloadingProvince == null) {
                                        downloadingStatePublisher.clearDownloadState(map)
                                        DownloadManager.downloadingMap = null
                                    }
                                    allRegionsDownloaded
                                }

                                else -> true
                            }
                            if (DownloadManager.downloadingMap == null && DownloadManager.downloadQueue.isEmpty()) clearDownloadsDbUseCase()
                            speedJob?.cancel()
                            speedJob = null

                            _downloadSuccessTrigger.update { !it }
                            if (downloadFinished) {
                                stopServiceIfFinished()
                            }
                        }
                    }
                }
        }
    }

    private fun stopServiceIfFinished() {
        if (DownloadManager.downloadQueue.isEmpty() && isForeground) {
            isForeground = false
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun createNotificationChannel(){
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSilent(true)
        .setContentTitle(getString(R.string.service_notification_title))
        .setSmallIcon(com.mudita.map.common.R.drawable.icon_border_download)
        .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    companion object {
        private const val CHANNEL_ID = "Download_Channel"
        private const val CHANNEL_NAME = "Download"
        private const val NOTIFICATION_ID = 215
        private const val RETRY_ERROR_DEBOUNCE = 1000L
        private const val RETRY_WAITING_TIME: Long = 90000
        private const val BACKGROUND_WAITING_TIME: Long = 2700000
    }
}

@Composable
fun bindDownloadService(
    action: Flow<Action>,
): StateFlow<DownloadService?> {
    val downloadService = remember { MutableStateFlow<DownloadService?>(null) }
    val context = LocalContext.current

    var serviceBound by remember { mutableStateOf(false) }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                serviceBound = true
                downloadService.value = (service as DownloadService.LocalBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceBound = false
            }
        }
    }

    DisposableEffect(Unit) {
        if (DownloadManager.isDownloading.value || DownloadManager.downloadingMap != null) {
            context.bindDownloadService(serviceConnection)
        }
        onDispose { if (serviceBound) context.unbindService(serviceConnection) }
    }

    LaunchedEffect(Unit) {
        action
            .onEach { action ->
                when (action) {
                    is Action.CancelDownload -> {
                        val service = downloadService.firstNotNull()
                        service.cancelDownload(action.item)
                    }

                    is Action.Download -> {
                        var service = downloadService.first()
                        if (service == null) {
                            context.startForegroundService(
                                Intent(
                                    context,
                                    DownloadService::class.java
                                )
                            )
                            context.bindDownloadService(serviceConnection)
                            service = downloadService.firstNotNull()
                        }
                        action.items.forEach { item -> service.addMapToDownloadQueue(item) }
                    }
                }
            }.launchIn(this)
    }

    return downloadService
}
