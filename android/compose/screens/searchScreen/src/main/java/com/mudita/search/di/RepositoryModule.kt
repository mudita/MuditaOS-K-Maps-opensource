package com.mudita.search.di

import com.mudita.search.repository.category.SearchCategoryRepository
import com.mudita.search.repository.category.SearchCategoryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindSearchCategoryRepository(impl: SearchCategoryRepositoryImpl): SearchCategoryRepository
}