package com.mudita.maps.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY search_time DESC")
    fun getAllHistory(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY search_time DESC LIMIT 14")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE search_query LIKE '%' || :query || '%' ORDER BY search_time DESC LIMIT 14")
    fun getHistoryByQuery(query: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE search_query LIKE :query")
    suspend fun deleteHistoryByQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun deleteAllHistory()


}