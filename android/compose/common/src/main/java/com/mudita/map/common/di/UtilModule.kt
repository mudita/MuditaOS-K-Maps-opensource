package com.mudita.map.common.di

import com.google.gson.Gson
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.memory.MemoryManagerImpl
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface UtilModule {

    @Binds
    @Singleton
    fun bindMemoryManager(memoryManager: MemoryManagerImpl): MemoryManager

    @Binds
    @Singleton
    fun bindNetworkManager(networkManager: NetworkManagerImpl): NetworkManager

    companion object {

        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()
    }
}