package com.mudita.maptypes.repository

import com.mudita.map.common.model.MapTypeItem

interface MapTypesRepository {
    fun getMapTypesItems() : List<MapTypeItem>
}