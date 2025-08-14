package com.mudita.maps.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.osmand.data.City.CityType

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "search_query")
    val searchQuery: String,

    @ColumnInfo(name = "local_name")
    val localName: String,

    @ColumnInfo(name = "search_category")
    val searchCategory: String,

    @ColumnInfo(name = "latitude")
    val lat: Double,

    @ColumnInfo(name = "longitude")
    val lon: Double,

    @ColumnInfo(name = "search_time")
    val searchTime: Long,

    @ColumnInfo(name = "item_type")
    val itemType: String,

    @Deprecated("resource ids mustn't be used in any persistent storage")
    val iconRes: Int = 0,

    @ColumnInfo(name = "icon_resource_name")
    val iconResourceName: String? = null,

    @ColumnInfo(name = "poi_category")
    val poiCategory: String? = null,

    @ColumnInfo(name = "poi_type")
    val poiType: String? = null,

    @ColumnInfo(name = "city_type")
    val cityType: CityType? = null,

    @ColumnInfo(name = "all_names")
    val allNames: Map<String, String>? = null,

    @ColumnInfo(name = "alternate_name")
    val alternateName: String? = null,
)