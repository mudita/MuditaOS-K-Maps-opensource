package com.mudita.map.common.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.mudita.map.common.R
import com.mudita.map.common.utils.DESCRIPTION_SEPARATOR
import com.mudita.map.common.utils.capitalize
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.map.common.utils.formattedCoordinates
import kotlinx.parcelize.Parcelize
import net.osmand.data.LatLon
import java.util.UUID

@Parcelize
data class SearchItem (
    val id: UUID? = null,
    val localName: String? = null,
    val desc: String? = null,
    val address: String? = null,
    val distance: Double? = null,
    val latLon: LatLon,
    @DrawableRes val icon: Int = R.drawable.icon_search,
    val itemType: SearchItemType,
    val categoryKeyName: String? = null,
    val subCategoryKeyName: String? = null,
    val typeName: String? = null,
): Parcelable {

    val formattedTitle: String get() = localName?.replace("_", " ")?.capitalize()?.takeUnless { it.isBlank() } ?: latLon.formattedCoordinates()

    private val formattedDescription: String
        get() = StringBuilder().apply {
            desc?.let {
                val formattedDesc = it.replace("_", " ")
                if (formattedDesc.equals(formattedTitle, true).not()) {
                    append(formattedDesc.capitalize())
                }
            }
        }.toString()

    fun formattedDesc(osmAndFormatter: OsmAndFormatter): String = StringBuilder().apply {
        if (typeName.isNullOrBlank().not()) {
            append("$typeName")
        }

        if (address.isNullOrBlank().not()) {
            if (this.isBlank().not()) append(DESCRIPTION_SEPARATOR)
            append("$address")
        }
        else if (formattedDescription.isBlank().not()) {
            if (this.isBlank().not()) append(DESCRIPTION_SEPARATOR)
            append(formattedDescription)
        }

        if (distance != null) {
            if (this.isBlank().not()) append(DESCRIPTION_SEPARATOR)
            append(osmAndFormatter.getFormattedDistanceValue(distance.toFloat()).formattedValue)
        }
    }.toString()
}