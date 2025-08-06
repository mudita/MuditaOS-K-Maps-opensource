package com.mudita.map.common.model;

enum class SearchItemType {
    HISTORY,
    CATEGORY,
    SUB_CATEGORY,
    POI,
    ADDRESS,
    CITY,
    POSTCODE;

    companion object {
        fun getByName(name: String?): SearchItemType? {
            return values().find { it.name == name }
        }
    }
}