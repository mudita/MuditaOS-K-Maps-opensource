package com.mudita.map.common.repository

import com.mudita.map.common.enums.MetricsConstants
import com.mudita.map.common.model.SettingItem
import com.mudita.map.common.model.SettingItemAction

interface SettingsRepository {
    fun getSettingsItems(hasSDCard: Boolean): List<SettingItem>
    fun saveMetricUnits(metricUnits: MetricsConstants)

    fun saveSoundEnabled(enabled: Boolean)
    fun getSoundEnabled(): Boolean
    fun registerOnSoundChangedListener(onSoundEnabled: (Boolean) -> Unit)
    fun unregisterOnSoundChangedListener(onSoundEnabled: (Boolean) -> Unit)

    fun saveScreenAlwaysOnEnabled(enabled: Boolean)
    fun getScreenAlwaysOn(): Boolean

    fun saveWifiOnlyEnabled(enabled: Boolean)
}
