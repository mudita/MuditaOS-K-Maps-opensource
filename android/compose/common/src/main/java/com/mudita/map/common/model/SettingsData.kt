package com.mudita.map.common.model

import com.mudita.map.common.enums.MetricsConstants

enum class StorageType {
    SD_CARD,
    PHONE
}

data class SettingsData(
    val soundEnabled: Boolean = false,
    val screenAlwaysOn: Boolean = false,
    val wifiOnlyEnabled: Boolean = false,
    val sdCardExists: Boolean = false,
    val storageType: StorageType = StorageType.PHONE,
    val distanceUnits: MetricsConstants = MetricsConstants.KILOMETERS_AND_METERS
)
