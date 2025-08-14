package com.mudita.download.ui

import com.mudita.maps.data.api.Resource as ResourceData
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.download.R
import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.domain.DeleteDownloadFromDbUseCase
import com.mudita.download.domain.DeleteMapUseCase
import com.mudita.download.domain.GetIndexesUseCase
import com.mudita.download.repository.models.*
import com.mudita.download.repository.utils.DownloadManager
import com.mudita.download.usecase.SetDownloadPausedUseCase
import com.mudita.download.utils.isItemDownloaded
import com.mudita.download.utils.loadObfData
import com.mudita.map.common.IntentHandler
import com.mudita.map.common.di.DispatcherQualifier
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState
import com.mudita.map.common.download.GetDownloadErrorNotificationUseCase
import com.mudita.map.common.download.GetDownloadProgressUseCase
import com.mudita.map.common.download.GetDownloadingStateUseCase
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkStatus
import com.mudita.map.common.utils.network.NetworkType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.osmand.IndexConstants
import retrofit2.HttpException

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val networkManager: NetworkManager,
    private val memoryManager: MemoryManager,
    private val getIndexesUseCase: GetIndexesUseCase,
    private val deleteMapUseCase: DeleteMapUseCase,
    private val deleteDownloadFromDbUseCase: DeleteDownloadFromDbUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val setDownloadPausedUseCase: SetDownloadPausedUseCase,
    private val getDownloadingStateUseCase: GetDownloadingStateUseCase,
    private val getDownloadProgressUseCase: GetDownloadProgressUseCase,
    getDownloadErrorNotificationUseCase: GetDownloadErrorNotificationUseCase,
    @DispatcherQualifier.IO private val dispatcher: CoroutineDispatcher,
) : ViewModel(), IntentHandler<DownloadViewModel.Intent> {

    private val _uiState = MutableStateFlow(DownloadState())
    val uiState: StateFlow<DownloadState> = _uiState.asStateFlow()

    private val _action = MutableStateFlow<Action>(Action.Empty)
    val action: StateFlow<Action> = _action.asStateFlow()

    val errorNotifications: Flow<Pair<Downloadable, DownloadingState.ErrorState?>> =
        getDownloadErrorNotificationUseCase()
            .map { (id, errorState) -> allDownloadableItems[id]?.to(errorState) }
            .filterNotNull()

    private val pages = ArrayDeque<Page>()

    private class Page(
        val resources: List<Resource>,
        val downloadRegions: DownloadRegions? = null,
        var firstVisibleItemIndex: Int = 0,
    )

    private val titles = ArrayDeque<String>()

    private val allMaps = mutableListOf<Resource>()
    private val allDownloadableItems = mutableMapOf<String, Downloadable>()
    private var downloadedItems = linkedSetOf<MapFile>()
    private var isLoadingDownloadedItems = true
    private var localIndexes = emptyList<LocalIndex>()

    private var downloadedMapsDuplicates = listOf<String>()
    private var shouldUpdateDownloadItemsAfterEdit: Boolean = false

    private var rootResourceGroup: ResourceGroup? = null

    init {
        observeUiState()
        collectNetworkStatus()
        collectDownloadingState()
        collectDownloadProgress()
    }

    override fun obtainIntent(intent: Intent) {
        when (intent) {
            is Intent.SearchQueryChange -> {
                onSearchQueryChanged(intent.value)
            }
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        search(query)
    }

    private fun collectDownloadingState() {
        getDownloadingStateUseCase()
            .onEach(::onDownloadingStateChanged)
            .launchIn(viewModelScope)
    }

    private fun collectDownloadProgress() {
        getDownloadProgressUseCase()
            .onEach(::onDownloadProgress)
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectNetworkStatus() {
        networkManager.networkStatus.distinctUntilChanged().flatMapLatest { status ->
            val mapsNotLoaded = !_uiState.value.indexesFetched
            _uiState.update {
                val isNetworkAvailable = status == NetworkStatus.Available
                it.copy(isNetworkAvailable = isNetworkAvailable)
            }
            if (mapsNotLoaded) {
                getResources() // Might load the cached ResourceGroups.
            } else {
                emptyFlow()
            }
        }.launchIn(viewModelScope)
    }

    fun cancelDownload(item: Downloadable) {
        setDownloadPausedUseCase(true)
        _action.update { Action.CancelDownload(item) }
    }

    private fun onDownloadProgress(downloadProgressMap: Map<String, DownloadProgress>) {
        _uiState.update { it.copy(downloadProgressMap = downloadProgressMap) }
    }

    fun updateMap(localMap: LocalIndex) {
        viewModelScope.launch(dispatcher) {
            val downloadItem = findDownloadItem(localMap) ?: return@launch
            if (checkInternetAndMemory(downloadItem, downloadItem.getFullSize())) {
                downloadMap(downloadItem)
            }
        }
    }

    fun deleteMap(localMap: LocalIndex) {
        val downloadItem = findDownloadItem(localMap)
        if (uiState.value.isDownloading(localMap)) cancelDownloadUseCase()
        if (uiState.value.isQueued(localMap) && downloadItem != null) {
            viewModelScope.launch(dispatcher) {
                deleteDownloadFromDbUseCase(downloadItem.fileName)
            }
            DownloadManager.downloadQueue.remove(downloadItem)
        }
        downloadItem?.downloaded = false
        viewModelScope.launch(dispatcher) {
            deleteMapUseCase(localMap)
        }
        downloadedItems.remove(localMap)
        localIndexes = localIndexes - localMap
        deleteDuplicatedMapIfNeeded(localMap)
        modifyState()

        if (_uiState.value.downloadedItems.isEmpty()) cancelEditMode()
    }

    private fun deleteDuplicatedMapIfNeeded(map: LocalIndex) {
        val fileName = map.file.name
        if (downloadedMapsDuplicates.contains(fileName)) {
            downloadedMapsDuplicates = downloadedMapsDuplicates.minus(fileName)
            shouldUpdateDownloadItemsAfterEdit = true
        }
    }

    fun cancelUpdate(localMap: LocalIndex) {
        setDownloadPausedUseCase(true)
        findDownloadItem(localMap)?.also { downloadItem ->
            _action.update { Action.CancelDownload(downloadItem) }
        }
    }

    private fun onDownloadingStateChanged(stateMap: Map<String, DownloadingState>) {
        _uiState.update { it.copy(downloadingStateMap = stateMap) }
    }

    fun onActionCanceled() {
        setDownloadPausedUseCase(false)
        _action.update { Action.Empty }
    }

    fun onDownloadStarted() {
        onActionCanceled()
    }

    fun onDownloadCanceled() {
        onActionCanceled()
    }

    fun goToPreviousPage(): Boolean {
        if (pages.size <= 1) return false
        titles.removeLastOrNull()
        pages.removeLastOrNull()
        modifyState()
        return true
    }

    fun refreshLocalIndexAfterEdit(
        indexFileNames: Map<String, String>,
        indexFiles: Map<String, File>,
        getLocalPath: (String?) -> File,
        getSdCardPath: (String?) -> File?,
    ) {
        if (shouldUpdateDownloadItemsAfterEdit) {
            shouldUpdateDownloadItemsAfterEdit = false
            getLocalIndexData(
                readFiles = true,
                indexFileNames = indexFileNames,
                indexFiles = indexFiles,
                getLocalPath = getLocalPath,
                getSdCardPath = getSdCardPath
            )
        }
    }

    fun clearSearchState() {
        pages.clear()
        pages.add(Page(allMaps))
        titles.clear()
        _uiState.update { it.copy(isSearchResultEmpty = false, searchQuery = "") }
        modifyState()
    }

    fun cancelSearchMode() {
        _action.update { Action.CancelSearch }
    }

    private fun cancelEditMode() {
        _action.update { Action.CancelEditMode }
    }

    fun search(query: String) {
        pages.clear()
        _uiState.update { it.copy(isSearchResultEmpty = false) }
        if (query.isNotBlank()) {
            val downloadIndexes = mutableSetOf<Resource>()
            allMaps.forEach {
                it as ResourceGroup
                if (it.name.contains(query, true)) {
                    downloadIndexes.add(it)
                }
            }
            allMaps.forEach {
                it as ResourceGroup
                searchAndAddResources(query, it, downloadIndexes)
            }
            val (itemsStartingWithQuery, itemsNotStartingWithQuery) = downloadIndexes.partition {
                when (it) {
                    is ResourceGroup -> it.name.startsWith(prefix = query, ignoreCase = true)
                    is DownloadItem -> it.name.startsWith(prefix = query, ignoreCase = true)
                    is DownloadRegions -> false
                }
            }
            pages.add(Page(itemsStartingWithQuery.plus(itemsNotStartingWithQuery)))
            if (downloadIndexes.isEmpty()) {
                _uiState.update { it.copy(isSearchResultEmpty = true) }
            }
        }
        modifyState()
    }

    private fun getUniqueFiles(files: List<LocalIndex>): List<LocalIndex> {
        val uniqueFilesMap = mutableMapOf<String, LocalIndex>()
        val result = mutableListOf<LocalIndex>()

        for (file in files) {
            val fileName = file.file.name
            if (!uniqueFilesMap.containsKey(fileName) || file.file.lastModified() > uniqueFilesMap[fileName]!!.file.lastModified()) {
                uniqueFilesMap[fileName] = file
            }
        }

        downloadedMapsDuplicates = files.groupingBy { it.file.name }
            .eachCount()
            .filter { it.value >= 2 }
            .map { it.key }

        result.addAll(uniqueFilesMap.values)
        return result
    }

    fun getLocalIndexData(
        readFiles: Boolean,
        indexFileNames: Map<String, String>,
        indexFiles: Map<String, File>,
        getLocalPath: (String?) -> File,
        getSdCardPath: (String?) -> File?,
        vararg indexTypes: LocalIndexType?
    ) {
        viewModelScope.launch(dispatcher) {
            val result: MutableList<LocalIndex> = mutableListOf()
            var types: Array<out LocalIndexType?> = indexTypes
            if (types.isEmpty()) {
                types = LocalIndexType.values()
            }
            for (type in types) {
                when (type) {
                    LocalIndexType.MAP_DATA -> {
                        loadObfData(
                            dataPath = getLocalPath(IndexConstants.MAPS_PATH),
                            result = result,
                            readFiles = readFiles,
                            indexFileNames = indexFileNames,
                            indexFiles = indexFiles,
                            osmandRegions = DownloadManager.appRegions,
                        )
                        getSdCardPath(IndexConstants.MAPS_PATH)?.let {
                            loadObfData(
                                dataPath = it,
                                result = result,
                                readFiles = readFiles,
                                indexFileNames = indexFileNames,
                                indexFiles = indexFiles,
                                osmandRegions = DownloadManager.appRegions,
                            )
                        }
                    }

                    else -> Unit
                }
            }
            downloadedItems = getUniqueFiles(result)
                .filter {
                    it.type == LocalIndexType.MAP_DATA &&
                            it.fileName.contains(DEFAULT_WORLD_MAP_FILENAME).not()
                }
                .also(::updateIsUpdateAvailableState)
                .let(::LinkedHashSet)

            localIndexes = downloadedItems.filterIsInstance<LocalIndex>()
                .sortedByDescending { it.file.lastModified() }
            isLoadingDownloadedItems = false
            collectDownloadingItems()
        }
    }

    fun onItemClicked(
        resource: Resource,
        isSearchMode: Boolean,
        firstVisibleItemIndex: Int,
    ) {
        viewModelScope.launch(dispatcher) {
            when (resource) {
                is DownloadItem -> {
                    if (checkInternetAndMemory(resource, resource.getFullSize())) downloadMap(resource)
                }

                is ResourceGroup -> {
                    val downloadIndexes = mutableListOf<Resource>()
                    downloadIndexes.addAll(resource.groups)

                    if (isSearchMode) {
                        clearSearchState()
                        cancelSearchMode()
                    }

                    var downloadRegions: DownloadRegions? = null
                    if (resource.isSingleCountry) {
                        if (resource.allDownloadItems.size > 1) {
                            downloadRegions = DownloadRegions(resource.name, resource.allDownloadItems)
                        }
                        downloadIndexes.addAll(resource.individualDownloadItems)
                    } else {
                        resource.individualDownloadItems.forEach {
                            val group = ResourceGroup(
                                name = it.name,
                                individualDownloadItems = mutableListOf(it)
                            )
                            downloadIndexes.add(group)
                        }
                    }
                    pages.lastOrNull()?.firstVisibleItemIndex = firstVisibleItemIndex
                    pages.add(
                        Page(
                            resources = downloadIndexes.sortedWith(Resource.NameComparator),
                            downloadRegions = downloadRegions,
                        )
                    )
                    titles.add(resource.name)
                }

                is DownloadRegions -> {
                    resource.regions.forEach {
                        if (downloadedItems.isItemDownloaded(it)) it.downloaded = true
                    }
                    downloadMap(resource)
                }
            }
            modifyState()
        }
    }

    private inline fun <reified T : DownloadingState> getMapsWithState(): List<DownloadItem> =
        uiState.value.downloadingStateMap.mapNotNull { (id, state) ->
            if (state is T) allDownloadableItems[id] as? DownloadItem else null
        }

    private fun observeUiState() {
        uiState.onEach { collectDownloadingItems() }.launchIn(viewModelScope)
    }

    private fun collectDownloadingItems() {
        val localIndexes = localIndexes
        val downloadedMaps = localIndexes.filter { it.updateAvailable }.toMutableList()
        val mapsWithUpdateAvailable = localIndexes.filter { !it.updateAvailable }.toMutableList()

        val queuedFiles = getMapsWithState<DownloadingState.Queued>()
        val downloadingFiles = getMapsWithState<DownloadingState.Downloading>()
        val preparingFiles = getMapsWithState<DownloadingState.PreparingMap>()
        val errorDownloads = getMapsWithState<DownloadingState.ErrorState>()
        val set = linkedSetOf<MapFile>()
        set.addAll(errorDownloads)
        set.addAll(downloadingFiles)
        set.addAll(preparingFiles)
        set.addAll(queuedFiles)
        set.addAll(downloadedMaps)
        set.addAll(mapsWithUpdateAvailable)
        downloadedItems = LinkedHashSet(set.distinctBy { it.getBasename() })
        modifyState()
    }

    fun findDownloadItem(mapFile: MapFile): DownloadItem? =
        allMaps.firstNotNullOfOrNull { resource ->
            when (resource) {
                is DownloadItem ->
                    if (resource.getBasename() == mapFile.getBasename()) resource else null

                is ResourceGroup ->
                    resource.allDownloadItems.firstNotNullOfOrNull { item ->
                        if (item.getBasename() == mapFile.getBasename()) item else null
                    }

                else ->
                    null
            }
        }

    fun isUpdatedAvailable(downloadable: Downloadable): Boolean =
        when (downloadable) {
            is DownloadItem -> {
                localIndexes.firstNotNullOfOrNull { localIndex ->
                    localIndex.takeIf { it.getBasename() == downloadable.getBasename() }
                }?.updateAvailable == true
            }

            is DownloadRegions -> false
        }

    private fun getResources(): Flow<Unit> =
        getIndexesUseCase()
            .onEach {
                when (it) {
                    is ResourceData.Error -> {
                        when (it.throwable) {
                            is UnknownHostException, is SocketException, is SocketTimeoutException, is HttpException -> {
                                _uiState.update { state -> state.copy(isNetworkAvailable = false) }
                            }
                        }
                    }

                    is ResourceData.Loading -> Unit
                    is ResourceData.Success -> {
                        allMaps.clear()
                        allMaps.addAll(it.model.groups)
                        allMaps.addAll(it.model.individualDownloadItems)
                        allDownloadableItems.clear()
                        allMaps
                            .allDownloadableItems()
                            .associateBy { downloadable -> downloadable.id }
                            .also(allDownloadableItems::putAll)
                        rootResourceGroup = it.model
                        pages.clear()
                        pages.add(Page(allMaps))
                        titles.clear()
                        updateIsUpdateAvailableState(downloadedItems.filterIsInstance<LocalIndex>())
                        collectDownloadingItems()
                        if (_uiState.value.searchQuery.isNotBlank()) {
                            search(_uiState.value.searchQuery)
                        }
                    }
                }
            }
            .map {}
            .flowOn(dispatcher)

    private fun updateIsUpdateAvailableState(localIndexes: List<LocalIndex>) {
        val resourceGroup = rootResourceGroup ?: return
        localIndexes.forEach { localIndex ->
            val downloadItem =
                resourceGroup.allDownloadItems.firstOrNull { it.getBasename() == localIndex.getBasename() }
            localIndex.updateAvailable =
                (downloadItem?.timestamp ?: 0) > localIndex.file.lastModified()
        }
    }

    private fun downloadMap(map: Downloadable) {
        if (map is DownloadRegions) {
            val maps = map.regions.filter { !it.downloaded }
            map.currentDownloadingProvince = null
            maps.forEach { item ->
                if (map.currentDownloadingProvince == null &&
                    checkInternetAndMemory(item, item.getFullSize())
                ) {
                    map.currentDownloadingProvince = item
                }
            }
            if (map.currentDownloadingProvince == null) return
        }
        _action.update { Action.Download(map) }
    }

    private fun searchAndAddResources(
        query: String,
        group: ResourceGroup,
        downloadIndexes: MutableSet<Resource>
    ) {
        group.groups.forEach { subGroup ->
            if (subGroup.name.contains(query, true)) {
                downloadIndexes.add(subGroup)
            }
            if (subGroup.groups.isNotEmpty()) {
                searchAndAddResources(query, subGroup, downloadIndexes)
            } else {
                val foundItems = subGroup.individualDownloadItems.filter { item ->
                    item.name.contains(query, ignoreCase = true)
                }
                downloadIndexes.addAll(foundItems)
            }
        }
        val foundItems = group.individualDownloadItems.filter { item ->
            item.name.contains(query, ignoreCase = true)
        }
        downloadIndexes.addAll(foundItems)
    }

    private fun checkInternetAndMemory(downloadable: Downloadable, downloadingMB: Double): Boolean =
        checkInternetConnection(downloadable) && hasMemorySpace(downloadable, downloadingMB)

    private fun modifyState() {
        _uiState.update {
            val lastPage = pages.lastOrNull()
            it.copy(
                downloadedItems = downloadedItems,
                isLoadingDownloadedItems = isLoadingDownloadedItems,
                downloadRegions = lastPage?.downloadRegions,
                downloadIndexes = lastPage?.resources ?: emptyList(),
                screenTitle = titles.lastOrNull(),
                isRootIndex = pages.size <= 1,
                indexesFetched = allMaps.isNotEmpty(),
                firstVisibleItemIndex = lastPage?.firstVisibleItemIndex ?: 0,
            )
        }
    }

    private fun checkInternetConnection(downloadable: Downloadable): Boolean = when {
        !networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) -> {
            _action.update { Action.ShowWifiNetworkError(downloadable) }
            false
        }

        !networkManager.isNetworkReachable(NetworkType.ALL) -> {
            _action.update { Action.ShowNetworkError(downloadable) }
            false
        }

        else -> true
    }

    private fun hasMemorySpace(downloadable: Downloadable, downloadingMB: Double): Boolean = when {
        !memoryManager.hasEnoughSpace(downloadingMB) -> {
            _action.update { Action.ShowMemoryError(downloadable) }
            false
        }

        else -> true
    }

    enum class LocalIndexType(
        @StringRes
        private val resId: Int,
        @DrawableRes
        val iconResource: Int,
        private val orderIndex: Int
    ) {
        MAP_DATA(
            R.string.local_indexes_cat_map,
            R.drawable.ic_map,
            10
        );

        fun getHumanString(ctx: Context): String {
            return ctx.getString(resId)
        }

        fun getOrderIndex(info: LocalIndex): Int {
            val fileName = info.fileName
            var index: Int = info.originalType.orderIndex
            if (fileName.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
                index++
            }
            return index
        }

        fun getBasename(localIndex: LocalIndex): String {
            val fileName = localIndex.fileName
            if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
                return fileName.substring(0, fileName.length - IndexConstants.EXTRA_ZIP_EXT.length)
            }
            if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
                return fileName.substring(0, fileName.length - IndexConstants.SQLITE_EXT.length)
            }
            val ls = fileName.lastIndexOf('_')
            if (ls >= 0) {
                return fileName.substring(0, ls)
            } else if (fileName.indexOf('.') > 0) {
                return fileName.substring(0, fileName.indexOf('.'))
            }
            return fileName
        }
    }

    data class DownloadState(
        override val downloadedItems: Set<MapFile> = emptySet(),
        val isLoadingDownloadedItems: Boolean = true,
        val downloadRegions: DownloadRegions? = null,
        val downloadIndexes: List<Resource> = emptyList(),
        override val downloadProgressMap: Map<String, DownloadProgress> = emptyMap(),
        val isNetworkAvailable: Boolean = true,
        val isSearchResultEmpty: Boolean = false,
        override val downloadingStateMap: Map<String, DownloadingState> = emptyMap(),
        val screenTitle: String? = null,
        val searchQuery: String = "",
        val isRootIndex: Boolean = true,
        val indexesFetched: Boolean = false,
        val firstVisibleItemIndex: Int = 0,
    ) : DownloadItemInfo {
        val downloadIndexesHash: Int = downloadIndexes.fold(0) { hash, resource ->
            hash + resource.id.hashCode() * 31
        }
    }

    sealed class Action {
        data class Download(val map: Downloadable) : Action()
        data class CancelDownload(val map: Downloadable) : Action()
        object Empty : Action()
        object CancelSearch : Action()
        object CancelEditMode : Action()
        data class ShowNetworkError(val map: Downloadable) : Action()
        data class ShowWifiNetworkError(val map: Downloadable) : Action()
        data class ShowMemoryError(val map: Downloadable) : Action()
    }

    sealed class Intent {
        data class SearchQueryChange(val value: String) : Intent()
    }

    companion object {
        private const val DEFAULT_WORLD_MAP_FILENAME = "World_basemap_mini"
    }
}

fun List<Resource>.allDownloadableItems(): List<Downloadable> =
    buildList {
        this@allDownloadableItems.forEach { resource ->
            when (resource) {
                is DownloadItem -> {
                    add(resource)
                }

                is DownloadRegions -> {
                    add(resource)
                    addAll(resource.regions)
                }

                is ResourceGroup -> {
                    add(DownloadRegions(resource.name, resource.allDownloadItems))
                    addAll(resource.individualDownloadItems)
                    addAll(resource.groups.allDownloadableItems())
                }
            }
        }
    }
