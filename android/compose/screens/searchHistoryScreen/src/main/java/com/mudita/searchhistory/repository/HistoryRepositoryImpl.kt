package com.mudita.searchhistory.repository

import com.mudita.map.common.di.DispatcherQualifier
import com.mudita.map.common.model.SearchItemData
import com.mudita.maps.data.db.dao.SearchHistoryDao
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import com.mudita.searchhistory.repository.mapper.SearchHistoryMapper
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.osmand.data.LatLon

class HistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val mapper: SearchHistoryMapper,
    @DispatcherQualifier.IO private val dispatcher: CoroutineDispatcher,
) : HistoryRepository {

    override fun getHistory(userLatLon: LatLon): Result<Flow<List<SearchItemData>>> = runCatching {
        searchHistoryDao
            .getAllHistory()
            .map { mapper.toSearchItems(it, userLatLon) }
            .flowOn(dispatcher)
    }

    override fun getHistoryByQuery(query: String): Result<Flow<List<SearchHistoryEntity>>> = runCatching {
        searchHistoryDao.getHistoryByQuery(query)
            .flowOn(dispatcher)
    }

    override suspend fun addHistoryItem(item: SearchItemData) = runCatching {
        withContext(dispatcher) {
            searchHistoryDao.addHistory(mapper.toEntity(item))
        }
    }

    override suspend fun clearHistory() = runCatching {
        withContext(dispatcher) {
            searchHistoryDao.deleteAllHistory()
        }
    }
}