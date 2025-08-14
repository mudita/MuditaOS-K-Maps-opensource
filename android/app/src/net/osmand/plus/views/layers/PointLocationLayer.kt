package net.osmand.plus.views.layers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.mudita.maps.R
import kotlin.math.abs
import kotlin.math.min
import net.osmand.Location
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.AnimatedValue
import net.osmand.core.jni.MapMarker
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.SWIGTYPE_p_void
import net.osmand.core.jni.SwigUtilities
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.MapViewTrackingUtilities.Companion.isSmallSpeedForAnimation
import net.osmand.plus.profiles.ProfileIconColors
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.AnimateMapMarkersThread
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.util.MapUtils

open class PointLocationLayer(context: Context) : OsmandMapLayer(context), OsmAndLocationListener,
    OsmAndCompassListener, IContextMenuProvider {
    private var headingPaint: Paint? = null
    private var bitmapPaint: Paint? = null
    private var area: Paint? = null
    private var aroundArea: Paint? = null
    private var appMode: ApplicationMode? = null
    private var carView = false
    private var layerTextScale = 1f

    @ColorInt
    private var profileColor = 0
    private var locationIcon: Drawable? = null
    private var locationIconId = 0
    private val locationProvider: OsmAndLocationProvider? = application.locationProvider
    private val mapViewTrackingUtilities = application.mapViewTrackingUtilities
    private val settings = application.settings
    private var nm = false
    private var locationOutdated = false
    private var prevLocation: Location? = null
    private var locationMarker: CoreMapMarker? = null
    private var markersInvalidated = true
    private var lastBearingCached: Float? = null
    private var lastHeadingCached: Float? = null
    private var currentMarkerState = MarkerState.Stay
    var lastMarkerLocation: LatLon? = null
        private set

    private enum class MarkerState {
        Stay, Move, None
    }

    private class CoreMapMarker {
        var marker: MapMarker? = null
        var onSurfaceIconKey: SWIGTYPE_p_void? = null
        var onSurfaceHeadingIconKey: SWIGTYPE_p_void? = null
        fun setVisibility(visible: Boolean) {
            if (marker == null) {
                return
            }
            marker?.setIsHidden(!visible)
            marker?.setIsAccuracyCircleVisible(visible)
        }

        companion object {
            fun createAndAddToCollection(
                markersCollection: MapMarkersCollection,
                id: Int,
                baseOrder: Int,
                icon: Drawable,
                scale: Float,
                @ColorInt profileColor: Int
            ): CoreMapMarker? {
                val marker = CoreMapMarker()
                val myLocMarkerBuilder = MapMarkerBuilder()
                myLocMarkerBuilder.markerId = id
                myLocMarkerBuilder.baseOrder = baseOrder
                myLocMarkerBuilder.setIsAccuracyCircleSupported(true)
                myLocMarkerBuilder.accuracyCircleBaseColor =
                    NativeUtilities.createFColorRGB(profileColor)
                myLocMarkerBuilder.pinIconVerticalAlignment =
                    MapMarker.PinIconVerticalAlignment.CenterVertical
                myLocMarkerBuilder.pinIconHorisontalAlignment =
                    MapMarker.PinIconHorisontalAlignment.CenterHorizontal
                myLocMarkerBuilder.setIsHidden(true)
                val markerBitmap = AndroidUtils.createScaledBitmap(icon, scale)
                if (markerBitmap != null) {
                    marker.onSurfaceIconKey = SwigUtilities.getOnSurfaceIconKey(1)
                    myLocMarkerBuilder.addOnMapSurfaceIcon(
                        marker.onSurfaceIconKey,
                        NativeUtilities.createSkImageFromBitmap(markerBitmap)
                    )
                }
                marker.marker = myLocMarkerBuilder.buildAndAddToCollection(markersCollection)
                return if (marker.marker != null) marker else null
            }
        }
    }

    private fun initLegacyRenderer() {
        headingPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        area = Paint()
        aroundArea = Paint()
        aroundArea?.style = Paint.Style.STROKE
        aroundArea?.strokeWidth = 1f
        aroundArea?.isAntiAlias = true
    }

    private fun initCoreRenderer() {
        markersInvalidated = true
    }

    override fun setMapActivity(mapActivity: MapActivity?) {
        super.setMapActivity(mapActivity)
        if (mapActivity != null) {
            initCoreRenderer()
        } else {
            clearMapMarkersCollections()
        }
    }

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        val hasMapRenderer = hasMapRenderer()
        if (hasMapRenderer) {
            initCoreRenderer()
        } else {
            initLegacyRenderer()
        }
        view.settings?.let {
            updateParams(
                it.applicationMode,
                false,
                locationProvider?.lastKnownLocation == null
            )
        }
        locationProvider?.addLocationListener(this)
        locationProvider?.addCompassListener(this)
    }

    override fun areMapRendererViewEventsAllowed(): Boolean {
        return true
    }

    override fun onUpdateFrame(mapRenderer: MapRendererView) {
        super.onUpdateFrame(mapRenderer)
        if (isMapLinkedToLocation && !isMovingToMyLocation) {
            val app = application
            val lastKnownLocation = locationProvider?.lastStaleKnownLocation
            val snapToRoad = app.settings?.SNAP_TO_ROAD?.get()
            val followingMode = app.routingHelper?.isFollowingMode
            val lastRouteProjection =
                if (followingMode == true && snapToRoad == true) app.osmandMap?.mapLayers?.routeLayer?.lastRouteProjection else null
            val target31 = mapRenderer.target
            val location = lastRouteProjection ?: lastKnownLocation
            updateMarker(location, target31, 0)
        }
        lastMarkerLocation = currentMarkerLocation
    }

    private fun setMarkerState(
        markerState: MarkerState,
        showHeading: Boolean,
        forceUpdate: Boolean
    ): Boolean {
        if (currentMarkerState == markerState && !forceUpdate) {
            return false
        }
        currentMarkerState = markerState
        updateMarkerState(showHeading)
        return true
    }

    private fun recreateMarker(
        icon: Drawable?,
        @ColorInt profileColor: Int
    ): CoreMapMarker? {
        if (view == null || icon == null) {
            return null
        }
        if (mapMarkersCollection == null) {
            mapMarkersCollection = MapMarkersCollection()
        }
        return CoreMapMarker.createAndAddToCollection(
            markersCollection = mapMarkersCollection,
            id = MARKER_ID_MY_LOCATION,
            baseOrder = getPointsOrder(),
            icon = icon,
            scale = textScale,
            profileColor = profileColor,
        )
    }

    private fun setMarkerProvider() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && mapMarkersCollection != null) {
            mapRenderer.addSymbolsProvider(mapMarkersCollection)
        }
    }

    private fun recreateMarkerCollection(): Boolean {
        if (view == null || !hasMapRenderer()) {
            return false
        }
        clearMapMarkersCollections()
        locationMarker = recreateMarker(locationIcon, profileColor)
        setMarkerProvider()
        return true
    }

    private fun updateMarkerState(showHeading: Boolean) {
        when (currentMarkerState) {
            MarkerState.Move -> locationMarker?.setVisibility(false)
            MarkerState.Stay -> locationMarker?.setVisibility(!showHeading)
            MarkerState.None -> locationMarker?.setVisibility(false)
        }
    }

    private fun updateMarker(location: Location?, target31: PointI?, animationDuration: Long) {
        if (location != null) {
            updateMarkerPosition(location, target31, animationDuration)
            if (location.hasBearing()) {
                lastBearingCached?.let {
                    val bearing = location.bearing - 90.0f
                    val updateBearing =
                        lastBearingCached == null || abs(bearing - it) > 0.1
                    if (updateBearing) {
                        lastBearingCached = bearing
                        updateMarkerBearing(bearing, isAnimateMyLocation)
                    }
                }
            }
        }
    }

    private fun updateMarkerPosition(
        location: Location,
        target31: PointI?,
        animationDuration: Long
    ) {
        var target31point = target31
        val mapRenderer = mapRenderer
        if (mapRenderer != null && view != null && locationMarker != null && locationMarker?.marker != null) {
            if (target31point == null) {
                target31point = PointI(
                    MapUtils.get31TileNumberX(location.longitude),
                    MapUtils.get31TileNumberY(location.latitude)
                )
            }
            val animationThread = view.animatedMapMarkersThread
            locationMarker?.marker?.let {
                animationThread?.cancelCurrentAnimation(it, AnimatedValue.Target)
                if (animationDuration > 0) {
                    animationThread?.animatePositionTo(
                        it,
                        target31point,
                        animationDuration
                    )
                } else {
                    it.position = target31point
                }
            }
            locationMarker?.marker?.accuracyCircleRadius = location.accuracy.toDouble()
        }
    }

    private fun updateMarkerBearing(bearing: Float, animateRotation: Boolean) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && view != null && locationMarker != null) {
            locationMarker?.marker?.let { marker ->
                val animationThread = view.animatedMapMarkersThread
                animationThread?.cancelCurrentAnimation(marker, AnimatedValue.Azimuth)
                val bearingRotationDuration =
                    if (animateRotation && locationMarker?.onSurfaceIconKey != null) AnimateMapMarkersThread.ROTATE_ANIMATION_TIME else 0

                locationMarker?.onSurfaceIconKey?.let {
                    if (bearingRotationDuration > 0) {
                        animationThread?.animateDirectionTo(
                            marker, it,
                            bearing, bearingRotationDuration
                        )
                    } else {
                        marker.setOnMapSurfaceIconDirection(locationMarker?.onSurfaceIconKey, bearing)
                    }
                }
            }
        }
    }

    private fun updateMarkerHeading(heading: Float) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && view != null && locationMarker != null && locationMarker?.marker != null) {
            if (locationMarker?.onSurfaceHeadingIconKey != null) {
                locationMarker?.marker?.setOnMapSurfaceIconDirection(
                    locationMarker?.onSurfaceHeadingIconKey,
                    heading
                )
            }
        }
    }

    private val currentMarkerPosition: PointI? get() = locationMarker?.marker?.position
    private val currentMarkerLocation: LatLon?
        get() {
            val pos31 = currentMarkerPosition
            return if (pos31 != null) LatLon(
                MapUtils.get31LatitudeY(pos31.y),
                MapUtils.get31LongitudeX(pos31.x)
            ) else null
        }

    private fun shouldShowHeading(): Boolean {
        return !locationOutdated && mapViewTrackingUtilities?.isShowViewAngle == true
    }

    private fun shouldShowBearing(location: Location?): Boolean {
        return getBearingToShow(location) != null
    }

    private fun getBearingToShow(location: Location?): Float? {
        if (!locationOutdated && location != null) {
            // Issue 5538: Some devices return positives for hasBearing() at rest, hence add 0.0 check:
            val hasBearing = location.hasBearing() && location.bearing != 0.0f
            if ((hasBearing || isUseRouting && lastBearingCached != null)
                && (!location.hasSpeed() || location.speed > BEARING_SPEED_THRESHOLD)
            ) {
                return if (hasBearing) location.bearing else lastBearingCached
            }
        }
        return null
    }

    private val isUseRouting: Boolean
        get() {
            val routingHelper = application.routingHelper
            return (routingHelper?.isFollowingMode == true || routingHelper?.isRoutePlanningMode == true
                    || routingHelper?.isRouteBeingCalculated == true || routingHelper?.isRouteCalculated == true)
        }

    private fun isLocationVisible(tb: RotatedTileBox, l: Location): Boolean {
        return tb.containsLatLon(l.latitude, l.longitude)
    }

    private fun drawMarkers(canvas: Canvas, box: RotatedTileBox, lastKnownLocation: Location) {
        val locationX: Int
        val locationY: Int
        if (isMapLinkedToLocation
            && !isSmallSpeedForAnimation(lastKnownLocation)
            && !isMovingToMyLocation
        ) {
            locationX = box.centerPixelX
            locationY = box.centerPixelY
        } else {
            locationX = box.getPixXFromLonNoRot(lastKnownLocation.longitude)
            locationY = box.getPixYFromLatNoRot(lastKnownLocation.latitude)
        }
        val dist = box.getDistance(0, box.pixHeight / 2, box.pixWidth, box.pixHeight / 2)
        val radius = (box.pixWidth.toDouble() / dist * lastKnownLocation.accuracy).toInt()
        if (radius > RADIUS * box.density) {
            val allowedRad = min(box.pixWidth / 2, box.pixHeight / 2)
            area?.let {
                canvas.drawCircle(
                    locationX.toFloat(),
                    locationY.toFloat(),
                    min(radius, allowedRad).toFloat(),
                    it
                )
            }
            aroundArea?.let {
                canvas.drawCircle(
                    locationX.toFloat(),
                    locationY.toFloat(),
                    min(radius, allowedRad).toFloat(),
                    it
                )
            }
        }
        // draw bearing/direction/location
        if (isLocationVisible(box, lastKnownLocation)) {
            val heading = locationProvider?.heading
            if (shouldShowHeading() && heading != null) {
                canvas.save()
                canvas.rotate(heading - 180, locationX.toFloat(), locationY.toFloat())
                canvas.restore()
            }
            locationIcon?.let { drawIcon(canvas, it, locationX, locationY) }
        }
    }

    private fun drawIcon(canvas: Canvas, icon: Drawable, locationX: Int, locationY: Int) {
        var width = (icon.intrinsicWidth * layerTextScale).toInt()
        var height = (icon.intrinsicHeight * layerTextScale).toInt()
        width += if (width % 2 == 1) 1 else 0
        height += if (height % 2 == 1) 1 else 0
        if (layerTextScale == 1f) {
            icon.setBounds(
                locationX - width / 2, locationY - height / 2,
                locationX + width / 2, locationY + height / 2
            )
            icon.draw(canvas)
        } else {
            icon.setBounds(0, 0, width, height)
            val bitmap = AndroidUtils.createScaledBitmap(icon, width, height)
            canvas.drawBitmap(bitmap, locationX - width / 2f, locationY - height / 2f, bitmapPaint)
        }
    }

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings?
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        val lastKnownLocation = locationProvider?.lastStaleKnownLocation
        if (view == null || tileBox.zoom < MIN_ZOOM || lastKnownLocation == null) {
            clearMapMarkersCollections()
            return
        }
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            var markersRecreated = false
            if (markersInvalidated || mapMarkersCollection == null) {
                markersRecreated = recreateMarkerCollection()
                markersInvalidated = false
            }
            val showHeading = shouldShowHeading()
            val showBearing = shouldShowBearing(lastKnownLocation)
            val stateUpdated = setMarkerState(
                if (showBearing) MarkerState.Move else MarkerState.Stay,
                showHeading,
                markersRecreated
            )
            if (markersRecreated || stateUpdated) {
                lastBearingCached = null
                lastHeadingCached = null
                if (!isMapLinkedToLocation) {
                    updateMarker(lastKnownLocation, null, 0)
                }
            }
        }
        val nightMode = settings != null && settings.isNightMode
        view.settings?.applicationMode?.let {
            updateParams(
                it,
                nightMode,
                locationProvider?.lastKnownLocation == null
            )
        }
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) {
        val lastKnownLocation = locationProvider?.lastStaleKnownLocation
        if (view == null || tileBox.zoom < MIN_ZOOM || lastKnownLocation == null) {
            return
        }
        if (!hasMapRenderer()) {
            drawMarkers(canvas, tileBox, lastKnownLocation)
        }
    }

    override fun destroyLayer() {
        super.destroyLayer()
        locationProvider?.removeLocationListener(this)
        locationProvider?.removeCompassListener(this)
        clearMapMarkersCollections()
    }

    override fun updateLocation(location: Location?) {
        if (view == null || view.zoom < MIN_ZOOM || location == null) {
            return
        }
        val mapRenderer = mapRenderer
        prevLocation?.let {
            if (mapRenderer != null && (!isMapLinkedToLocation || isMovingToMyLocation)) {
                val dataChanged = !MapUtils.areLatLonEqualPrecise(prevLocation, location)
                if (dataChanged) {
                    val movingTime =
                        if (prevLocation != null) location.time - it.time else 0
                    updateMarker(location, null, if (isAnimateMyLocation) movingTime else 0)
                    prevLocation = location
                }
            }
        }
    }

    override fun updateCompassValue(value: Float) {
        updateMarker(null, null, 0)
    }

    private val isAnimateMyLocation: Boolean
        get() = settings?.ANIMATE_MY_LOCATION?.get() == true
    private val isMapLinkedToLocation: Boolean
        get() = mapViewTrackingUtilities?.isMapLinkedToLocation == true
    private val isMovingToMyLocation: Boolean
        get() = mapViewTrackingUtilities?.isMovingToMyLocation == true

    private fun updateParams(
        appMode: ApplicationMode,
        nighMode: Boolean,
        locationOutdated: Boolean
    ) {
        val ctx = context
        val profileColor = if (locationOutdated) ContextCompat.getColor(
            ctx,
            ProfileIconColors.getOutdatedLocationColor(nighMode)
        ) else appMode.getProfileColor(nighMode)
        val locationIconId = appMode.locationIcon.iconId
        val textScale = textScale
        if (appMode !== this.appMode || nm != nighMode || this.locationOutdated != locationOutdated || this.profileColor != profileColor || this.locationIconId != locationIconId || this.layerTextScale != textScale) {
            this.appMode = appMode
            this.profileColor = profileColor
            nm = nighMode
            this.locationOutdated = locationOutdated
            this.locationIconId = locationIconId
            this.layerTextScale = textScale
            locationIcon = AppCompatResources.getDrawable(ctx, locationIconId)
            if (!hasMapRenderer()) {
                headingPaint?.colorFilter =
                    PorterDuffColorFilter(profileColor, PorterDuff.Mode.SRC_IN)
                area?.color = ColorUtilities.getColorWithAlpha(profileColor, 0.16f)
                aroundArea?.color = profileColor
            }
            markersInvalidated = true
        }
    }

    override fun drawInScreenPixels(): Boolean {
        return false
    }

    override fun collectObjectsFromPoint(
        point: PointF,
        tileBox: RotatedTileBox,
        o: MutableList<Any>,
        unknownLocation: Boolean
    ) {
        if (tileBox.zoom >= 3) {
            getMyLocationFromPoint(tileBox, point, o)
        }
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        return myLocation
    }

    override fun getObjectName(o: Any?): PointDescription? {
        return PointDescription(
            PointDescription.POINT_TYPE_MY_LOCATION,
            context.getString(R.string.shared_string_my_location), ""
        )
    }

    override fun disableSingleTap(): Boolean {
        return false
    }

    override fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean {
        return false
    }

    override fun isObjectClickable(o: Any): Boolean {
        return false
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        return false
    }

    private val myLocation: LatLon?
        get() {
            val location = locationProvider?.lastKnownLocation
            return if (location != null) {
                LatLon(location.latitude, location.longitude)
            } else {
                null
            }
        }

    private fun getMyLocationFromPoint(
        tb: RotatedTileBox,
        point: PointF,
        myLocations: MutableList<in LatLon>
    ) {
        val location: LatLon? = myLocation
        if (location != null && view != null) {
            val ex = point.x.toInt()
            val ey = point.y.toInt()
            val pixel = NativeUtilities.getPixelFromLatLon(
                mapRenderer, tb,
                location.latitude, location.longitude
            )
            val rad = (18 * tb.density).toInt()
            if (abs(pixel.x - ex) <= rad && ey - pixel.y <= rad && pixel.y - ey <= 2.5 * rad) {
                myLocations.add(location)
            }
        }
    }

    companion object {
        protected const val BEARING_SPEED_THRESHOLD = 0.1f
        protected const val MIN_ZOOM = 3
        protected const val RADIUS = 7
        private const val MARKER_ID_MY_LOCATION = 1
    }
}