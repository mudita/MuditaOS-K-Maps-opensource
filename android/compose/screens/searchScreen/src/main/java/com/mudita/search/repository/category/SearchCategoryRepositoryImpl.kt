package com.mudita.search.repository.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import net.osmand.Location
import net.osmand.ResultMatcher
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.osm.CommonPoiElement
import net.osmand.osm.MapPoiTypes
import net.osmand.search.SearchUICore
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.search.core.SearchSettings
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SearchCategoryRepositoryImpl @Inject constructor(
    private val mapPoiTypes: MapPoiTypes,
    private val repositorySearchUiCore: SearchUICore
) : SearchCategoryRepository, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + Job()

    private lateinit var searchUICore: SearchUICore

    override fun setupSearchCore(searchUICore: SearchUICore?) {
        this.searchUICore = searchUICore ?: repositorySearchUiCore
    }

    override suspend fun getCategoriesImmediate(
        query: String,
        location: Location?
    ): Flow<Result<List<CommonPoiElement>>> = callbackFlow {
        val settings: SearchSettings = searchUICore.searchSettings
            .setLang("en", false)
            .setSearchTypes(ObjectType.POI_TYPE)

        val categories: MutableList<CommonPoiElement> = mutableListOf()
        searchUICore.updateSettings(settings)
        location?.let {
            searchUICore.immediateSearch(
                query,
                LatLon(it.latitude, it.longitude)
            ).currentSearchResults.forEach { result ->
                when (result.objectType) {
                    ObjectType.POI_TYPE -> {
                        val commonPoiElement = result.`object` as? CommonPoiElement ?: return@forEach
                        categories.add(commonPoiElement)
                    }
                    else -> Unit
                }
            }
            trySend(Result.success(categories))
        }

        awaitClose {
            Timber.d("Search awaitClose")
        }
    }

    override suspend fun getPois(
        query: String,
        location: Location?,
        radius: Int
    ): Flow<Result<List<Amenity>>> = callbackFlow {
        val settings: SearchSettings = searchUICore.searchSettings
            .setOriginalLocation(
                LatLon(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
            )
            .setLang("en", false)
            .setSearchTypes(ObjectType.POI)
            .setRadiusLevel(radius)

        Timber.d("Search getPois: $query")

        searchUICore.updateSettings(settings)
        searchUICore.search(query, true, getResultMatcherForPoi(this, query), settings)

        awaitClose {
            Timber.d("Search awaitClose")
        }
    }

    private fun getResultMatcherForPoi(producerScope: ProducerScope<Result<List<Amenity>>>, query: String) =
        object : ResultMatcher<SearchResult> {
            val pois = mutableListOf<Amenity>()
            override fun publish(result: SearchResult?): Boolean {
                launch(coroutineContext) {
                    when (result?.objectType) {
                        ObjectType.POI -> {
                            val amenity = result.`object` as? Amenity ?: return@launch
//                            if (query.contains(amenity.subType, true))
                                pois.add(amenity)
                        }
                        ObjectType.SEARCH_FINISHED -> {
                            producerScope.trySend(Result.success(pois))
                        }
                        else -> Unit
                    }
                }
                return true
            }

            override fun isCancelled(): Boolean = false
        }

    companion object {
        private val availableCategories = listOf(
            "Accommodation",
            "Cafe and restaurant",
            "Gas station",
            "Shop",
            "Parking",
            "Finance",
            "Health care",
            "Entertainment",
            "Emergency",
            "Tourism",
            "Nearest POIs"
        )
    }
}