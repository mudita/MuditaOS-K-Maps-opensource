package com.mudita.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.map.common.model.LocalizationHelper
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemData
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.map.common.utils.POI_NAME_TYPE_DELIMITED
import com.mudita.map.common.utils.getCategory
import com.mudita.map.common.utils.getDefaultLanguage
import com.mudita.map.common.utils.getIcon
import com.mudita.search.repository.category.SearchCategoryRepository
import com.mudita.searchhistory.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.osmand.Location
import net.osmand.ResultMatcher
import net.osmand.data.Amenity
import net.osmand.data.City
import net.osmand.data.LatLon
import net.osmand.data.Street
import net.osmand.osm.PoiCategory
import net.osmand.osm.PoiType
import net.osmand.search.SearchUICore
import net.osmand.search.SearchUICore.SearchResultCollection
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult
import timber.log.Timber

private const val QUERY_DEBOUNCE_TIMEOUT_MS = 1000L

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val searchCategoryRepository: SearchCategoryRepository,
    private val osmAndFormatter: OsmAndFormatter,
    private val localizationHelper: LocalizationHelper,
) : ViewModel() {
    private var currentResultMatcher: CancellableMatcher? = null
    private val _searchState = MutableStateFlow(SearchState())
    val searchState = _searchState.asStateFlow()

    private var currentItems = emptyList<SearchItemData>()

    private var searchUICore: SearchUICore? = null

    init {
        @Suppress("OPT_IN_USAGE")
        _searchState
            .map { it.searchText }
            .distinctUntilChanged()
            .debounce(QUERY_DEBOUNCE_TIMEOUT_MS)
            .onEach(::runSearch)
            .launchIn(viewModelScope)
    }

    fun setSearchUiCore(searchUICore: SearchUICore?) {
        this.searchUICore = searchUICore
        searchCategoryRepository.setupSearchCore(searchUICore)
    }

    fun onItemClicked(item: SearchItemData): SearchItem {
        if (item.itemType != SearchItemType.CATEGORY && item.itemType != SearchItemType.SUB_CATEGORY) {
            addHistoryItem(item)
        }

        // TODO remove those mappings and generate SearchItem properly from SearchResult
        val name = when (item.itemType) {
            SearchItemType.ADDRESS -> listOfNotNull(
                item.address?.takeUnless { it.isBlank() },
                item.title
            ).joinToString(" ")

            else -> item.formattedTitle
        }
        val description = when (item.itemType) {
            SearchItemType.POI -> item.desc?.substringAfterLast(POI_NAME_TYPE_DELIMITED)
            else -> item.desc
        }
        val address = when (item.itemType) {
            SearchItemType.ADDRESS -> null
            else -> item.address
        }

        return SearchItem(
            UUID.randomUUID(),
            name,
            description,
            address,
            null,
            LatLon(item.lat, item.lon),
            item.iconRes,
            item.itemType
        )
    }

    fun resetSearch() = viewModelScope.launch {
        _searchState.update {
            it.copy(
                searchText = "",
                searchInProgress = false,
                radius = 1,
                showSearchMore = false,
                nextSearchRadius = null,
                showEmptyResults = false,
            )
        }
        currentResultMatcher?.cancelled = true
        currentResultMatcher = null
        getHistory()
    }

    fun formatDistance(searchItemData: SearchItemData): String =
        searchItemData.formattedDistance(osmAndFormatter)

    fun updateSearchData(searchItem: SearchItem?) {
        Timber.i("Update search data: $searchItem")
        getHistory()
    }

    fun onRadiusIncreased() {
        if (searchUICore?.isSearchMoreAvailable(searchUICore?.phrase) != true) {
            _searchState.value = searchState.value.copy(nextSearchRadius = null)
            return
        }
        _searchState.value = searchState.value.copy(
            radius = searchState.value.radius + 1,
            nextSearchRadius = null,
            showEmptyResults = false,
        )
        runSearch(_searchState.value.searchText)
    }

    private fun getHistory() {
        val location = visibleLocationOrFallback()
        viewModelScope.launch {
            historyRepository.getHistory(location).onSuccess { historyFlow ->
                historyFlow
                    .onEach { historyItems ->
                        currentItems = historyItems
                        _searchState.tryEmit(_searchState.value.copy(currentItems = currentItems))
                    }
                    .launchIn(this)
            }
        }
    }

    fun onSearchTextChanged(query: String) {
        if (query == _searchState.value.searchText) {
            return
        }

        currentResultMatcher?.cancelled = true
        currentResultMatcher = null
        _searchState.update {
            it.copy(
                searchText = query,
                searchInProgress = false,
                radius = 1,
                showSearchMore = false,
                nextSearchRadius = null,
                showEmptyResults = false,
            )
        }
    }

    private fun runSearch(query: String) {
        if (query.isEmpty()) {
            getHistory()
            return
        }

        searchUICore?.let {
            runCoreSearchInternal(it, query)
        }
    }

    private fun runCoreSearchInternal(searchUICore: SearchUICore, text: String) {
        val settings = searchUICore.searchSettings
            .setSearchTypes(
                ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE,
                ObjectType.HOUSE, ObjectType.STREET_INTERSECTION, ObjectType.STREET,
                ObjectType.LOCATION, ObjectType.POI
            )
            .setRadiusLevel(_searchState.value.radius)
            .setEmptyQueryAllowed(false)
            .setLang(getDefaultLanguage(), false)
        searchUICore.updateSettings(settings)

        val location = settings.originalLocation ?: LatLon.zero
        val resultMatcher = prepareResultMatcher(searchUICore, text, location)
        currentResultMatcher = resultMatcher
        searchUICore.search(text, false, resultMatcher)
    }

    private fun prepareResultMatcher(
        searchUICore: SearchUICore,
        text: String,
        location: LatLon
    ): CancellableMatcher {
        return object : CancellableMatcher {
            var results = mutableListOf<SearchResult>()
            var resultCollection: SearchResultCollection? = null
            override var cancelled = false

            override fun publish(searchResult: SearchResult): Boolean {
                when (searchResult.objectType) {
                    ObjectType.SEARCH_STARTED -> {
                        _searchState.update {
                            it.copy(
                                searchInProgress = true,
                                showSearchMore = false
                            )
                        }
                    }

                    ObjectType.SEARCH_FINISHED -> {
                        resultCollection?.let {
                            if (!it.hasSearchResults()) {
                                showSearchResults(text, location, it)
                            }
                        }
                        if (!isCancelled) {
                            val isSearchMoreAvailable =
                                searchUICore.isSearchMoreAvailable(searchUICore.phrase)
                            val nextSearchRadius = osmAndFormatter
                                .calculateRoundedDist(
                                    searchUICore.getNextSearchRadius(searchUICore.phrase).toDouble()
                                )
                                .toFloat()
                                .takeIf { isSearchMoreAvailable }
                                ?.let { osmAndFormatter.getFormattedDistanceValue(it).formattedValue }
                            val showEmptyResults = resultCollection?.hasSearchResults() != true
                            _searchState.update {
                                it.copy(
                                    searchInProgress = false,
                                    showEmptyResults = showEmptyResults,
                                    nextSearchRadius = nextSearchRadius,
                                    showSearchMore = isSearchMoreAvailable && !showEmptyResults
                                )
                            }
                        }
                        resultCollection = null
                    }

                    ObjectType.FILTER_FINISHED -> {}

                    ObjectType.SEARCH_API_FINISHED -> {
                        if (isCancelled) {
                            return false
                        }
                        addToResultCollection(results, searchResult.requiredSearchPhrase)
                    }

                    ObjectType.SEARCH_API_REGION_FINISHED -> {
                        if (isCancelled) {
                            return false
                        }
                        addToResultCollection(results, searchResult.requiredSearchPhrase)
                    }

                    ObjectType.PARTIAL_LOCATION -> {
                    }

                    else -> {
                        if (isCancelled) {
                            return false
                        }
                        results.add(searchResult)
                    }
                }
                return true
            }

            override fun isCancelled(): Boolean {
                return cancelled
            }

            private fun addToResultCollection(
                results: MutableList<SearchResult>,
                searchPhrase: SearchPhrase
            ) {
                val resultCollection = resultCollection ?: SearchResultCollection(searchPhrase)
                this.resultCollection = resultCollection
                resultCollection.addSearchResults(results, true, true)
                if (resultCollection.hasSearchResults() && results.isNotEmpty()) {
                    showSearchResults(text, location, resultCollection)
                }

                this.results.clear()
            }
        }
    }

    private fun showSearchResults(
        text: String,
        location: LatLon,
        resultCollection: SearchResultCollection
    ) {
        _searchState.value = _searchState.value.copy(
            currentItems = resultCollection.currentSearchResults.map {
                val distance = calculateDistance(
                    it.location?.latitude ?: 0.0,
                    it.location?.longitude ?: 0.0,
                    location.latitude,
                    location.longitude,
                )
                val itemType = when (it.objectType) {
                    ObjectType.POI -> SearchItemType.POI
                    ObjectType.HOUSE -> SearchItemType.ADDRESS
                    ObjectType.POSTCODE -> SearchItemType.POSTCODE
                    else -> SearchItemType.HISTORY
                }
                val address = when (it.objectType) {
                    ObjectType.HOUSE -> it.localeRelatedObjectName
                    else -> null
                }
                val title = when (it.objectType) {
                    ObjectType.LOCATION -> ""
                    else -> it.localeName
                }
                val desc = when (it.objectType) {
                    ObjectType.HOUSE -> getCity(it)
                    else -> getCategory(it, localizationHelper)
                }
                val lat = it.location?.latitude ?: 0.0
                val lon = it.location?.longitude ?: 0.0
                val id = generateItemId(title, desc, lat, lon, itemType, address)
                var poiCategory: String? = null
                var poiType: String? = null
                (it.`object` as? Amenity)?.also { amenity ->
                    poiCategory = amenity.type.keyName
                    poiType = amenity.subType
                }
                SearchItemData.History(
                    id = id, // DB primary key with OnConflictStrategy.REPLACE
                    searchQuery = text,
                    title = title,
                    desc = desc,
                    distance = distance,
                    lat = lat,
                    lon = lon,
                    iconRes = getIcon(it),
                    itemType = itemType,
                    address = address,
                    poiCategory = poiCategory,
                    poiType = poiType,
                    cityType = (it.`object` as? City)?.type,
                    allNames = (it.`object` as? City)?.getNamesMap(true),
                    alternateName = if (it.objectType != ObjectType.HOUSE) it.alternateName else null,
                )
            }
        )
    }

    private fun addHistoryItem(item: SearchItemData) {
        item.searchTime = System.currentTimeMillis()
        viewModelScope.launch {
            historyRepository.addHistoryItem(item)
        }
    }

    private fun calculateDistance(
        latitude: Double,
        longitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Double {
        val mes = floatArrayOf(0f, 0f)
        Location.distanceBetween(
            toLatitude,
            toLongitude,
            latitude,
            longitude,
            mes
        )
        return mes[0].toDouble()
    }

    private fun getCity(searchResult: SearchResult): String? {
        val relatedStreet = searchResult.relatedObject as? Street ?: return null

        return relatedStreet.city?.getName(
            searchResult.requiredSearchPhrase.settings.lang,
            true
        )
    }

    private fun visibleLocationOrFallback(): LatLon =
        searchUICore?.searchSettings?.originalLocation ?: LatLon.zero

    private fun generateItemId(
        title: String,
        desc: String?,
        lat: Double,
        lon: Double,
        itemType: SearchItemType,
        address: String?
    ): Int {
        return title.hashCode()
            .times(31).plus(desc.hashCode())
            .times(31).plus(lat.hashCode())
            .times(31).plus(lon.hashCode())
            .times(31).plus(itemType.name.hashCode())
            .times(31).plus(address.hashCode())
    }

    data class SearchState(
        val currentItems: List<SearchItemData> = emptyList(),
        val searchText: String = "",
        val selectedParentCategory: PoiCategory? = null,
        val selectedSubCategoryPageNumber: Int = 1,
        val selectedSubCategory: PoiType? = null,
        val radius: Int = 1,
        val searchInProgress: Boolean = false,
        val showEmptyResults: Boolean = false,
        val nextSearchRadius: String? = null,
        val showSearchMore: Boolean = false,
    )

    private interface CancellableMatcher : ResultMatcher<SearchResult> {
        var cancelled: Boolean
    }
}