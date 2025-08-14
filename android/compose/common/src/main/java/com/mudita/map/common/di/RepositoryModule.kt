package com.mudita.map.common.di

import com.mudita.map.common.repository.SettingsRepository
import com.mudita.map.common.repository.SettingsRepositoryImpl
import com.mudita.map.common.repository.geocoding.GeocodingRepository
import com.mudita.map.common.repository.geocoding.GeocodingRepositoryImpl
import com.mudita.map.common.sharedPrefs.MetricUnitPreference
import com.mudita.map.common.sharedPrefs.SettingsPreference
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindGeocodingRepository(repository: GeocodingRepositoryImpl): GeocodingRepository

    companion object {
        @Provides
        @Singleton
        fun provideSettingsRepository(
            metricUnitPreference: MetricUnitPreference,
            settingsPreference: SettingsPreference
        ): SettingsRepository {
            return SettingsRepositoryImpl(
                metricUnitPreference = metricUnitPreference,
                settingsPreference = settingsPreference
            )
        }
    }
}