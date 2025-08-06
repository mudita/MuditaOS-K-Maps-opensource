package com.mudita.map.common.utils

import com.mudita.map.common.R
import com.mudita.map.common.model.LocalizationHelper
import java.io.File
import java.util.Locale
import net.osmand.GPXUtilities
import net.osmand.data.Amenity
import net.osmand.data.City
import net.osmand.data.Street
import net.osmand.osm.AbstractPoiType
import net.osmand.osm.MapPoiTypes
import net.osmand.osm.PoiCategory
import net.osmand.osm.PoiFilter
import net.osmand.osm.PoiType
import net.osmand.search.core.CustomSearchPoiFilter
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms
import timber.log.Timber

const val POI_NAME_TYPE_DELIMITED = " • "

private const val defaultPoiIconName = "craft_default"

private val smallIcons: MutableMap<String, Int> = linkedMapOf()
private val bigIcons: MutableMap<String, Int> = linkedMapOf()

fun initIcons() {
    val cl = R.drawable::class.java
    for (f in cl.declaredFields) {
        try {
            if (f.name.startsWith("mm_")) {
                smallIcons[f.name.substring(3)] = f.getInt(null)
            } else if (f.name.startsWith("mx_")) {
                bigIcons[f.name.substring(3)] = f.getInt(null)
            }
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        } catch (e: IllegalAccessException) {
            Timber.e(e)
        }
    }
}

fun getPoiTypeIconName(abstractPoiType: AbstractPoiType?): String? {
    if (abstractPoiType != null && containsBigIcon(abstractPoiType.iconKeyName)) {
        return abstractPoiType.iconKeyName
    } else if (abstractPoiType is PoiType) {
        val iconId = abstractPoiType.osmTag + "_" + abstractPoiType.osmValue
        if (containsBigIcon(iconId)) {
            return iconId
        } else if (abstractPoiType.parentType != null) {
            return getPoiTypeIconName(abstractPoiType.parentType)
        }
    }
    return null
}

fun getIconNameForPoiType(poiType: PoiType): String? {
    if (containsSmallIcon(poiType.iconKeyName)) {
        return poiType.iconKeyName
    } else if (containsSmallIcon(poiType.osmTag + "_" + poiType.osmValue)) {
        return poiType.osmTag + "_" + poiType.osmValue
    }
    var iconName: String? = null
    if (poiType.parentType != null) {
        iconName = poiType.parentType.iconKeyName
    } else if (poiType.filter != null) {
        iconName = poiType.filter.iconKeyName
    } else if (poiType.category != null) {
        iconName = poiType.category.iconKeyName
    }
    return if (containsSmallIcon(iconName)) {
        iconName
    } else defaultPoiIconName
}

fun getBigIconResourceId(s: String?): Int {
    val i = bigIcons[s]
    return i ?: 0
}

fun getBigIconResourceIdOrNull(s: String?): Int? {
    return bigIcons[s]
}

fun getIconNameForAmenity(amenity: Amenity): String? {
    return if (amenity.type != null) "mudita_${amenity.type.iconKeyName}" else null
}

fun getCategory(searchResult: SearchResult, localizationHelper: LocalizationHelper): String {
    val typeName = getTypeName(searchResult, localizationHelper)
    val alternateName = searchResult.alternateName
    val prefix = if (alternateName != null && !alternateName.startsWith("http")) {
        searchResult.alternateName + POI_NAME_TYPE_DELIMITED
    } else {
        ""
    }
    return prefix + typeName
}

fun getIcon(searchResult: SearchResult): Int {
    var iconId = -1
    when (searchResult.objectType) {
        null -> return R.drawable.mx_mudita_default
        ObjectType.CITY ->  {
            val town = (searchResult.`object` as? City)?.type == City.CityType.TOWN
            return if (town) R.drawable.mx_mudita_town else R.drawable.mx_mudita_city
        }
        ObjectType.VILLAGE -> return R.drawable.mx_mudita_village
        ObjectType.POSTCODE, ObjectType.STREET -> return R.drawable.mx_mudita_street
        ObjectType.HOUSE -> return R.drawable.mx_mudita_house
        ObjectType.STREET_INTERSECTION -> return R.drawable.mm_mudita_traffic
        ObjectType.POI_TYPE -> {
            // TODO this needs to be updated in case POI categories icons are needed
            if (searchResult.`object` is AbstractPoiType) {
                var iconName: String? =
                    getPoiTypeIconName(searchResult.`object` as AbstractPoiType)
                if (Algorithms.isEmpty(iconName) && searchResult.`object` is PoiType) {
                    iconName = getIconNameForPoiType(searchResult.`object` as PoiType)
                }
                if (!Algorithms.isEmpty(iconName)) {
                    iconId = getBigIconResourceId(iconName)
                }
            }
            return if (iconId > 0) {
                iconId
            } else {
                R.drawable.mx_mudita_default
            }
        }
        ObjectType.POI -> {
            val amenity = searchResult.`object` as Amenity
            val id: String? = getIconNameForAmenity(amenity)
            if (id != null) {
                iconId = getBigIconResourceId(id)
                if (iconId > 0) {
                    return iconId
                }
            }
            return R.drawable.mx_mudita_default
        }
        else -> return R.drawable.mx_mudita_default
    }
}

private fun getTypeName(searchResult: SearchResult, localizationHelper: LocalizationHelper): String {
    when (searchResult.objectType) {
        ObjectType.CITY -> {
            val city = searchResult.`object` as City
            return localizationHelper.getCityTypeTranslation(city.type).orEmpty()
        }
        ObjectType.POSTCODE -> return localizationHelper.getPostcodeTranslation()
        ObjectType.VILLAGE -> {
            val city = searchResult.`object` as City
            return if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
                if (searchResult.distRelatedObjectName > 0) {
                    localizationHelper.getCityTypeTranslation(city.type).orEmpty()
                } else {
                    (localizationHelper.getCityTypeTranslation(city.type).orEmpty()
                            + ", "
                            + searchResult.localeRelatedObjectName)
                }
            } else {
                localizationHelper.getCityTypeTranslation(city.type).orEmpty()
            }
        }
        ObjectType.STREET -> {
            val streetBuilder = StringBuilder()
            if (searchResult.localeName.endsWith(")")) {
                val i = searchResult.localeName.indexOf('(')
                if (i > 0) {
                    streetBuilder.append(
                        searchResult.localeName.substring(
                            i + 1,
                            searchResult.localeName.length - 1
                        )
                    )
                }
            }
            if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
                if (streetBuilder.isNotEmpty()) {
                    streetBuilder.append(", ")
                }
                streetBuilder.append(searchResult.localeRelatedObjectName)
            }
            return streetBuilder.toString()
        }
        ObjectType.HOUSE -> {
            if (searchResult.relatedObject != null) {
                val relatedStreet = searchResult.relatedObject as Street
                return if (relatedStreet.city != null) {
                    (searchResult.localeRelatedObjectName + ", "
                            + relatedStreet.city.getName(
                        searchResult.requiredSearchPhrase.settings.lang,
                        true
                    ))
                } else {
                    searchResult.localeRelatedObjectName
                }
            }
            return ""
        }
        ObjectType.STREET_INTERSECTION -> {
            val street = searchResult.`object` as Street
            return if (street.city != null) {
                street.city.getName(searchResult.requiredSearchPhrase.settings.lang, true)
            } else ""
        }
        ObjectType.POI_TYPE -> {
            var res = ""
            if (searchResult.`object` is AbstractPoiType) {
                res = when (val abstractPoiType = searchResult.`object` as AbstractPoiType) {
                    is PoiCategory -> {
                        ""
                    }
                    is PoiFilter -> {
                        if (abstractPoiType.poiCategory != null) abstractPoiType.poiCategory.translation else ""
                    }
                    is PoiType -> {
                        if (abstractPoiType.category != null) abstractPoiType.category.translation else ""
                    }
                    else -> {
                        ""
                    }
                }
            } else if (searchResult.`object` is CustomSearchPoiFilter) {
                res = (searchResult.`object` as CustomSearchPoiFilter).name
            }
            return res
        }
        ObjectType.POI -> {
            val amenity = searchResult.`object` as Amenity
            return getTypeName(amenity)
        }
        ObjectType.LOCATION -> {
            val latLon = searchResult.location
            if (latLon != null && searchResult.localeRelatedObjectName == null) {
                val locationCountry: String = ""//app.regions.getCountryName(latLon)
                searchResult.localeRelatedObjectName = locationCountry ?: ""
            }
            return searchResult.localeRelatedObjectName
        }
        ObjectType.WPT -> {
            val sb = StringBuilder()
            val gpx = searchResult.relatedObject as GPXUtilities.GPXFile
            if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
                sb.append(searchResult.localeRelatedObjectName)
            }
            if (!Algorithms.isEmpty(gpx.path)) {
                if (sb.isNotEmpty()) {
                    sb.append(", ")
                }
                sb.append(File(gpx.path).name)
            }
            return sb.toString()
        }
        ObjectType.ROUTE -> return ""
        else -> Unit
    }
    return searchResult.objectType.name
}

fun getTypeName(amenity: Amenity): String {
    val pc = amenity.type
    val pt = pc.getPoiTypeByKeyName(amenity.subType)
    var typeStr = amenity.subType
    if (pt != null) {
        typeStr = pt.translation
    } else if (typeStr != null) {
        typeStr =
            Algorithms.capitalizeFirstLetterAndLowercase(typeStr.replace('_', ' '))
    }
    return typeStr
}

fun getTypeName(type: String, subType: String?): String? {
    val pc = MapPoiTypes.getDefaultNoInit().getPoiCategoryByName(type)
    val pt = pc.getPoiTypeByKeyName(subType)
    return when {
        pt != null -> pt.translation
        subType != null -> Algorithms.capitalizeFirstLetterAndLowercase(subType.replace('_', ' '))
        else -> null
    }
}

fun getDefaultLanguage(): String {
    return Locale.getDefault().language
}

private fun containsBigIcon(s: String?): Boolean {
    return bigIcons.containsKey(s)
}

private fun containsSmallIcon(s: String?): Boolean {
    return smallIcons.containsKey(s)
}
