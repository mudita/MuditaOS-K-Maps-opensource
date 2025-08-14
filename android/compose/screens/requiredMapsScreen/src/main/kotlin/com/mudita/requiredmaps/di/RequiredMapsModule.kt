package com.mudita.requiredmaps.di

import androidx.lifecycle.SavedStateHandle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mudita.map.common.navigation.Screen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
interface RequiredMapsModule {
    companion object {

        @Suppress("UNCHECKED_CAST")
        @Provides
        fun provideMissingMapNames(savedStateHandle: SavedStateHandle, gson: Gson): List<String> {
            val token = TypeToken.getParameterized(List::class.java, String::class.java) as TypeToken<List<String>>
            return savedStateHandle.get<String>(Screen.MAP_NAMES)
                ?.let { json -> gson.fromJson(json, token) }
                ?: emptyList()
        }
    }
}
