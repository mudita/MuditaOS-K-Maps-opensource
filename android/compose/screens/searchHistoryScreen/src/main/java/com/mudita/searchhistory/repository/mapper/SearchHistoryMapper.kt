@file:Suppress("DEPRECATION")

package com.mudita.searchhistory.repository.mapper

import android.content.Context
import com.mudita.map.common.model.LocalizationHelper
import com.mudita.map.common.model.SearchItemData
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.utils.POI_NAME_TYPE_DELIMITED
import com.mudita.map.common.utils.getTypeName
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import net.osmand.data.LatLon
import net.osmand.util.MapUtils

class SearchHistoryMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localizationHelper: LocalizationHelper,
) {
    val language: String get() = Locale.getDefault().language

    private fun toSearchItem(entity: SearchHistoryEntity, latLon: LatLon): SearchItemData {
        val distance = MapUtils.getDistance(latLon, LatLon(entity.lat, entity.lon))
        return when(SearchItemType.getByName(entity.itemType)) {
            SearchItemType.HISTORY -> entity.historyItem(distance)
            SearchItemType.CATEGORY -> entity.categoryItem()
            SearchItemType.SUB_CATEGORY -> entity.subCategoryItem()
            SearchItemType.POI -> entity.poiItem(distance)
            SearchItemType.ADDRESS -> entity.addressItem(distance)
            SearchItemType.POSTCODE -> entity.postcodeItem(distance)
            else -> entity.historyItem(distance)
        }
    }

    fun toSearchItems(entities: List<SearchHistoryEntity>, latLon: LatLon) = entities.map { toSearchItem(it, latLon) }

    fun toEntity(item: SearchItemData) = when (item.itemType) {
        SearchItemType.ADDRESS -> item.addressToSearchHistoryEntity()
        else -> item.defaultToSearchHistoryEntity()
    }

    private fun SearchItemData.defaultToSearchHistoryEntity() = SearchHistoryEntity(
        id = id,
        localName = address ?: title,
        searchQuery = title,
        searchCategory = desc ?: title,
        searchTime = searchTime,
        lat = lat,
        lon = lon,
        itemType = itemType.name,
        iconResourceName = getResourceName(iconRes),
        poiType = poiType,
        poiCategory = poiCategory,
        cityType = cityType,
        allNames = allNames,
        alternateName = alternateName,
    )

    private fun SearchItemData.addressToSearchHistoryEntity() = SearchHistoryEntity(
        id = id,
        localName = ("$address ".takeUnless { address.isNullOrBlank() } ?: "") + title,
        searchQuery = (this as? SearchItemData.Address)?.searchQuery ?: "",
        searchCategory = desc ?: "",
        searchTime = searchTime,
        lat = lat,
        lon = lon,
        itemType = itemType.name,
        iconResourceName = getResourceName(iconRes),
    )

    private fun SearchHistoryEntity.historyItem(distance: Double) = SearchItemData.History(
        id = id,
        searchQuery = searchQuery,
        title = getTitle(),
        desc = getDescription(),
        distance = distance,
        lat = lat,
        lon = lon,
        iconRes = iconResourceName?.let(::getIconRes) ?: iconRes,
        itemType = SearchItemType.getByName(itemType) ?: SearchItemType.HISTORY,
        cityType = cityType,
        allNames = allNames,
        alternateName = alternateName,
    )

    private fun SearchHistoryEntity.addressItem(distance: Double) = SearchItemData.Address(
        id = id,
        searchQuery = searchQuery,
        title = getTitle(),
        desc = searchCategory,
        distance = distance,
        lat = lat,
        lon = lon,
        iconRes = iconResourceName?.let(::getIconRes) ?: iconRes,
    )

    private fun SearchHistoryEntity.postcodeItem(distance: Double) = SearchItemData.Address(
        id = id,
        searchQuery = searchQuery,
        title = localName,
        desc = localizationHelper.getPostcodeTranslation(),
        distance = distance,
        lat = lat,
        lon = lon,
        iconRes = iconResourceName?.let(::getIconRes) ?: iconRes,
    )

    private fun SearchHistoryEntity.categoryItem() = SearchItemData.Category(
        title = localName,
        desc = searchCategory,
        iconRes = iconResourceName?.let(::getIconRes) ?: iconRes,
    )

    private fun SearchHistoryEntity.subCategoryItem() = SearchItemData.SubCategory(
        title = getTitle(),
        desc = searchCategory,
        iconRes = iconResourceName?.let(::getIconRes) ?: iconRes,
    )

    private fun SearchHistoryEntity.poiItem(distance: Double) = SearchItemData.Poi(
        id = id,
        title = getTitle(),
        desc = getDescription(),
        iconRes = iconResourceName?.let(::getIconRes) ?: iconRes,
        lat = lat,
        lon = lon,
        distance = distance,
        poiCategory = poiCategory,
        poiType = poiType,
    )

    private fun getIconRes(iconName: String): Int =
        context.resources.getIdentifier(iconName, "drawable", context.packageName)

    private fun getResourceName(resourceId: Int): String =
        context.resources.getResourceName(resourceId)


    private fun SearchHistoryEntity.getTitle(): String = allNames?.get(language) ?: localName

    private fun SearchHistoryEntity.getDescription(): String {
        val poiCategory = poiCategory
        val cityType = cityType
        val category =  when {
            poiCategory != null -> getTypeName(poiCategory, poiType)
            cityType != null -> localizationHelper.getCityTypeTranslation(cityType)
            else -> null
        }

        return if (category !== null) {
            listOfNotNull(alternateName, category).joinToString(separator = POI_NAME_TYPE_DELIMITED)
        } else {
            searchCategory
        }
    }
}
