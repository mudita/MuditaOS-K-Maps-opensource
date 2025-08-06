package com.mudita.map.common.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class MyPlaceItem(
    val title: String? = null,
    val desc: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val distance: Double? = null,
    val searchItemType: SearchItemType? = null,
    val id: UUID
): Parcelable {
    val formattedTitle: String get() = when(searchItemType) {
        SearchItemType.ADDRESS -> ""
        else -> title.orEmpty()
    }

    val formattedDesc get() = when(searchItemType) {
        SearchItemType.ADDRESS -> desc.orEmpty() + " " + title.orEmpty()
        else -> address.takeIf { it.isNullOrEmpty().not() } ?: desc
    }
}