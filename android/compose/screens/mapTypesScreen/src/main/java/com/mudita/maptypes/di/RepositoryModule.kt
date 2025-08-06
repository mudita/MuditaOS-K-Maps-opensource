package com.mudita.maptypes.di

import com.mudita.maptypes.repository.MapTypesRepository
import com.mudita.maptypes.repository.MapTypesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindMapTypesRepository(impl: MapTypesRepositoryImpl): MapTypesRepository
}