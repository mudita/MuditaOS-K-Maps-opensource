package com.mudita.search.repository.category

import kotlinx.coroutines.flow.Flow
import net.osmand.Location
import net.osmand.data.Amenity
import net.osmand.osm.CommonPoiElement
import net.osmand.search.SearchUICore

interface SearchCategoryRepository {
    fun setupSearchCore(searchUICore: SearchUICore?)
    suspend fun getCategoriesImmediate(
        query: String,
        location: Location?
    ): Flow<Result<List<CommonPoiElement>>>

    suspend fun getPois(
        query: String,
        location: Location?,
        radius: Int
    ): Flow<Result<List<Amenity>>>
}
