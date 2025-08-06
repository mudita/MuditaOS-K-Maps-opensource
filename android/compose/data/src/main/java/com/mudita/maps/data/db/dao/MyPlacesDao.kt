package com.mudita.maps.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mudita.maps.data.db.entity.MyPlacesEntity
import java.util.UUID

@Dao
interface MyPlacesDao {

    @Query("SELECT * FROM my_places ORDER BY timestamp")
    fun getAllMyPlaces(): List<MyPlacesEntity>
    @Query("SELECT * FROM my_places WHERE title LIKE :query OR description LIKE :query ORDER BY timestamp")
    fun getMyPlacesByQuery(query: String): List<MyPlacesEntity>
    @Query("SELECT * FROM my_places WHERE title = :title LIMIT 1")
    suspend fun getMyPlaceByTitle(title: String?): MyPlacesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMyPlace(item: MyPlacesEntity)

    @Update
    suspend fun updateMyPlace(item: MyPlacesEntity)

    @Query("DELETE FROM my_places WHERE id = :id")
    suspend fun deleteMyPlaceById(id: UUID)

    @Query("DELETE FROM my_places")
    suspend fun deleteAllMyPlaces()
}