package com.mudita.map.common.sharedPrefs

interface MetricUnitPreference {
    fun getMetricUnit(): String
    fun setMetricUnit(metricUnit: String)
}