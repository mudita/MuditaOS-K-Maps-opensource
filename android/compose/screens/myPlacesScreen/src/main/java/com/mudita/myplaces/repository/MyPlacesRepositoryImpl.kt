package com.mudita.myplaces.repository

import com.mudita.map.common.model.MyPlaceItem
import com.mudita.maps.data.db.dao.MyPlacesDao
import com.mudita.myplaces.repository.mapper.toItems
import com.mudita.myplaces.repository.mapper.toMyPlacesEntity
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyPlacesRepositoryImpl @Inject constructor(
    private val myPlacesDao: MyPlacesDao
) : MyPlacesRepository {

    override suspend fun getMyPlaces(): Result<List<MyPlaceItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                myPlacesDao.getAllMyPlaces().toItems()
            }
        }

    override suspend fun addMyPlace(myPlaceItem: MyPlaceItem): Result<MyPlaceItem?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val myPlace = myPlacesDao.getMyPlaceByTitle(myPlaceItem.title)

                if (myPlace != null) {
                    myPlacesDao.deleteMyPlaceById(myPlace.id)
                    null
                } else {
                    myPlacesDao.addMyPlace(myPlaceItem.toMyPlacesEntity())
                    myPlaceItem
                }
            }
        }

    override suspend fun updateMyPlace(myPlaceItem: MyPlaceItem): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                myPlacesDao.updateMyPlace(myPlaceItem.toMyPlacesEntity())
            }
        }


    override suspend fun searchMyPlaces(text: String): Result<List<MyPlaceItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                myPlacesDao.getMyPlacesByQuery("%$text%").toItems()
            }
        }

    override suspend fun clearDatabase(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                myPlacesDao.deleteAllMyPlaces()
            }
        }

    override suspend fun deleteMyPlace(myPlaceItem: MyPlaceItem) =
        withContext(Dispatchers.IO) {
            runCatching {
                myPlacesDao.deleteMyPlaceById(myPlaceItem.id)
            }
        }
}
