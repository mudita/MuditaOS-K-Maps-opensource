package com.mudita.map.common.sharedPrefs

import android.content.SharedPreferences
import javax.inject.Inject

class SettingsPreferencesImpl @Inject constructor(private val sharedPreferences: SharedPreferences) : SettingsPreference {

    companion object {
        const val SOUND_KEY = "sound"
        const val SCREEN_ALWAYS_ON_KEY = "screen_always_on"
        const val WIFI_ONLY_KEY = "wifi_only"
    }

    private val soundChangedListeners = mutableListOf<(Boolean) -> Unit>()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == SOUND_KEY && soundChangedListeners.isNotEmpty()) {
            val soundEnabled = sharedPreferences.getBoolean(SOUND_KEY, getSoundEnabled())
            soundChangedListeners.forEach { it(soundEnabled) }
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun getSoundEnabled(): Boolean = sharedPreferences.getBoolean(SOUND_KEY, true)

    override fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(SOUND_KEY, enabled).apply()
    }

    override fun registerOnSoundChangedListener(onSoundEnabled: (Boolean) -> Unit) {
        soundChangedListeners.add(onSoundEnabled)
    }

    override fun unregisterOnSoundChangedListener(onSoundEnabled: (Boolean) -> Unit) {
        soundChangedListeners.remove(onSoundEnabled)
    }

    override fun getScreenAlwaysOnEnabled(): Boolean = sharedPreferences.getBoolean(SCREEN_ALWAYS_ON_KEY, true)

    override fun setScreenAlwaysOnEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(SCREEN_ALWAYS_ON_KEY, enabled).apply()
    }

    override fun getWifiOnlyEnabled(): Boolean = sharedPreferences.getBoolean(WIFI_ONLY_KEY, true)

    override fun setWifiOnlyEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(WIFI_ONLY_KEY, enabled).apply()
    }
}