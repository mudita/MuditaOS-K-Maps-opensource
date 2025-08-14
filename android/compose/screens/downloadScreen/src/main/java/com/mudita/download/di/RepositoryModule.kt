package com.mudita.download.di

import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.DownloadRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindIndexesRepository(impl: DownloadRepositoryImpl): DownloadRepository
}