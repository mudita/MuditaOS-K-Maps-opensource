package com.mudita.myplaces.repository

import com.mudita.map.common.model.MyPlaceItem


interface MyPlacesRepository {

    suspend fun getMyPlaces(): Result<List<MyPlaceItem>>
    suspend fun addMyPlace(myPlaceItem: MyPlaceItem): Result<MyPlaceItem?>
    suspend fun updateMyPlace(myPlaceItem: MyPlaceItem): Result<Unit>
    suspend fun searchMyPlaces(text: String): Result<List<MyPlaceItem>>
    suspend fun clearDatabase(): Result<Unit>
    suspend fun deleteMyPlace(myPlaceItem: MyPlaceItem): Result<Unit>
}
