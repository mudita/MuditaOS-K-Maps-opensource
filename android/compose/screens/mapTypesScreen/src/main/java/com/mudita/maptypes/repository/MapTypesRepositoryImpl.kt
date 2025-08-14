package com.mudita.maptypes.repository

import com.mudita.map.common.model.MapTypeItem
import javax.inject.Inject

class MapTypesRepositoryImpl @Inject constructor() : MapTypesRepository {
    override fun getMapTypesItems(): List<MapTypeItem> {
        return listOf(MapTypeItem.Driving(), MapTypeItem.Walking(), MapTypeItem.Cycling())
    }
}