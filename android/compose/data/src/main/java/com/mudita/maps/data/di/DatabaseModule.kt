package com.mudita.maps.data.di

import android.content.Context
import com.mudita.maps.data.db.MapsDatabaseRoomDatabase
import com.mudita.maps.data.db.converter.MapConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideYourDatabase(
        @ApplicationContext context: Context,
        mapConverter: MapConverter,
    ) = MapsDatabaseRoomDatabase.create(context, mapConverter)

    @Singleton
    @Provides
    fun provideSearchHistoryDao(db: MapsDatabaseRoomDatabase) = db.searchHistoryDao()
    @Singleton
    @Provides
    fun provideMyPlacesDao(db: MapsDatabaseRoomDatabase) = db.myPlacesDao()
    @Singleton
    @Provides
    fun provideDownloadsDao(db: MapsDatabaseRoomDatabase) = db.downloadsDao()
}