package com.mudita.maps.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mudita.maps.data.db.entity.DownloadEntity
import java.util.*

@Dao
interface DownloadsDao {

    @Query("SELECT * FROM downloads")
    suspend fun getDownloadsQueue(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDownload(item: DownloadEntity)

    @Query("DELETE FROM downloads WHERE filename = :filename")
    suspend fun deleteFromDownloads(filename: String)

    @Query("DELETE FROM downloads")
    suspend fun clearDownloadsQueue()

}