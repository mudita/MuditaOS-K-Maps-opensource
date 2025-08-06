package com.mudita.map.common.model

import net.osmand.data.City.CityType

interface LocalizationHelper {
    fun getCityTypeTranslation(cityType: CityType): String?
    fun getPostcodeTranslation(): String
}
