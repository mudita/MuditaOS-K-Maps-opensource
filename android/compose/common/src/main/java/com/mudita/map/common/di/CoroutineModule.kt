package com.mudita.map.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
interface CoroutineModule {
    companion object {
        @Provides
        @DispatcherQualifier.IO
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @DispatcherQualifier.Default
        fun provideDefaultQualifier(): CoroutineDispatcher = Dispatchers.Default
    }
}

object DispatcherQualifier {
    @Qualifier
    annotation class IO

    @Qualifier
    annotation class Default
}
