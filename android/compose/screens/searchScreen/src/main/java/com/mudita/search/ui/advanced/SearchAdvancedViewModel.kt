package com.mudita.search.ui.advanced

import android.location.Location
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.map.common.IntentHandler
import com.mudita.map.common.model.LocalizationHelper
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemData
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.repository.geocoding.GeocodingRepository
import com.mudita.map.common.utils.getCategory
import com.mudita.map.common.utils.removeDiacriticalMarks
import com.mudita.search.repository.model.CitySearchHistory
import com.mudita.search.repository.model.SearchCityItem
import com.mudita.searchhistory.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.osmand.ResultMatcher
import net.osmand.data.LatLon
import net.osmand.search.SearchUICore
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import timber.log.Timber

@Stable
@HiltViewModel
class SearchAdvancedViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val geocodingRepository: GeocodingRepository,
    private val localizationHelper: LocalizationHelper,
    repositorySearchUiCore: SearchUICore
) : ViewModel(), IntentHandler<SearchAdvancedViewModel.Intent> {

    private val citySearchHistory: CitySearchHistory = CitySearchHistory()

    private val _uiState = MutableStateFlow(SearchState())
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()

    private var searchItems: MutableSet<SearchCityItem> = mutableSetOf()

    private var searchUICore: SearchUICore = repositorySearchUiCore
    private var location: LatLon = LatLon(0.0, 0.0)

    override fun obtainIntent(intent: Intent) {
        when (intent) {
            is Intent.SearchQueryChange -> onSearchQueryChanged(intent.value)
            else -> Unit
        }
    }

    fun init(searchUICore: SearchUICore?, location: LatLon?) {
        if (searchUICore != null) {
            this.searchUICore = searchUICore
        }
        location?.let { this.location = it }
    }

    fun setItem(searchItem: SearchItem?) {
        Timber.d("SearchAdvancedViewModel setItem: $searchItem")
        _uiState.update { it.copy(searchItem = searchItem) }
    }

    fun onShowOnMapClicked(callback: (SearchItemData.Address?) -> Unit) {
        val address = uiState.value.searchCityItem?.toAddress()
        callback.invoke(address)
    }

    fun increaseRadius(query: String = "") {
        _uiState.update { it.copy(radius = it.radius + 1) }
        val city = uiState.value.searchCity
        if (city.isNullOrEmpty()) {
            if (uiState.value.searchItem?.itemType == SearchItemType.POSTCODE)
                runCoreSearchInternal(query)
            else
                runCoreSearchInternal("")
        }
        else runCoreSearchInternal(city, true)
    }

    fun onItemClicked(item: SearchCityItem, callback: (SearchItemData.Address?) -> Unit) {
        _uiState.update { it.copy(searchCityItem = item) }

        when (item.sr.objectType) {
            ObjectType.POI, ObjectType.LOCATION, ObjectType.HOUSE, ObjectType.FAVORITE, ObjectType.RECENT_OBJ, ObjectType.WPT, ObjectType.STREET_INTERSECTION, ObjectType.GPX_TRACK -> {

                _uiState.update { it.copy(searchHouse = item.sr.localeName) }
                val addressItem = item.toAddress()
                addressItem.searchTime = System.currentTimeMillis()
                viewModelScope.launch {
                    historyRepository.addHistoryItem(addressItem)
                }
                callback.invoke(addressItem)
            }

            else -> {
                searchUICore.selectSearchResult(item.sr)
                val txt = searchUICore.phrase?.getText(true)
                if (item.sr.objectType == ObjectType.CITY || item.sr.objectType == ObjectType.VILLAGE || item.sr.objectType == ObjectType.POSTCODE) {
                    _uiState.update { it.copy(searchCity = item.sr.localeName) }
                    if (!citySearchHistory.history.contains(item)) {
                        citySearchHistory.history.add(0, item)
                    }
                }
                if (item.sr.objectType == ObjectType.STREET) _uiState.update { it.copy(searchStreet = item.sr.localeName) }
                callback.invoke(null)

                runCoreSearchInternal(txt.toString(), true)//research
            }
        }
    }

    fun runSearch(query: String) {
        if (uiState.value.searchItem?.itemType == SearchItemType.POSTCODE) {
            runCoreSearchInternal(query)
            return
        }
        if (searchItems.isNotEmpty()) filterInternalList(query, true)
        else runCoreSearchInternal(query)
    }

    private suspend fun composeTextSearch(queryText: String , addressSearch: Boolean): String {
        return queryText.takeIf { addressSearch }
                ?: geocodingRepository
                    .searchAddress(location)
                    .getOrNull()
                    ?.postcode
                    ?.getOrNull(0)
                    ?.toString()
                    .orEmpty()

    }

    private fun runCoreSearchInternal(
        text: String,
        addressSearch: Boolean = false
    ) = viewModelScope.launch(Dispatchers.IO) {
        val searchTypes = when (uiState.value.searchItem?.itemType) {
            SearchItemType.CITY -> {
                if (!addressSearch) listOf(ObjectType.CITY)
                else listOf(
                    ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE, ObjectType.HOUSE, ObjectType.STREET_INTERSECTION,
                    ObjectType.STREET, ObjectType.LOCATION, ObjectType.PARTIAL_LOCATION
                )
            }

            SearchItemType.POSTCODE -> if (!addressSearch) listOf(ObjectType.POSTCODE)
            else listOf(ObjectType.HOUSE, ObjectType.STREET)

            else -> emptyList()
        }
        val settings = searchUICore.searchSettings
            .setEmptyQueryAllowed(
                searchTypes.contains(ObjectType.POSTCODE)
                        || searchTypes.contains(ObjectType.CITY)
                        || searchTypes.contains(ObjectType.VILLAGE)
            )
            .setSearchTypes(*searchTypes.toTypedArray())
            .setRadiusLevel(uiState.value.radius)
        searchUICore.updateSettings(settings)

        val searchText = try {
            when {
                searchTypes.contains(ObjectType.CITY) -> composeTextSearch(text, addressSearch)
                searchTypes.contains(ObjectType.POSTCODE) -> composeTextSearch(text, true)
                else -> text.takeIf { addressSearch }.orEmpty()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        searchUICore.search(
            searchText.toString(),
            true,
            object : ResultMatcher<SearchResult> {
                val results = mutableListOf<SearchCityItem>()
                override fun publish(searchResult: SearchResult): Boolean {
                    when (searchResult.objectType) {
                        ObjectType.SEARCH_STARTED -> {
                            Timber.i("Started")
                            _uiState.update { it.copy(isSearching = true) }
                        }

                        ObjectType.SEARCH_FINISHED -> {
                            Timber.i("Search finished")
                            try {
                                searchItems.clear()
                                if (!addressSearch) {
                                    val filteredHistory =
                                        citySearchHistory.history.filter {
                                            it.localName.contains(text, true) || getCategory(it.sr, localizationHelper).contains(
                                                text,
                                                true
                                            )
                                        }
                                    searchItems.addAll(filteredHistory)
                                }

                                searchItems.addAll(results)
                                val modifiedSearchItems = searchItems.map {
                                    when (it.sr.objectType) {
                                        ObjectType.POI, ObjectType.LOCATION, ObjectType.HOUSE, ObjectType.FAVORITE, ObjectType.RECENT_OBJ, ObjectType.WPT, ObjectType.STREET_INTERSECTION, ObjectType.GPX_TRACK -> {
                                            it.copy(distance = it.distance * METERS_IN_KILOMETER)
                                        }
                                        else -> it
                                    }
                                }
                                _uiState.update { state ->
                                    state.copy(
                                        searchItems = searchItems.sortedBy { it.distance }
                                            .distinctBy { it.localName.removeDiacriticalMarks() },
                                        isSearching = false
                                    )
                                }
                                filterInternalList(uiState.value.searchText, false)
                            } catch (e: Exception) {
                                Timber.e(e)
                            }
                        }

                        ObjectType.FILTER_FINISHED -> {
                            Timber.i("Filter finished")
                        }

                        ObjectType.SEARCH_API_FINISHED -> {
                            Timber.i("Search api finished")

                        }

                        ObjectType.SEARCH_API_REGION_FINISHED -> {
                            Timber.i("Search api region finished")
                        }

                        ObjectType.PARTIAL_LOCATION -> {
                            Timber.i("Partial location")

                        }

                        else -> {
                            val mes = floatArrayOf(0f, 0f)
                            val toLoc = searchResult.location
                            toLoc?.let {
                                Location.distanceBetween(toLoc.latitude, toLoc.longitude, location.latitude, location.longitude, mes)
                            }
                            val km = mes[0].toDouble() / 1000

                            /**
                             * FIXME:
                             *  Workaround for now. Fetching offline cities does not apply radius filter and returns all cities available for region.
                             *  Need to find a better way to filter out results based on bbox and radius in SearchCoreFactory.
                             *  Should work similar to how it is done in POI search using getRadiusLevel() * 25, but this approach is ignored for offline cities.
                             *  See [SearchCoreFactory.initAndSearchCities()]
                             */
                            if (km > uiState.value.radius * 25) return false

                            val item = SearchCityItem(
                                localName = searchResult.localeName,
                                desc = searchResult.parentSearchResult?.localeName ?: "",
                                distance = km,
                                lat = searchResult.location?.latitude ?: 0.0,
                                lon = searchResult.location?.longitude ?: 0.0,
                                sr = searchResult
                            )
                            var allowed = true
                            citySearchHistory.history.forEach {
                                if (it.localName == item.localName
                                    && it.lat == item.lat
                                    && it.lon == item.lon
                                ) {
                                    allowed = false
                                }
                            }
                            if (allowed) {
                                results.add(item)
                            }
                        }
                    }
                    return true
                }

                override fun isCancelled(): Boolean {
                    return false
                }
            },
            settings
        )
    }

    fun selectPage(number: Int) {
        _uiState.update { it.copy(selectedPage = number) }
    }

    private fun filterInternalList(text: String, resetPage: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                searchItems = searchItems.filter { it.localName.contains(text, true) }.sortedBy { it.distance },
                selectedPage = if (resetPage) 1 else state.selectedPage
            )
        }
    }

    private fun SearchCityItem.toAddress() = SearchItemData.Address(
        id = 0,
        searchQuery = localName,
        title = uiState.value.searchCity.orEmpty(),
        address = uiState.value.searchStreet.orEmpty() + " " + uiState.value.searchHouse.orEmpty(),
        desc = uiState.value.searchCity.orEmpty(),
        distance = distance * METERS_IN_KILOMETER,
        lat = lat,
        lon = lon,
        iconRes = com.mudita.map.common.R.drawable.mx_mudita_city
    )

    private fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchText = query) }
    }

    data class SearchState(
        val isSearching: Boolean = false,
        val searchItems: List<SearchCityItem> = emptyList(),
        val searchItem: SearchItem? = null,
        val searchCityItem: SearchCityItem? = null,
        val searchCity: String? = null,
        val searchStreet: String? = null,
        val searchHouse: String? = null,
        val radius: Int = 6,
        val searchText: String = "",
        val selectedPage: Int = 1,
    )

    sealed class Intent {
        data class SearchQueryChange(val value: String) : Intent()
    }

    companion object {
        private const val METERS_IN_KILOMETER = 1000
        const val ITEMS_PER_PAGE = 7
    }

}