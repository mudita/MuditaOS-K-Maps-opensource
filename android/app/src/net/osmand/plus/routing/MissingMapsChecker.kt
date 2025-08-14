package net.osmand.plus.routing

import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.map.WorldRegion
import net.osmand.map.WorldRegion.PORTUGAL_REGION_ID
import net.osmand.plus.OsmandApplication
import net.osmand.util.MapUtils
import java.io.IOException

object MissingMapsChecker {

    // Minimum distance in meters between two points
    private const val MIN_POINT_SEPARATION_DIST = 20000

    private const val PRAGUE_MAP_NAME = "czech-republic_praha_europe"

    @JvmStatic
    @Throws(IOException::class)
    fun isAnyPointOnWater(points: List<Location>, params: RouteCalculationParams): Boolean {
        for (point in points) {
            val regions: List<WorldRegion> = params.ctx.regions?.getWorldRegionsAt(
                LatLon(point.latitude, point.longitude), true
            ) ?: emptyList()
            if (regions.isEmpty()) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getDistributedPathPoints(points: List<Location>): List<Location> {
        val result = mutableListOf<Location>()
        for (i in 0 until points.size - 1) {
            result.add(points[i])

            val totalDistance = points[i].distanceTo(points[i + 1])
            val numSegments = (totalDistance / MIN_POINT_SEPARATION_DIST).toInt()

            if (numSegments > 1) {
                for (j in 1 until numSegments) {
                    val fraction = j.toDouble() / numSegments
                    val intermediatePoint = MapUtils.calculateInterpolatedPoint(points[i], points[i + 1], fraction)
                    result.add(intermediatePoint)
                }
            }
        }
        result.add(points.last())
        return removeDensePoints(result)
    }

    @JvmStatic
    fun getStartFinishIntermediatePoints(params: RouteCalculationParams): List<Location> {
        val points: MutableList<Location> = mutableListOf()
        points.add(Location("", params.start.latitude, params.start.longitude))
        params.intermediates?.forEach { point ->
            points.add(Location("", point.latitude, point.longitude))
        }
        points.add(Location("", params.end.latitude, params.end.longitude))
        return points
    }

    @JvmStatic
    @Throws(IOException::class, IllegalStateException::class)
    fun getMissingMaps(application: OsmandApplication, points: List<Location>): List<String> {
        val missingWorldRegions = mutableSetOf<String>()
        val missingWorldRegionsToExclude = mutableSetOf<String>()

        for (point in points) {
            val latLon = LatLon(point.latitude, point.longitude)

            val worldRegions: List<WorldRegion> = application.regions?.getWorldRegionsAt(latLon, true) ?: emptyList()
            val missingWorldRegionsForPoint = mutableSetOf<String>()
            val missingWorldRegionsToExcludeForPoint = mutableSetOf<String>()
            var hasAnyRegionDownloaded = false

            /* Regions that should be downloaded even if different region for the same point is already downloaded
            Currently the exceptions are: czech-republic_praha_europe */
            val requiredRegions = mutableSetOf<String>()

            for (region in worldRegions) {
                val isDownloaded = region.regionDownloadName?.let { application.resourceManager?.checkIfObjectDownloaded(it) } ?: true

                if (!isDownloaded && region.regionDownloadName == PRAGUE_MAP_NAME) {
                    requiredRegions.add(PRAGUE_MAP_NAME)
                }

                // Ignore super regions, as we don't support downloading them.
                if (!isDownloaded && region.subregions.isEmpty()) {
                    missingWorldRegionsForPoint.add(region.regionDownloadName)
                } else if (isDownloaded) {
                    hasAnyRegionDownloaded = true
                }

                /* If getWorldRegionsAt returns region and its superregion we want to exclude superregion
                because it doesn't have its own downloadable map (eg. poland_europe). The only exception is Portugal. */
                if (region.level > 2 && region.superregion != null && region.superregion.regionFullName != PORTUGAL_REGION_ID) {
                    region.superregion.regionDownloadName?.let {
                        missingWorldRegionsToExcludeForPoint.add(it)
                    }
                }
            }

            // Regions that should be downloaded regardless of hasAnyRegionDownloaded state
            if (requiredRegions.isNotEmpty()) {
                missingWorldRegions.addAll(requiredRegions)
            }

            if (!hasAnyRegionDownloaded) {
                missingWorldRegions.addAll(missingWorldRegionsForPoint)
                missingWorldRegionsToExclude.addAll(missingWorldRegionsToExcludeForPoint)
            }
        }

        // Returns missing maps file names (eg. Poland_greater-poland_europe)
        return missingWorldRegions
            .minus(missingWorldRegionsToExclude)
            .map { it.replaceFirstChar { char -> char.uppercaseChar()} }
    }

    private fun removeDensePoints(routeLocation: List<Location>): List<Location> {
        val mapsBasedOnPoints: MutableList<Location> = mutableListOf()
        if (routeLocation.isNotEmpty()) {
            mapsBasedOnPoints.add(routeLocation[0])
            var i = 0
            var j = 1
            while (j < routeLocation.size) {
                if (shouldAddPoint(i, j, routeLocation)) {
                    mapsBasedOnPoints.add(routeLocation[j])
                    i = j
                }
                j++
            }
        }
        return mapsBasedOnPoints
    }

    private fun shouldAddPoint(i: Int, j: Int, routeLocation: List<Location>): Boolean {
        return j == routeLocation.size - 1 || routeLocation[i].distanceTo(routeLocation[j]) >= MIN_POINT_SEPARATION_DIST
    }
}
