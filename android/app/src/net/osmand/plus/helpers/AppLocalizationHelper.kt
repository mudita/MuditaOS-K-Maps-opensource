package net.osmand.plus.helpers

import com.mudita.maps.R as mainR
import android.content.Context
import com.mudita.map.common.model.LocalizationHelper
import com.mudita.maps.frontitude.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import net.osmand.data.City.CityType

class AppLocalizationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalizationHelper {
    override fun getCityTypeTranslation(cityType: CityType): String? =
        when (cityType) {
            CityType.CITY -> context.getString(R.string.maps_common_label_city)
            CityType.TOWN -> context.getString(R.string.maps_common_label_town)
            CityType.VILLAGE -> context.getString(R.string.maps_common_label_village)
            CityType.HAMLET -> context.getString(R.string.maps_common_label_hamlet)
            CityType.SUBURB -> context.getString(R.string.maps_common_label_suburb)
            CityType.DISTRICT -> context.getString(R.string.maps_common_label_district)
            CityType.NEIGHBOURHOOD -> context.getString(R.string.maps_common_label_neighbourhood)
            CityType.BOROUGH -> null
        }

    override fun getPostcodeTranslation(): String = context.getString(mainR.string.poi_ref_post)
}
