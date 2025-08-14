package com.mudita.map.common.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.mudita.map.common.R
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.map.common.utils.capitalize
import com.mudita.map.common.utils.formattedCoordinates
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import net.osmand.data.City.CityType
import net.osmand.data.LatLon

sealed class SearchItemData {
    open val id: Int = 0
    open val distance: Double? = null
    open val lat: Double = 0.0
    open val lon: Double = 0.0
    open var searchTime: Long = 0L
    open val iconRes: Int = R.drawable.icon_search
    open val poiCategory: String? = null
    open val poiType: String? = null
    open val cityType: CityType? = null
    open val allNames: Map<String, String>? = null
    open val alternateName: String? = null
    abstract val itemType: SearchItemType

    abstract val title: String
    abstract val desc: String?
    abstract val address: String?

    @Parcelize
    data class History(
        val searchQuery: String,
        override val id: Int = 0,
        override val title: String,
        @DrawableRes override val iconRes: Int = R.drawable.icon_search,
        override val desc: String? = null,
        override val address: String? = null,
        override val distance: Double? = null,
        override val lat: Double = 0.0,
        override val lon: Double = 0.0,
        override val itemType: SearchItemType,
        override val poiCategory: String? = null,
        override val poiType: String? = null,
        override val cityType: CityType? = null,
        override val allNames: Map<String, String>? = null,
        override val alternateName: String? = null,
    ) : Parcelable, SearchItemData()

    @Parcelize
    data class Address(
        val searchQuery: String,
        override val id: Int,
        override val title: String,
        @DrawableRes override val iconRes: Int = R.drawable.icon_search,
        override val desc: String? = null,
        override val address: String? = null,
        override val distance: Double? = null,
        override val lat: Double = 0.0,
        override val lon: Double = 0.0,
    ) : Parcelable, SearchItemData() {
        @IgnoredOnParcel
        override val itemType: SearchItemType = SearchItemType.ADDRESS
    }

    data class Category(
        @DrawableRes override val iconRes: Int,
        override val title: String,
        override val desc: String?,
        override val address: String? = null,
        val isPoiFilter: Boolean = false,
    ) : SearchItemData() {
        @IgnoredOnParcel
        override val itemType: SearchItemType = SearchItemType.CATEGORY
    }

    data class SubCategory(
        @DrawableRes override val iconRes: Int,
        override val title: String,
        override val desc: String?,
        override val address: String? = null,
    ) : SearchItemData() {
        @IgnoredOnParcel
        override val itemType: SearchItemType = SearchItemType.SUB_CATEGORY
    }

    data class Poi(
        override val id: Int,
        @DrawableRes override val iconRes: Int,
        override val title: String,
        override val desc: String?,
        override val address: String? = null,
        override val distance: Double? = null,
        override val lat: Double = 0.0,
        override val lon: Double = 0.0,
        override val poiCategory: String? = null,
        override val poiType: String? = null,
    ) : SearchItemData() {
        @IgnoredOnParcel
        override val itemType: SearchItemType = SearchItemType.POI
    }

    val formattedTitle: String
        get() = title.replace("_", " ").capitalize().takeUnless { it.isBlank() }
            ?: LatLon(lat, lon).formattedCoordinates()

    val formattedDescription: String
        get() = StringBuilder().apply {
            address
                ?.takeUnless { it.isBlank() }
                ?.let { append(address) }
            if (!address.isNullOrBlank() && !desc.isNullOrBlank()) {
                append(", ")
            }
            desc?.let {
                val formattedDesc = it.replace("_", " ")
                if (formattedDesc.equals(title, true).not()) {
                    append(formattedDesc.capitalize())
                }
            }
        }.toString()

    fun formattedDistance(osmAndFormatter: OsmAndFormatter): String = StringBuilder().apply {
        distance?.let {
            if (!desc.isNullOrBlank()) append(" • ")
            append(osmAndFormatter.getFormattedDistanceValue(it.toFloat()).formattedValue)
        }
    }.toString()

    fun findByText(searchText: String) =
        title.contains(searchText, true) || desc?.contains(searchText, true) ?: false
}

/**
 *  Key names which need to remove from categories
 */
fun String.needToRemove(): Boolean = removeCategories.contains(this)
private val removeCategories = setOf(
    "sustenance",
    "personal_transport",
    "public_transport",
    "routes",
    "sightseeing",
    "sport",
    "shop",
    "finance",
)
//TODO "Nearest POIs" not exist, need to check Osmand part

val customCategoriesOrder = listOf(
    "accomodation",
    "tourism",
    "charging_station",
    "shop_food",
    "emergency",
    "filling_station",
    "healthcare",
    "entertainment",
    "cafe_and_restaurant",
    "seamark",
    "parking",
)

fun String.isPoiFilter(): Boolean = poiFiltersAsCategories.contains(this)
private val poiFiltersAsCategories = setOf(
    "accomodation",
    "charging_station",
    "filling_station",
    "parking",
)
