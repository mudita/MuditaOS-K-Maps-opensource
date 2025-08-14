package com.mudita.search.repository

import android.content.Context
import com.mudita.map.common.model.LocalizationHelper
import com.mudita.maps.data.db.dao.SearchHistoryDao
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import com.mudita.searchhistory.repository.HistoryRepositoryImpl
import com.mudita.searchhistory.repository.mapper.SearchHistoryMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.osmand.data.LatLon
import net.osmand.util.MapUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class HistoryRepositoryImplTest {

    private val scheduler = TestCoroutineScheduler()
    private val searchHistoryDao = mock(SearchHistoryDao::class.java)
    private val context = mock(Context::class.java)
    private val localizationHelper = mock(LocalizationHelper::class.java)
    private val mapper = SearchHistoryMapper(context, localizationHelper)
    private val historyRepository = HistoryRepositoryImpl(searchHistoryDao, mapper, UnconfinedTestDispatcher(scheduler))

    @Test
    fun `getHistory should return history`() = runTest(scheduler) {
        val testHistoryFlow = flowOf(testHistory)

        `when`(searchHistoryDao.getAllHistory()).thenReturn(testHistoryFlow)

        val history = historyRepository.getHistory(LatLon.zero)

        assertEquals(testHistoryFlow.first().first().id, history.getOrNull()?.first()?.first()?.id)
    }

    @Test
    fun `getHistory should return history with the updated distance`() = runTest(scheduler) {
        val testHistoryFlow = flowOf(testHistory)
        val searchLocation = LatLon(50.0, 50.0)
        val expectedDistance = testHistory.map {
            MapUtils.getDistance(searchLocation, LatLon(it.lat, it.lon))
        }

        `when`(searchHistoryDao.getAllHistory()).thenReturn(testHistoryFlow)

        val history = historyRepository.getHistory(searchLocation)

        assertEquals(expectedDistance, history.getOrNull()?.first()?.map { it.distance })
    }

    @Test
    fun `getHistoryByQuery should return history`() = runTest(scheduler) {
        val testHistoryFlow = flowOf(testHistory)

        `when`(searchHistoryDao.getHistoryByQuery("query")).thenReturn(testHistoryFlow)

        val history = historyRepository.getHistoryByQuery("query")

        assertEquals(testHistoryFlow.first().first().id, history.getOrNull()?.first()?.first()?.id)
    }

    companion object {
        val testHistory = listOf(
            SearchHistoryEntity(1, "query1", "name1", "", 51.0, 55.0, 0, ""),
            SearchHistoryEntity(2, "query2", "name2", "", 52.0, 54.0, 0, ""),
            SearchHistoryEntity(3, "query3", "name3", "", 53.0, 53.0, 0, ""),
        )
    }
}