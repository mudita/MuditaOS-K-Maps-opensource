package com.mudita.search.repository.model

data class CitySearchHistory (
    val history: MutableList<SearchCityItem> = mutableListOf()
)