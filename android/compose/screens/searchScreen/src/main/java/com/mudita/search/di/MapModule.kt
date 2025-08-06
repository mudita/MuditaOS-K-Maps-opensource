package com.mudita.search.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.osmand.osm.MapPoiTypes
import net.osmand.search.SearchUICore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapModule {

    @Singleton
    @Provides
    fun providesMapPoiTypes() = MapPoiTypes(null).apply { init() }

    @Singleton
    @Provides
    fun providesSearchUICore(mapPoiTypes: MapPoiTypes) = SearchUICore(mapPoiTypes, "en", false).apply { init() }
}