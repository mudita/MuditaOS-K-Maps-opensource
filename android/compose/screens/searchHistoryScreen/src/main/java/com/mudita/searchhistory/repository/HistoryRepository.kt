package com.mudita.searchhistory.repository

import com.mudita.map.common.model.SearchItemData
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import net.osmand.data.LatLon

interface HistoryRepository {
    fun getHistory(userLatLon: LatLon): Result<Flow<List<SearchItemData>>>
    fun getHistoryByQuery(query: String): Result<Flow<List<SearchHistoryEntity>>>
    suspend fun addHistoryItem(item: SearchItemData): Result<Unit>
    suspend fun clearHistory(): Result<Unit>
}
