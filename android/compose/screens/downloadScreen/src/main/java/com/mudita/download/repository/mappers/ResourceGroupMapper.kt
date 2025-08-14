package com.mudita.download.repository.mappers

import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.Resource
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.maps.data.api.dtos.MapResponseType
import net.osmand.map.OsmandRegions
import net.osmand.map.WorldRegion
import net.osmand.map.WorldRegion.AFRICA_REGION_ID
import net.osmand.map.WorldRegion.ANTARCTICA_REGION_ID
import net.osmand.map.WorldRegion.ASIA_REGION_ID
import net.osmand.map.WorldRegion.AUSTRALIA_AND_OCEANIA_REGION_ID
import net.osmand.map.WorldRegion.CENTRAL_AMERICA_REGION_ID
import net.osmand.map.WorldRegion.EUROPE_REGION_ID
import net.osmand.map.WorldRegion.NORTH_AMERICA_REGION_ID
import net.osmand.map.WorldRegion.PORTUGAL_REGION_ID
import net.osmand.map.WorldRegion.RUSSIA_REGION_ID
import net.osmand.map.WorldRegion.SOUTH_AMERICA_REGION_ID
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val continents = listOf(
    ANTARCTICA_REGION_ID,
    AFRICA_REGION_ID,
    ASIA_REGION_ID,
    AUSTRALIA_AND_OCEANIA_REGION_ID,
    CENTRAL_AMERICA_REGION_ID,
    EUROPE_REGION_ID,
    NORTH_AMERICA_REGION_ID,
    RUSSIA_REGION_ID,
    SOUTH_AMERICA_REGION_ID
)

private const val UNKNOWN_MAP_NAME = "Unknown"

fun List<MapResponseType>.createResource(osmandRegions: OsmandRegions): ResourceGroup {
    val groups = mutableListOf<ResourceGroup>()
    val items = mutableListOf<DownloadItem>()
    forEach {
        when (it) {
            is MapResponseType.MapDto -> items.add(it.toDownloadItem(osmandRegions))
            is MapResponseType.RegionDto -> groups.add(it.toResourceGroup(osmandRegions))
        }
    }
    val resourceGroup = ResourceGroup(
        name = "World",
        groups = groups.sortedWith(Resource.NameComparator),
        individualDownloadItems = items
    )
    return resourceGroup
}

private fun MapResponseType.MapDto.toDownloadItem(osmandRegions: OsmandRegions): DownloadItem {
    val regionData: WorldRegion? = osmandRegions.getRegionData(this.name)
    val regionName  = regionData?.localeName ?: UNKNOWN_MAP_NAME
    val parentName = regionData?.superregion?.localeName ?: UNKNOWN_MAP_NAME

    val item = DownloadItem(
        name = regionName,
        description = size,
        fileName = fileName,
        size = size,
        targetSize = targetSize,
        timestamp = dateTimeToMillis(timestamp),
        parentName = parentName,
    )
    return item
}

private fun MapResponseType.RegionDto.toResourceGroup(
    osmandRegions: OsmandRegions,
    parents: List<String> = emptyList()
): ResourceGroup {
    val regionName = getRegionGroupName(osmandRegions, this.name)
    val regs = this.regions
    val groups = mutableListOf<ResourceGroup>()
    val items = mutableListOf<DownloadItem>()
    val finalParents = mutableListOf<String>()
    finalParents.addAll(parents)
    finalParents.add(regionName)
    regs.forEach {
        when (it) {
            is MapResponseType.MapDto -> items.add(it.toDownloadItem(osmandRegions))
            is MapResponseType.RegionDto -> groups.add(it.toResourceGroup(osmandRegions, finalParents))
        }
    }

    val groupDownloadItem = if (fileName != null && size != null && targetSize != null) {
        DownloadItem(
            name = regionName,
            description = size.orEmpty(),
            fileName = fileName.orEmpty(),
            size = size.orEmpty(),
            targetSize = targetSize.orEmpty(),
            timestamp = dateTimeToMillis(timestamp),
            parentName = osmandRegions.getRegionData(name)?.superregion?.localeName ?: UNKNOWN_MAP_NAME
        ).also {
            // Portugal is an exception, because its regions (individualDownloadItems: Azores and Madeira) do not cover all of Portugal
            if (name == PORTUGAL_REGION_ID) {
                items.add(0, it)
            }
        }
    } else {
        null
    }

    val group = ResourceGroup(
        name = regionName,
        groups = groups,
        individualDownloadItems = items,
        groupDownloadItem = groupDownloadItem,
        parents = parents
    )
    return group
}

private fun getRegionGroupName(osmandRegions: OsmandRegions, fullName: String): String {
    if (fullName in continents) {
        // Continents names are taken from strings.xml file and are available in osmandRegions.worldRegion in regionName
        return osmandRegions.worldRegion.subregions?.firstOrNull { it.regionFullName == fullName }?.regionName ?: UNKNOWN_MAP_NAME
    }

    return (osmandRegions.getRegionData(fullName)?.localeName ?: UNKNOWN_MAP_NAME)
}

private fun dateTimeToMillis(dateTimeString: String?): Long? {
    dateTimeString ?: return null
    val zonedDateTime = ZonedDateTime.parse(dateTimeString, formatter.withZone(ZoneOffset.UTC))
    return zonedDateTime.toInstant().toEpochMilli()
}