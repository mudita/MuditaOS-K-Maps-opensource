package com.mudita.map.common.sharedPrefs

interface SettingsPreference {
    fun getSoundEnabled(): Boolean
    fun setSoundEnabled(enabled: Boolean)
    fun registerOnSoundChangedListener(onSoundEnabled: (Boolean) -> Unit)
    fun unregisterOnSoundChangedListener(onSoundEnabled: (Boolean) -> Unit)

    fun getScreenAlwaysOnEnabled(): Boolean
    fun setScreenAlwaysOnEnabled(enabled: Boolean)

    fun getWifiOnlyEnabled(): Boolean
    fun setWifiOnlyEnabled(enabled: Boolean)
}