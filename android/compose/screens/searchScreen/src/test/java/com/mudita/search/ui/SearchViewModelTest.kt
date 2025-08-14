package com.mudita.search.ui

import com.mudita.map.common.model.LocalizationHelper
import com.mudita.map.common.model.SearchItemData
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.search.repository.category.SearchCategoryRepository
import com.mudita.searchhistory.repository.HistoryRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.osmand.ResultMatcher
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.osm.MapPoiTypes
import net.osmand.osm.PoiCategory
import net.osmand.search.SearchUICore
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult
import net.osmand.search.core.SearchSettings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class SearchViewModelTest {
    @MockK private lateinit var historyRepository: HistoryRepository
    @MockK private lateinit var searchCategoryRepository: SearchCategoryRepository
    @MockK private lateinit var osmAndFormatter: OsmAndFormatter
    @MockK private lateinit var searchUICore: SearchUICore
    @MockK private lateinit var localizationHelper: LocalizationHelper

    private lateinit var searchViewModel: SearchViewModel
    private val testCoroutineScheduler = TestCoroutineScheduler()
    private val historyItem = SearchItemData.History(
        searchQuery = "query1",
        id = 1,
        iconRes = 1,
        title = "name1",
        desc = "",
        distance = 0.0,
        lat = 0.0,
        lon = 0.0,
        itemType = SearchItemType.HISTORY
    )
    private val currentLocation = LatLon( 52.0, 14.0)
    private val settings = SearchSettings(ArrayList())
        .setOriginalLocation(currentLocation)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        searchViewModel = SearchViewModel(
            historyRepository,
            searchCategoryRepository,
            osmAndFormatter,
            localizationHelper,
        )
        every { historyRepository.getHistory(currentLocation) } returns Result.success(flowOf(listOf(historyItem)))
        every { searchCategoryRepository.setupSearchCore(any()) } just runs
        every { searchUICore.searchSettings } returns settings
        every { searchUICore.updateSettings(any()) } just runs
        every { searchUICore.phrase } returns SearchPhrase.emptyPhrase(settings)
        every { searchUICore.isSearchMoreAvailable(any()) } returns false
        every { searchUICore.getNextSearchRadius(any()) } returns 0

        every { searchUICore.search(any(), any(), any()) } answers {
            val matcher = thirdArg<ResultMatcher<SearchResult>>()
            matcher.publish(SearchResult().apply {
                objectType = ObjectType.SEARCH_STARTED
            })
            matcher.publish(SearchResult().apply {
                objectType = ObjectType.POSTCODE
                localeName = "12-345"
            })
            matcher.publish(SearchResult().apply {
                objectType = ObjectType.SEARCH_API_REGION_FINISHED
                requiredSearchPhrase = SearchPhrase.emptyPhrase(settings)
            })
            matcher.publish(SearchResult().apply {
                objectType = ObjectType.SEARCH_FINISHED
            })
        }

        every { osmAndFormatter.calculateRoundedDist(any()) } returns 0.0
        every { localizationHelper.getPostcodeTranslation() } returns "Postcode"

        searchViewModel.setSearchUiCore(searchUICore)
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resetSearch clears the query`() = runTest {
        // Given
        searchViewModel.onSearchTextChanged(query = "query1")
        testCoroutineScheduler.advanceUntilIdle()

        // When
        searchViewModel.resetSearch()

        // Then
        assertEquals("", searchViewModel.searchState.value.searchText)
    }

    @Test
    fun `resetSearch shows search history`() = runTest {
        // Given
        searchViewModel.onSearchTextChanged("query4")
        testCoroutineScheduler.advanceUntilIdle()

        // When
        searchViewModel.resetSearch()

        // Then
        assertEquals(listOf(historyItem), searchViewModel.searchState.value.currentItems)
    }

    @Test
    fun `Show search history on empty query`() = runTest {
        // Given
        searchViewModel.onSearchTextChanged("query2")
        testCoroutineScheduler.advanceUntilIdle()

        // When
        searchViewModel.onSearchTextChanged(query = "")
        testCoroutineScheduler.advanceUntilIdle()

        // Then
        assertEquals(listOf(historyItem), searchViewModel.searchState.value.currentItems)
    }

    @Test
    fun `Screen receives results from searchUICore`() = runTest {
        // When
        searchViewModel.onSearchTextChanged("query3")
        testCoroutineScheduler.advanceUntilIdle()

        // Then
        val searchResults = searchViewModel.searchState.value.currentItems
        assertEquals(
            listOf(SearchItemData.History(
                searchQuery = "query3",
                id = -575616008,
                iconRes = searchResults.first().iconRes, // ignore icorRes, it may be automatically regenerated
                title = "12-345",
                desc = "Postcode",
                distance = 5910885.0,
                lat = 0.0,
                lon = 0.0,
                itemType = SearchItemType.POSTCODE
            )),
            searchResults
        )
    }

    @Test
    fun `Search results dont contain duplicates`() = runTest {
        // Given
        val searchResult = SearchResult().apply {
            objectType = ObjectType.POI
            localeName = "Oper Breslau"
            `object` = Amenity().apply {
                subType = "theatre"
                id = 739148623
                type = PoiCategory(MapPoiTypes.getDefault(), "entertainment", 0)
            }
            location = LatLon(51.105747, 17.030418)
        }
        val duplicate = SearchResult().apply {
            objectType = ObjectType.POI
            localeName = "Oper Breslau"
            `object` = Amenity().apply {
                subType = "building"
                id = 739148623
                type = PoiCategory(MapPoiTypes.getDefault(), "man_made", 0)
            }
            location = LatLon(51.105747, 17.030418)
        }
        every { searchUICore.search(any(), any(), any()) } answers {
            val matcher = thirdArg<ResultMatcher<SearchResult>>()
            matcher.publish(searchResult)
            matcher.publish(duplicate)
            matcher.publish(SearchResult().apply {
                objectType = ObjectType.SEARCH_API_REGION_FINISHED
                requiredSearchPhrase = SearchPhrase.emptyPhrase(settings)
            })
        }

        // When
        searchViewModel.onSearchTextChanged("Breslávia")
        testCoroutineScheduler.advanceUntilIdle()

        // Then
        val searchResults = searchViewModel.searchState.value.currentItems
        assertEquals(
            listOf(SearchItemData.History(
                searchQuery = "Breslávia",
                id = 577959927,
                iconRes = searchResults.first().iconRes, // ignore icorRes, it may be automatically regenerated
                title = "Oper Breslau",
                desc = "Theatre",
                distance = 232523.96875,
                lat = 51.105747,
                lon = 17.030418,
                itemType = SearchItemType.POI,
                poiCategory = "entertainment",
                poiType = "theatre"
            )),
            searchResults
        )
    }

    @Test
    fun `Progress indicator appears when search starts`() {
        // Given
        every { searchUICore.search(any(), any(), any()) } answers {
            val matcher = thirdArg<ResultMatcher<SearchResult>>()
            matcher.publish(SearchResult().apply {
                objectType = ObjectType.SEARCH_STARTED
            })
        }

        // When
        searchViewModel.onSearchTextChanged("query10")
        testCoroutineScheduler.advanceUntilIdle()

        // Then
        assertTrue(searchViewModel.searchState.value.searchInProgress)
    }

    @Test
    fun `Progress indicator stops when search finishes`() {
        // Given
        // ResultMatcher configured in setup()

        // When
        searchViewModel.onSearchTextChanged("query11")
        testCoroutineScheduler.advanceUntilIdle()

        // Then
        assertFalse(searchViewModel.searchState.value.searchInProgress)
    }
}