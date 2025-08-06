package com.mudita.map.repository

import com.mudita.maps.data.db.dao.SearchHistoryDao
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : HistoryRepository {

    override suspend fun addHistoryItem(item: SearchHistoryEntity) = runCatching {
        searchHistoryDao.addHistory(item)
    }
}