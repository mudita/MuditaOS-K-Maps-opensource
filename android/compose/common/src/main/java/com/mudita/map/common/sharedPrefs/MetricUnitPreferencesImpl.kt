package com.mudita.map.common.sharedPrefs

import android.content.SharedPreferences
import com.mudita.map.common.enums.MetricsConstants
import javax.inject.Inject

class MetricUnitPreferencesImpl @Inject constructor(private val sharedPreferences: SharedPreferences) : MetricUnitPreference {

    companion object {
        const val METRIC_UNIT_KEY = "metric_unit"
    }

    override fun getMetricUnit(): String {
        val default = MetricsConstants.KILOMETERS_AND_METERS.toTTSString()
        return sharedPreferences.getString(METRIC_UNIT_KEY, default) ?: default
    }

    override fun setMetricUnit(metricUnit: String) {
        sharedPreferences.edit().putString(METRIC_UNIT_KEY, metricUnit).apply()
    }
}