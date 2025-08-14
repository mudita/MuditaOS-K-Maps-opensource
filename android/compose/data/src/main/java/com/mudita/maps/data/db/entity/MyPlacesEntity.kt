package com.mudita.maps.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.UUID

@Entity(tableName = "my_places")
data class MyPlacesEntity (

    @PrimaryKey
    val id: UUID,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "description")
    val desc: String?,

    @ColumnInfo(name = "address")
    val address: String?,

    @ColumnInfo(name = "latitude")
    val latitude: Double?,

    @ColumnInfo(name = "longitude")
    val longitude: Double?,

    @ColumnInfo(name = "distance")
    val distance: Double?,

    @ColumnInfo(name = "item_type")
    val itemType: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)