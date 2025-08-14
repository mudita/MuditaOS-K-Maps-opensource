package com.mudita.map.repository

import com.mudita.maps.data.db.entity.SearchHistoryEntity

interface HistoryRepository {

    suspend fun addHistoryItem(item: SearchHistoryEntity): Result<Unit>

}