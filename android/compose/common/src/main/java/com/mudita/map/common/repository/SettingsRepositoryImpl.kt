package com.mudita.map.common.repository

import com.mudita.map.common.BuildConfig
import com.mudita.map.common.enums.MetricsConstants
import com.mudita.map.common.model.SettingItem
import com.mudita.map.common.model.SettingItemAction
import com.mudita.map.common.sharedPrefs.MetricUnitPreference
import com.mudita.map.common.sharedPrefs.SettingsPreference
import com.mudita.maps.frontitude.R
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val metricUnitPreference: MetricUnitPreference,
    private val settingsPreference: SettingsPreference
) : SettingsRepository {

    private val metricUnit get() = MetricsConstants.fromTTSString(metricUnitPreference.getMetricUnit())

    override fun getSettingsItems(hasSDCard: Boolean): List<SettingItem> = listOfNotNull(
        if (BuildConfig.IS_PLAN_ROUTE_ENABLED) {
            SettingItem.ScreenAlwaysOn(
                title = R.string.common_toggle_button_h1_screenalwayson,
                desc = R.string.maps_menu_toggle_button_body_stopsthescreenlocking,
                isChecked = settingsPreference.getScreenAlwaysOnEnabled()
            )
        } else {
            null
        },
        SettingItem.WifiOnly(
            title = R.string.common_toggle_button_h1_wifionly,
            desc = R.string.maps_menu_toggle_button_body_offlinemapswill,
            isChecked = settingsPreference.getWifiOnlyEnabled()
        ),
        SettingItem.Storage(
            title = R.string.common_menuitem_h1_storage,
            options = storageOptions(hasSDCard)
        ),
        SettingItem.DistanceUnits(
            title = R.string.common_label_distanceunits,
            desc = metricUnit.toStringRes(),
            options = listOf(
                SettingItemAction.Checkable.CheckableItem.Miles(metricUnit == MetricsConstants.MILES_AND_FEET),
                SettingItemAction.Checkable.CheckableItem.Kilometers(metricUnit == MetricsConstants.KILOMETERS_AND_METERS),
            )
        ),
    )

    override fun saveMetricUnits(metricUnits: MetricsConstants) {
        metricUnitPreference.setMetricUnit(metricUnits.toTTSString())
    }

    override fun saveSoundEnabled(enabled: Boolean) {
        settingsPreference.setSoundEnabled(enabled)
    }

    override fun getSoundEnabled(): Boolean = settingsPreference.getSoundEnabled()

    override fun registerOnSoundChangedListener(onSoundEnabled: (Boolean) -> Unit) {
        settingsPreference.registerOnSoundChangedListener(onSoundEnabled)
    }

    override fun unregisterOnSoundChangedListener(onSoundEnabled: (Boolean) -> Unit) {
        settingsPreference.unregisterOnSoundChangedListener(onSoundEnabled)
    }

    override fun saveScreenAlwaysOnEnabled(enabled: Boolean) {
        settingsPreference.setScreenAlwaysOnEnabled(enabled)
    }

    override fun getScreenAlwaysOn(): Boolean = settingsPreference.getScreenAlwaysOnEnabled()

    override fun saveWifiOnlyEnabled(enabled: Boolean) {
        settingsPreference.setWifiOnlyEnabled(enabled)
    }
}

private fun storageOptions(hasSDCard: Boolean = false) =
    listOf(
        SettingItemAction.Checkable.CheckableItem.Card(isAvailable = hasSDCard),
        SettingItemAction.Checkable.CheckableItem.Phone(),
    )
