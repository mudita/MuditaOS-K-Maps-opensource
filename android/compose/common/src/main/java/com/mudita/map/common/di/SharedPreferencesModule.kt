package com.mudita.map.common.di

import android.content.Context
import android.content.SharedPreferences
import com.mudita.map.common.sharedPrefs.AppFirstRunPreference
import com.mudita.map.common.sharedPrefs.AppFirstRunPreferenceImpl
import com.mudita.map.common.sharedPrefs.MapTypePreferenceImpl
import com.mudita.map.common.sharedPrefs.MapTypesPreference
import com.mudita.map.common.sharedPrefs.MetricUnitPreference
import com.mudita.map.common.sharedPrefs.MetricUnitPreferencesImpl
import com.mudita.map.common.sharedPrefs.SDCardPreferencesManager
import com.mudita.map.common.sharedPrefs.SDCardPreferencesManagerImpl
import com.mudita.map.common.sharedPrefs.SettingsPreference
import com.mudita.map.common.sharedPrefs.SettingsPreferencesImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedPreferencesModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext application: Context): SharedPreferences {
        return application.getSharedPreferences("mudita_map_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideMetricUnitPreference(sharedPreferences: SharedPreferences): MetricUnitPreference = MetricUnitPreferencesImpl(sharedPreferences)

    @Provides
    @Singleton
    fun provideSettingsPreferences(sharedPreferences: SharedPreferences): SettingsPreference = SettingsPreferencesImpl(sharedPreferences)

    @Provides
    @Singleton
    fun provideMapTypesPreference(sharedPreferences: SharedPreferences): MapTypesPreference = MapTypePreferenceImpl(sharedPreferences)

    @Provides
    @Singleton
    fun provideAppFirstRunPreference(sharedPreferences: SharedPreferences): AppFirstRunPreference = AppFirstRunPreferenceImpl(sharedPreferences)

    @Provides
    @Singleton
    fun provideSDCardPreferencesManager(@ApplicationContext context: Context): SDCardPreferencesManager = SDCardPreferencesManagerImpl(context)
}