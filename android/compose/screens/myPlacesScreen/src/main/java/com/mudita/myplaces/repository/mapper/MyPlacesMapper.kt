package com.mudita.myplaces.repository.mapper

import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.utils.round
import com.mudita.maps.data.db.entity.MyPlacesEntity
import java.util.UUID
import net.osmand.data.LatLon

fun MyPlacesEntity.toItem() = MyPlaceItem(
    title,
    desc,
    address,
    latitude,
    longitude,
    distance,
    SearchItemType.getByName(itemType),
    id
)

fun List<MyPlacesEntity>.toItems() = map { it.toItem() }

fun MyPlaceItem.toMyPlacesEntity() = MyPlacesEntity(
    id,
    title,
    desc,
    address,
    lat,
    lng,
    distance,
    searchItemType?.name,
    System.currentTimeMillis()
)

fun SearchItem.toMyPlaceItem() = MyPlaceItem(
    localName.takeIf { itemType != SearchItemType.ADDRESS },
    desc,
    if (itemType == SearchItemType.ADDRESS) "$desc $localName" else address,
    latLon.latitude.round(5),
    latLon.longitude.round(5),
    distance,
    itemType,
    UUID.randomUUID()
)

fun MyPlaceItem.toSearchItem() = SearchItem(
    id = id,
    formattedTitle,
    desc,
    formattedDesc,
    distance,
    latLon = if (lat == null || lng == null) LatLon(0.0, 0.0) else LatLon(lat!!, lng!!),
    itemType = searchItemType ?: SearchItemType.POI
)