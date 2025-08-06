package com.mudita.maps.data.di

import com.mudita.maps.data.api.DownloadApiService
import com.mudita.maps.data.api.RetrofitHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Singleton
    @Provides
    fun provideDownloadApiService() = RetrofitHelper.getInstance().create<DownloadApiService>()

}