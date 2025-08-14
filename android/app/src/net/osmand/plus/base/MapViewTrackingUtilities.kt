package net.osmand.plus.base

import android.content.Context
import android.os.AsyncTask
import android.view.WindowManager
import androidx.core.util.Pair
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import net.osmand.CallbackWithObject
import net.osmand.Location
import net.osmand.StateChangedListener
import net.osmand.data.LatLon
import net.osmand.data.RotatedTileBox
import net.osmand.map.IMapLocationListener
import net.osmand.map.WorldRegion
import net.osmand.plus.OsmAndConstants
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.dashboard.DashboardOnMap
import net.osmand.plus.mapmarkers.MapMarker
import net.osmand.plus.mapmarkers.MapMarkersHelper.MapMarkerChangedListener
import net.osmand.plus.resources.DetectRegionTask
import net.osmand.plus.routing.RoutingHelper
import net.osmand.plus.routing.RoutingHelperUtils
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.utils.BooleanUtils.toNotNull
import net.osmand.plus.utils.LetUtils.safeLet
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.AnimateDraggingMapThread
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.util.MapUtils

class MapViewTrackingUtilities(
    private val app: OsmandApplication
) : OsmAndLocationListener,
    IMapLocationListener, OsmAndCompassListener, MapMarkerChangedListener {

    private val settings: OsmandSettings? = app.settings
    private val routingHelper: RoutingHelper? = app.routingHelper
    private var mapView: OsmandMapTileView? = null
    private var dashboard: DashboardOnMap? = null
    private var enable3DViewListener: StateChangedListener<Boolean>? = null
    var isMapLinkedToLocation = true
        set(value) {
            if (field == value) return
            field = value
            if (!value) {
                isMovingToMyLocation = false
            }
            settings?.MAP_LINKED_TO_LOCATION?.set(value)
            if (!value) {
                val autoFollow = settings?.AUTO_FOLLOW_ROUTE?.get() ?: 0
                if (autoFollow > 0 && routingHelper!!.isFollowingMode && !routePlanningMode) {
                    backToLocationWithDelay(autoFollow)
                }
            }
        }
    var isMovingToMyLocation = false
        private set
    private var lastTimeAutoZooming: Long = 0
    var lastManualZoomTime: Long = 0
        private set
    private var followingMode = false
    private var routePlanningMode = false
    var isShowViewAngle = false
        private set
    var locationProvider: String? = null
        private set
    private var myLocation: Location?
    var heading: Float? = null
        private set
    private var drivingRegionUpdated = false

    init {
        myLocation = app.locationProvider?.lastKnownLocation
        app.locationProvider?.addLocationListener(this)
        app.locationProvider?.addCompassListener(this)
        addTargetPointListener(app)
        addMapMarkersListener(app)
        addEnable3DViewListener()
        initMapLinkedToLocation()
    }

    fun resetDrivingRegionUpdate() {
        drivingRegionUpdated = false
    }

    private fun addTargetPointListener(app: OsmandApplication) {
        app.targetPointsHelper?.addListener {
            app.runInUIThread {
                mapView?.refreshMap()
            }
        }
    }

    private fun addEnable3DViewListener() {
        enable3DViewListener =
            StateChangedListener { updateMapTiltAndRotation() }
        settings?.ENABLE_3D_VIEW?.addListener(enable3DViewListener)
    }

    private fun addMapMarkersListener(app: OsmandApplication) {
        app.mapMarkersHelper?.addListener(this)
    }

    override fun onMapMarkerChanged(mapMarker: MapMarker) {}
    override fun onMapMarkersChanged() {
        mapView?.refreshMap()
    }

    fun setMapView(mapView: OsmandMapTileView?) {
        this.mapView = mapView?.also {
            val wm: WindowManager? = app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            var orientation = wm?.defaultDisplay?.rotation ?: 0
            app.locationProvider?.updateScreenOrientation(orientation)
            mapView.addMapLocationListener(this)
        }
    }

    override fun updateCompassValue(value: Float) {
        val prevHeading = heading
        heading = value
        var headingChanged = prevHeading == null
        if (!headingChanged) {
            safeLet(prevHeading, heading) { prevHeading, heading ->
                headingChanged = abs(
                    MapUtils.degreesDiff(
                    prevHeading.toDouble(),
                    heading.toDouble()
                )) > COMPASS_HEADING_THRESHOLD
            }
        }
        mapView?.let { mapView ->
            val isRotateMapCompass =
                settings?.ROTATE_MAP?.get() == OsmandSettings.ROTATE_MAP_COMPASS
            if (isRotateMapCompass && !routePlanningMode) {
                if (abs(MapUtils.degreesDiff(mapView.rotate.toDouble(), -value.toDouble())) > 1.0) {
                    mapView.setRotateValue(-value, false)
                }
            } else if (isShowViewAngle && headingChanged) {
                mapView.refreshMap()
            }
        }
        if (dashboard != null && headingChanged) {
            dashboard?.updateCompassValue(value.toDouble())
        }
    }

    fun setDashboard(dashboard: DashboardOnMap?) {
        this.dashboard = dashboard
    }

    fun detectDrivingRegion(latLon: LatLon) {
        detectCurrentRegion(latLon) { detectedRegion: WorldRegion? ->
            if (detectedRegion != null) {
                val oldRegion = app.settings?.DRIVING_REGION?.get()
                app.setupDrivingRegion(detectedRegion)
                val currentRegion = app.settings?.DRIVING_REGION?.get()
                if (oldRegion?.leftHandDriving != currentRegion?.leftHandDriving) {
                    val mode = routingHelper?.appMode
                    routingHelper?.onSettingsChanged(mode, true)
                }
            }
            true
        }
    }

    fun detectCurrentRegion(
        latLon: LatLon,
        onRegionDetected: CallbackWithObject<WorldRegion?>
    ) {
        DetectRegionTask(app, onRegionDetected)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, latLon)
    }

    override fun updateLocation(location: Location?) {
        val prevLocation = myLocation
        var movingTime =
            if (prevLocation != null && location != null) location.time - prevLocation.time else 0
        myLocation = location
        var showViewAngle = false

        if (location != null) {
            locationProvider = location.provider
            if (settings?.DRIVING_REGION_AUTOMATIC?.get().toNotNull() && !drivingRegionUpdated && !app.isApplicationInitializing) {
                drivingRegionUpdated = true
                RoutingHelperUtils.checkAndUpdateStartLocation(app, location, true)
            }
        }
        mapView?.let { view ->
            val tb = view.currentRotatedTileBox?.copy()

            if (isMapLinkedToLocation && location != null) {
                var zoom: Pair<Int?, Double?>? = null
                var rotation: Float? = null
                var pendingRotation = false
                if (settings?.AUTO_ZOOM_MAP?.get().toNotNull()) {
                    zoom = autozoom(tb, location)
                }
                val currentMapRotation = settings?.ROTATE_MAP?.get()
                val smallSpeedForCompass = isSmallSpeedForCompass(location)
                val smallSpeedForAnimation = isSmallSpeedForAnimation(location)
                showViewAngle = (!location.hasBearing() || smallSpeedForCompass) && tb != null &&
                        NativeUtilities.containsLatLon(
                            view.mapRenderer,
                            tb,
                            location.latitude,
                            location.longitude
                        )
                if (currentMapRotation == OsmandSettings.ROTATE_MAP_BEARING) {
                    // special case when bearing equals to zero (we don't change anything)
                    if (location.hasBearing() && location.bearing != 0f) {
                        rotation = -location.bearing
                    }
                    if (rotation == null && prevLocation != null && tb != null) {
                        val distDp = tb.pixDensity * MapUtils.getDistance(
                            prevLocation,
                            location
                        ) / tb.density
                        if (distDp > AnimateDraggingMapThread.SKIP_ANIMATION_DP_THRESHOLD) {
                            movingTime = 0
                        }
                    }
                } else if (currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS) {
                    showViewAngle = routePlanningMode // disable compass rotation in that mode
                    pendingRotation = true
                } else if (currentMapRotation == OsmandSettings.ROTATE_MAP_NONE) {
                    pendingRotation = true
                }
                registerUnregisterSensor(location, smallSpeedForCompass)
                if (settings?.ANIMATE_MY_LOCATION?.get().toNotNull() && !smallSpeedForAnimation && !isMovingToMyLocation) {
                    view.animatedDraggingThread?.startMoving(
                        location.latitude, location.longitude, zoom,
                        pendingRotation, rotation, movingTime, false
                    ) { isMovingToMyLocation = false }
                } else {
                    if (view.hasMapRenderer()) {
                        movingTime = if (isMovingToMyLocation) (movingTime * 0.7)
                            .coerceAtMost(MOVE_ANIMATION_TIME.toDouble())
                            .toLong() else MOVE_ANIMATION_TIME
                        if (view.settings?.DO_NOT_USE_ANIMATIONS?.get().toNotNull()) {
                            movingTime = 0
                        }
                        view.animatedDraggingThread?.startMoving(
                            location.latitude, location.longitude, zoom,
                            pendingRotation, rotation, movingTime, false
                        ) { isMovingToMyLocation = false }
                    } else {
                        safeLet(zoom?.first, zoom?.second) { first, second ->
                            view.animatedDraggingThread?.startZooming(first, second, false)
                        }
                        if (rotation != null) {
                            view.setRotateValue(rotation, false)
                        }
                        view.setLatLon(location.latitude, location.longitude)
                    }
                }
            } else if (location != null) {
                showViewAngle =
                    (!location.hasBearing() || isSmallSpeedForCompass(location)) && tb != null &&
                            NativeUtilities.containsLatLon(
                                view.mapRenderer,
                                tb,
                                location.latitude,
                                location.longitude
                            )
                registerUnregisterSensor(location, false)
            }
            isShowViewAngle = showViewAngle
            followingMode = routingHelper?.isFollowingMode ?: false
            if (routePlanningMode != routingHelper?.isRoutePlanningMode) {
                switchRoutePlanningMode()
            }
            // When location is changed we need to refresh map in order to show movement!
            view.refreshMap()
        }
        dashboard?.updateMyLocation(location)
    }

    fun switchRoutePlanningMode() {
        routePlanningMode = routingHelper?.isRoutePlanningMode ?: false
        if (!routePlanningMode && followingMode) {
            backToLocationImpl()
        }
    }

    fun appModeChanged() {
        resetDrivingRegionUpdate()
        updateMapTiltAndRotation()
    }

    fun updateMapTiltAndRotation() {
        mapView?.let {

            it.elevationAngle = settings?.lastKnownMapElevation ?: 0f
            if (settings?.ROTATE_MAP?.get() != OsmandSettings.ROTATE_MAP_COMPASS) {
                it.setRotateValue(settings?.lastKnownMapRotation ?: 0f, true)
            }
        }
    }

    private fun registerUnregisterSensor(location: Location?, smallSpeedForCompass: Boolean) {
        val currentMapRotation = settings?.ROTATE_MAP?.get()
        val registerCompassListener = ((isShowViewAngle) && location != null
                || currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS && !routePlanningMode
                || currentMapRotation == OsmandSettings.ROTATE_MAP_BEARING && smallSpeedForCompass)

        app.locationProvider?.registerOrUnregisterCompassListener(registerCompassListener && routingHelper?.isFollowingMode == true)
    }

    private fun defineZoomFromSpeed(tb: RotatedTileBox?, speed: Float): Float {
        if (speed < 7f / 3.6 || tb == null) {
            return 0f
        }
        val visibleDist = tb.getDistance(tb.centerPixelX, 0, tb.centerPixelX, tb.centerPixelY)
        var time = 75f // > 83 km/h show 75 seconds
        if (speed < 83f / 3.6) {
            time = 60f
        }
        time /= settings?.AUTO_ZOOM_MAP_SCALE?.get()?.coefficient ?: 1f
        val distToSee = (speed * time).toDouble()
        // check if 17, 18 is correct?
        return (ln(visibleDist / distToSee) / ln(2.0)).toFloat()
    }

    fun autozoom(tb: RotatedTileBox?, location: Location): Pair<Int?, Double?>? {
        if (tb == null) return null
        if (location.hasSpeed()) {
            val now = System.currentTimeMillis()
            var zdelta = defineZoomFromSpeed(tb, location.speed)
            if (abs(zdelta) >= 0.5 /*?Math.sqrt(0.5)*/) {
                // prevent ui hysteresis (check time interval for autozoom)
                if (zdelta >= 2) {
                    // decrease a bit
                    zdelta -= 1f
                } else if (zdelta <= -2) {
                    // decrease a bit
                    zdelta += 1f
                }
                var targetZoom = min(
                    tb.zoom + tb.zoomFloatPart + zdelta,
                    settings?.AUTO_ZOOM_MAP_SCALE?.get()?.maxZoom?.toDouble() ?: 15.5
                )
                val isUserZoomed = lastManualZoomTime > lastTimeAutoZooming
                val threshold = settings?.AUTO_FOLLOW_ROUTE?.get() ?: 0
                if (now - lastTimeAutoZooming > AUTO_ZOOM_DEFAULT_CHANGE_ZOOM && !isUserZoomed
                    || now - lastManualZoomTime > max(
                        threshold,
                        AUTO_ZOOM_DEFAULT_CHANGE_ZOOM
                    ) && isUserZoomed
                ) {
                    lastTimeAutoZooming = now
                    // round to 0.33
                    targetZoom = ((targetZoom * 3).roundToLong() / 3f).toDouble()
                    val newIntegerZoom = targetZoom.roundToLong().toInt()
                    val zPart = targetZoom - newIntegerZoom
                    return if (newIntegerZoom > 0) Pair(newIntegerZoom, zPart) else null
                }
            }
        }
        return null
    }

    @JvmOverloads
    fun backToLocationImpl(zoom: Int = 15) {
        mapView?.let {
            val locationProvider = app.locationProvider
            val lastKnownLocation = locationProvider?.lastKnownLocation
            val lastStaleKnownLocation = locationProvider?.lastStaleKnownLocation
            val location = lastKnownLocation ?: lastStaleKnownLocation ?: app.settings?.lastKnownMapLocation?.let { lastLocation ->
                Location("", lastLocation.latitude, lastLocation.longitude)
            }
            if (location != null) {
                animateBackToLocation(location, zoom)
            }
            it.refreshMap()
        }
    }

    fun animateBackToLocation(location: Location, zoom: Int) {
        val thread = mapView?.animatedDraggingThread
        val startAnimationCallback = Runnable {
            isMovingToMyLocation = true
            if (!isMapLinkedToLocation) {
                isMapLinkedToLocation = true
            }
        }
        val finishAnimationCallback =
            Runnable { isMovingToMyLocation = false }
        thread?.startMoving(
            location.latitude, location.longitude, zoom, false,
            true, startAnimationCallback, finishAnimationCallback
        )
    }

    private fun backToLocationWithDelay(delay: Int) {
        app.runMessageInUIThreadAndCancelPrevious(
            AUTO_FOLLOW_MSG_ID,
            {
                if ((mapView != null) && !isMapLinkedToLocation) {
                    backToLocationImpl(15)
                }
            }, delay * 1000L
        )
    }

    private fun initMapLinkedToLocation() {
        settings?.let {
            if (!settings.MAP_LINKED_TO_LOCATION.get()) {
                val lastAppClosedTime = settings.LAST_MAP_ACTIVITY_PAUSED_TIME.get()
                isMapLinkedToLocation =
                    System.currentTimeMillis() - lastAppClosedTime > MAP_LINKED_LOCATION_TIME_MS
            }
            settings.MAP_LINKED_TO_LOCATION.set(isMapLinkedToLocation)
        }
    }

    override fun locationChanged(newLatitude: Double, newLongitude: Double, source: Any) {
        // when user start dragging
        isMapLinkedToLocation = false
    }

    @Suppress("UNUSED_PARAMETER")
    fun setMapRotationEnabled(enabled: Boolean, isWalkingNavigation: Boolean) {
        val settings = settings ?: return
        val mode = when {
            // This will be re-enabled after the map rendering speed is improved.
            // enabled && isWalkingNavigation -> OsmandSettings.ROTATE_MAP_COMPASS
            enabled -> OsmandSettings.ROTATE_MAP_BEARING
            else -> OsmandSettings.ROTATE_MAP_NONE
        }
        if (mode == settings.ROTATE_MAP.get()) return
        settings.ROTATE_MAP.set(mode)
        mapView?.mapPosition = if (enabled) OsmandSettings.MIDDLE_BOTTOM_CONSTANT else OsmandSettings.CENTER_CONSTANT
        mapView?.refreshMap()
    }

    fun switchRotateMapMode() {
        if (shouldResetRotation()) {
            mapView?.resetManualRotation()
        } else {
            val vl = ((settings?.ROTATE_MAP?.get() ?: 0) + 1) % 3
            settings?.ROTATE_MAP?.set(vl)
        }

        mapView?.refreshMap()
    }

    fun shouldResetRotation(): Boolean {
        return settings?.ROTATE_MAP?.get() == OsmandSettings.ROTATE_MAP_NONE && mapView != null && mapView!!.rotate != 0f
    }

    val mapLocation: LatLon
        get() = mapView?.let {
            LatLon(it.latitude, it.longitude)
        } ?: settings?.lastKnownMapLocation ?: LatLon(0.0,0.0)
    val mapRotate: Float?
        get() {
            return mapView?.rotate
        }

    fun setZoomTime(time: Long) {
        lastManualZoomTime = time
    }

    companion object {
        const val COMPASS_HEADING_THRESHOLD = 1.0f
        private const val MAP_LINKED_LOCATION_TIME_MS = 60 * 60 * 1000
        private const val COMPASS_REQUEST_TIME_INTERVAL_MS = 5000
        private const val AUTO_FOLLOW_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 4
        private const val MOVE_ANIMATION_TIME: Long = 500
        const val AUTO_ZOOM_DEFAULT_CHANGE_ZOOM = 4500
        fun isSmallSpeedForCompass(location: Location): Boolean {
            return !location.hasSpeed() || location.speed < 0.5
        }

        fun isSmallSpeedForAnimation(location: Location): Boolean {
            return !location.hasSpeed() || java.lang.Float.isNaN(location.speed) || location.speed < 1.5
        }
    }
}