package com.mudita.map.common.sharedPrefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val SD_CARD_PREFERENCES = "sd_card_preferences"
const val SD_CARD_KEY = "sd_card_key"

val Context.dataStore by preferencesDataStore(name = SD_CARD_PREFERENCES)

class SDCardPreferencesManagerImpl(private val context: Context) : SDCardPreferencesManager {

    companion object {
        private val SD_CARD = booleanPreferencesKey(SD_CARD_KEY)
    }

    override val isSDCardEnabled: Flow<Boolean>
        get() = context.dataStore.data.map { it[SD_CARD] ?: false }

    override suspend fun updateSDCardEnabled(value: Boolean) {
        context.dataStore.edit { it[SD_CARD] = value }
    }
}