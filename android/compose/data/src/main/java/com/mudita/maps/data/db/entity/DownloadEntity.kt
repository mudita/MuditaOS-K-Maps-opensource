package com.mudita.maps.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity (

    @PrimaryKey
    @ColumnInfo(name = "filename")
    val filename: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "size")
    val size: String,

    @ColumnInfo(name = "target_size")
    val targetSize: String,

    @ColumnInfo(name = "parent")
    val parentName: String,
)