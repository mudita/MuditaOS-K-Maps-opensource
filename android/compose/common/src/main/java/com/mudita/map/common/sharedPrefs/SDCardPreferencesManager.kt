package com.mudita.map.common.sharedPrefs

import kotlinx.coroutines.flow.Flow

interface SDCardPreferencesManager {
    val isSDCardEnabled: Flow<Boolean>
    suspend fun updateSDCardEnabled(value: Boolean)
}