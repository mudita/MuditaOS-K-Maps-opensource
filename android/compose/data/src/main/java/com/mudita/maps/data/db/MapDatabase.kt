package com.mudita.maps.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mudita.maps.data.db.converter.MapConverter
import com.mudita.maps.data.db.dao.DownloadsDao
import com.mudita.maps.data.db.dao.MyPlacesDao
import com.mudita.maps.data.db.dao.SearchHistoryDao
import com.mudita.maps.data.db.entity.DownloadEntity
import com.mudita.maps.data.db.entity.MyPlacesEntity
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import com.mudita.maps.data.db.migration.AutoMigration4to5

@Database(
    version = 5,
    entities = [
        SearchHistoryEntity::class,
        MyPlacesEntity::class,
        DownloadEntity::class
    ],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(4, 5, AutoMigration4to5::class),
    ]
)
@TypeConverters(MapConverter::class)
abstract class MapsDatabaseRoomDatabase : RoomDatabase() {

    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun myPlacesDao(): MyPlacesDao
    abstract fun downloadsDao(): DownloadsDao

    companion object {
        private const val DATABASE_NAME = "mudita_kompakt_map.db"

        fun create(context: Context, mapConverter: MapConverter) = Room.databaseBuilder(
            context,
            MapsDatabaseRoomDatabase::class.java,
            DATABASE_NAME
        )
            .addTypeConverter(mapConverter)
            .fallbackToDestructiveMigration()
            .build()
    }
}
