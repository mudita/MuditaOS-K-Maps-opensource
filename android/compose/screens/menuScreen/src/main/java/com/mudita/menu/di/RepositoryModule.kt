package com.mudita.menu.di

import com.mudita.menu.repository.MenuRepository
import com.mudita.menu.repository.MenuRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSettingsRepository(impl: MenuRepositoryImpl): MenuRepository
}