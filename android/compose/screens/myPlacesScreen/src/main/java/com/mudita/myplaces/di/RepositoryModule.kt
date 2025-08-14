package com.mudita.myplaces.di

import com.mudita.myplaces.repository.MyPlacesRepository
import com.mudita.myplaces.repository.MyPlacesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMyPlacesRepository(impl: MyPlacesRepositoryImpl): MyPlacesRepository
}