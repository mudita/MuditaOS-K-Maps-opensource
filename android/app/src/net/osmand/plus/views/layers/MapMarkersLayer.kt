package net.osmand.plus.views.layers

import com.mudita.map.common.R as commonR
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.text.TextPaint
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.mudita.maps.R
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sqrt
import net.osmand.GPXUtilities.TrkSegment
import net.osmand.GPXUtilities.WptPt
import net.osmand.Location
import net.osmand.core.jni.FColorARGB
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.QVectorPointI
import net.osmand.core.jni.Utilities
import net.osmand.core.jni.VectorDouble
import net.osmand.core.jni.VectorLineBuilder
import net.osmand.core.jni.VectorLinesCollection
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.plus.OsmAndConstants
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.MapViewTrackingUtilities.Companion.isSmallSpeedForAnimation
import net.osmand.plus.mapmarkers.MapMarker
import net.osmand.plus.render.OsmandDashPathEffect
import net.osmand.plus.routing.ColoringType
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.Renderable.RenderableSegment
import net.osmand.plus.views.Renderable.StandardTrack
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.geometry.GeometryWay
import net.osmand.plus.views.layers.geometry.GpxGeometryWay
import net.osmand.plus.views.layers.geometry.GpxGeometryWayContext
import net.osmand.util.MapUtils

open class MapMarkersLayer(context: Context) : OsmandMapLayer(context), IContextMenuProvider,
    IContextMenuProviderSelection, IMoveObjectProvider {

    private val bitmapPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    private val bitmapPaintDestBlue: Paint by lazy {
        createPaintDest(R.color.marker_blue)
    }
    private val bitmapPaintDestGreen: Paint by lazy {
        createPaintDest(R.color.marker_green)
    }
    private val bitmapPaintDestOrange: Paint by lazy {
        createPaintDest(R.color.marker_orange)
    }
    private val bitmapPaintDestRed: Paint by lazy {
        createPaintDest(R.color.marker_red)
    }
    private val bitmapPaintDestYellow: Paint by lazy {
        createPaintDest(R.color.marker_yellow)
    }
    private val bitmapPaintDestTeal: Paint by lazy {
        createPaintDest(R.color.marker_teal)
    }
    private val bitmapPaintDestPurple: Paint by lazy {
        createPaintDest(R.color.marker_purple)
    }
    private val calculations = FloatArray(2)
    private val textPaint = TextPaint()
    private val lineAttrs = RenderingLineAttributes("measureDistanceLine")
    private val textAttrs = RenderingLineAttributes("rulerLineFont")
    private val planRouteAttrs = RenderingLineAttributes("markerPlanRouteline")
    private var textSize = 0f
    private var verticalOffset = 0f
    private var hasMoved = false
    private var moving = false
    private var useFingerLocation = false
    var isInPlanRouteMode = false
    private var defaultAppMode = true
    private var carView = false
    private var layerTextScale = 1f
    private var markerSizePx = 0.0
    private var route: TrkSegment? = null
    private var fingerLocation: LatLon? = null
    private var longTapDetector: GestureDetector? = null
    private var handler: Handler? = null
    private var contextMenuLayer: ContextMenuLayer? = null
    private var markerBitmapBlue: Bitmap? = null
    private var markerBitmapGreen: Bitmap? = null
    private var markerBitmapOrange: Bitmap? = null
    private var markerBitmapRed: Bitmap? = null
    private var markerBitmapYellow: Bitmap? = null
    private var markerBitmapTeal: Bitmap? = null
    private var markerBitmapPurple: Bitmap? = null
    private var arrowLight: Bitmap? = null
    private var arrowToDestination: Bitmap? = null
    private var arrowShadow: Bitmap? = null

    //OpenGL
    private var markersCount = 0
    private var vectorLinesCollection: VectorLinesCollection? = null
    private var needDrawLines = true
    private val displayedMarkers = mutableListOf<MapMarker>()
    private var displayedWidgets = 0
    private var cachedPoints: List<WptPt>? = null
    private var cachedRenderer: RenderableSegment? = null
    private var savedLoc: Location? = null
    private var cachedTarget31: PointI? = null
    private var cachedZoom = 0
    private val cachedPaths = hashMapOf<Int, Path?>()
    private val amenities = mutableListOf<Amenity>()

    fun setDefaultAppMode(defaultAppMode: Boolean) {
        this.defaultAppMode = defaultAppMode
    }

    private fun initUI() {
        updateBitmaps(true)
        contextMenuLayer = view.getLayerByClass(ContextMenuLayer::class.java)
    }

    override fun setMapActivity(mapActivity: MapActivity?) {
        super.setMapActivity(mapActivity)
        if (mapActivity != null) {
            longTapDetector =
                GestureDetector(mapActivity, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onLongPress(e: MotionEvent) {
                        cancelFingerAction()
                    }
                })
        } else {
            longTapDetector = null
        }
    }

    private fun createPaintDest(colorId: Int): Paint {
        val paint = Paint()
        paint.isDither = true
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        val color = ContextCompat.getColor(context, colorId)
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        return paint
    }

    private fun getMarkerDestPaint(colorIndex: Int): Paint? {
        return when (colorIndex) {
            0 -> bitmapPaintDestBlue
            1 -> bitmapPaintDestGreen
            2 -> bitmapPaintDestOrange
            3 -> bitmapPaintDestRed
            4 -> bitmapPaintDestYellow
            5 -> bitmapPaintDestTeal
            6 -> bitmapPaintDestPurple
            else -> bitmapPaintDestBlue
        }
    }

    private fun getMapMarkerBitmap(colorIndex: Int): Bitmap? {
        return when (colorIndex) {
            0 -> markerBitmapBlue
            1 -> markerBitmapGreen
            2 -> markerBitmapOrange
            3 -> markerBitmapRed
            4 -> markerBitmapYellow
            5 -> markerBitmapTeal
            6 -> markerBitmapPurple
            else -> markerBitmapBlue
        }
    }

    fun setRoute(route: TrkSegment?) {
        this.route = route
    }

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        handler = Handler()
        initUI()
    }

    override fun onPrepareBufferImage(
        canvas: Canvas, tileBox: RotatedTileBox, drawSettings: DrawSettings
    ) {
        super.onPrepareBufferImage(canvas, tileBox, drawSettings)
        val app = application
        val settings = app.settings
        if (settings?.SHOW_MAP_MARKERS?.get() == false) {
            clearMapMarkersCollections()
            clearVectorLinesCollections()
            resetCachedRenderer()
            return
        }
        val markersHelper = app.mapMarkersHelper
        val activeMapMarkers = markersHelper?.mapMarkers
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            if (markersCount != activeMapMarkers?.size || mapActivityInvalidated) {
                clearMapMarkersCollections()
                clearVectorLinesCollections()
                cachedPaths.clear()
                cachedTarget31 = null
            }
            initMarkersCollection()
            activeMapMarkers?.size?.let {
                markersCount = it
            }
            mapActivityInvalidated = false
        }
        route?.let {
            if (it.points.size > 0) {
                planRouteAttrs.updatePaints(app, drawSettings, tileBox)
                if (mapRenderer != null) {
                    val shouldDraw = shouldDrawPoints()
                    if (shouldDraw || mapActivityInvalidated) {
                        resetCachedRenderer()
                        val baseOrder = getPointsOrder() - 10
                        val correctedQuadRect = getCorrectedQuadRect(tileBox.latLonBounds)
                        val renderer: RenderableSegment = StandardTrack(
                            ArrayList(
                                it.points
                            ), 17.2
                        )
                        it.renderer = renderer
                        val wayContext = view.density?.let { density ->
                            GpxGeometryWayContext(context, density)
                        }
                        val geometryWay = GpxGeometryWay(wayContext)
                        geometryWay.baseOrder = baseOrder
                        renderer.setTrackParams(
                            lineAttrs.paint.color, "", ColoringType.TRACK_SOLID, null
                        )
                        renderer.setDrawArrows(false)
                        renderer.geometryWay = geometryWay
                        cachedRenderer = renderer
                        cachedPoints = ArrayList(it.points)
                        renderer.drawGeometry(
                            canvas,
                            tileBox,
                            correctedQuadRect,
                            planRouteAttrs.paint.color,
                            planRouteAttrs.paint.strokeWidth,
                            getDashPattern(planRouteAttrs.paint)
                        )
                    }
                } else {
                    StandardTrack(ArrayList(it.points), 17.2).drawSegment(
                        view.zoom.toDouble(),
                        if (defaultAppMode) planRouteAttrs.paint else planRouteAttrs.paint2,
                        canvas,
                        tileBox
                    )
                }
            } else {
                resetCachedRenderer()
                cachedPoints = null
            }
        }
        if (settings?.SHOW_LINES_TO_FIRST_MARKERS?.get() == true && mapRenderer == null) {
            drawLineAndText(canvas, tileBox, drawSettings)
        }
    }

    private fun resetCachedRenderer() {
        cachedRenderer?.let {
            val geometryWay = it.geometryWay
            geometryWay?.resetSymbolProviders()
        }
    }

    private fun shouldDrawPoints(): Boolean {
        var shouldDraw = true
        cachedPoints?.let { cachedPoints ->
            if (cachedPoints.size == route?.points?.size) {
                shouldDraw = false
                for (i in cachedPoints.indices) {
                    var shouldBreak = false
                    route?.let {
                        if (it.points[i] != cachedPoints[i]) {
                            shouldDraw = true
                            shouldBreak = true
                        }
                    }
                    if (shouldBreak)
                        break
                }
            }
        }

        return shouldDraw
    }

    private fun getDashPattern(paint: Paint): FloatArray? {
        var intervals: FloatArray? = null
        val pathEffect = paint.pathEffect
        if (pathEffect is OsmandDashPathEffect) {
            intervals = pathEffect.intervals
        }
        return intervals
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, nightMode: DrawSettings) {
        val app = application
        val settings = app.settings
        if (tileBox.zoom < 3 || settings?.SHOW_MAP_MARKERS?.get() == false) {
            clearVectorLinesCollections()
            return
        }
        val mapRenderer = mapRenderer
        val displayedWidgets = settings?.DISPLAYED_MARKERS_WIDGETS_COUNT?.get()
        val markersHelper = app.mapMarkersHelper
        updateBitmaps(false)
        if (mapRenderer == null && markersHelper != null) {
            for (marker in markersHelper.mapMarkers) {
                if (isMarkerVisible(
                        tileBox, marker
                    ) && !overlappedByWaypoint(marker) && !isInMotion(marker) && !isSynced(marker)
                ) {
                    getMapMarkerBitmap(marker.colorIndex)?.let { bmp ->
                        val marginX = bmp.width / 2
                        val marginY = bmp.height
                        val locationX = tileBox.getPixXFromLonNoRot(marker.longitude)
                        val locationY = tileBox.getPixYFromLatNoRot(marker.latitude)
                        canvas.rotate(-tileBox.rotate, locationX.toFloat(), locationY.toFloat())
                        canvas.drawBitmap(
                            bmp,
                            (locationX - marginX).toFloat(),
                            (locationY - marginY).toFloat(),
                            bitmapPaint
                        )
                        canvas.rotate(tileBox.rotate, locationX.toFloat(), locationY.toFloat())
                    }
                }
            }
        }
        if (settings?.SHOW_LINES_TO_FIRST_MARKERS?.get() == true && mapRenderer != null) {
            drawLineAndText(canvas, tileBox, nightMode)
        } else {
            clearVectorLinesCollections()
        }
        if (settings?.SHOW_ARROWS_TO_FIRST_MARKERS?.get() == true && markersHelper != null) {
            val loc = tileBox.centerLatLon
            var i = 0
            for (marker in markersHelper.mapMarkers) {
                if (!isLocationVisible(tileBox, marker) && !isInMotion(marker)) {
                    canvas.save()
                    var bearing: Float
                    val radiusBearing = DIST_TO_SHOW * tileBox.density
                    var cx: Float
                    var cy: Float
                    if (mapRenderer != null) {
                        val marker31 =
                            NativeUtilities.getPoint31FromLatLon(marker.latitude, marker.longitude)
                        val center31 = NativeUtilities.get31FromPixel(
                            mapRenderer, tileBox, tileBox.centerPixelX, tileBox.centerPixelY
                        ) ?: continue
                        val line =
                            calculateLineInScreenRect(tileBox, marker31, center31) ?: continue
                        val centerPixel = PointF(
                            tileBox.centerPixelX.toFloat(), tileBox.centerPixelY.toFloat()
                        )
                        cx = centerPixel.x
                        cy = centerPixel.y
                        bearing = getAngleBetween(centerPixel, line[1]).toFloat() - tileBox.rotate
                    } else {
                        val cp = tileBox.centerPixelPoint
                        cx = cp.x
                        cy = cp.y
                        Location.distanceBetween(
                            loc.latitude,
                            loc.longitude,
                            marker.latitude,
                            marker.longitude,
                            calculations
                        )
                        bearing = calculations[1] - 90
                    }
                    canvas.rotate(bearing, cx, cy)
                    canvas.translate(-24 * tileBox.density + radiusBearing, -22 * tileBox.density)
                    arrowShadow?.let { canvas.drawBitmap(it, cx, cy, bitmapPaint) }
                    arrowToDestination?.let {
                        canvas.drawBitmap(
                            it, cx, cy, getMarkerDestPaint(marker.colorIndex)
                        )
                    }
                    arrowLight?.let { canvas.drawBitmap(it, cx, cy, bitmapPaint) }
                    canvas.restore()
                }
                i++
                if (displayedWidgets != null && i > displayedWidgets - 1) {
                    break
                }
            }
        }
        val movableObject = contextMenuLayer?.moveableObject
        if (movableObject is MapMarker) {
            setMovableObject(movableObject.latitude, movableObject.longitude)
            drawMovableMarker(canvas, tileBox, movableObject)
        }
        if (this.movableObject != null) {
            cancelMovableObject()
        }
    }

    private fun updateBitmaps(forceUpdate: Boolean) {
        val app = application
        val textScale = textScale
        if (this.layerTextScale != textScale || forceUpdate) {
            this.layerTextScale = textScale
            recreateBitmaps()
            textSize = app.resources.getDimensionPixelSize(R.dimen.guide_line_text_size) * textScale
            verticalOffset =
                app.resources.getDimensionPixelSize(R.dimen.guide_line_vertical_offset) * textScale
        }
    }

    private fun recreateBitmaps() {
        val marker = AppCompatResources.getDrawable(context, commonR.drawable.ic_location_white)?.toBitmap()
        markerBitmapBlue = marker
        markerBitmapGreen = marker
        markerBitmapOrange = marker
        markerBitmapRed = marker
        markerBitmapYellow = marker
        markerBitmapTeal = marker
        markerBitmapPurple = marker
        markerBitmapBlue?.let { it ->
            markerSizePx = sqrt(
                (it.width * it.width + it.height * it.height).toDouble()
            )
        }
    }

    override fun getScaledBitmap(drawableId: Int): Bitmap? =
        getScaledBitmap(drawableId, layerTextScale)

    private fun isSynced(marker: MapMarker): Boolean =
        marker.wptPt != null || marker.favouritePoint != null

    private fun isInMotion(marker: MapMarker): Boolean = marker == contextMenuLayer?.moveableObject


    fun isLocationVisible(tb: RotatedTileBox?, marker: MapMarker?): Boolean =
        if (marker == null || tb == null) {
            false
        } else containsLatLon(tb, marker.latitude, marker.longitude, 0.0, 0.0)

    fun isMarkerVisible(tb: RotatedTileBox?, marker: MapMarker?): Boolean {
        return if (marker == null || tb == null) {
            false
        } else containsLatLon(
            tb, marker.latitude, marker.longitude, markerSizePx, markerSizePx
        )
    }

    fun containsLatLon(
        tb: RotatedTileBox, lat: Double, lon: Double, w: Double, h: Double
    ): Boolean {
        var widgetHeight = 0.0
        val pixel = NativeUtilities.getPixelFromLatLon(mapRenderer, tb, lat, lon)
        val tx = pixel.x.toDouble()
        val ty = pixel.y.toDouble()
        return tx >= -w && tx <= tb.pixWidth + w && ty >= widgetHeight - h && ty <= tb.pixHeight + h
    }

    private fun overlappedByWaypoint(marker: MapMarker): Boolean {
        application.targetPointsHelper?.let {
            val targetPoints = it.allPoints
            for (t in targetPoints) {
                if (t.point == marker.point) {
                    return true
                }
            }
        }
        return false
    }

    private fun drawMovableMarker(
        canvas: Canvas, tileBox: RotatedTileBox, movableMarker: MapMarker
    ) {
        val point = contextMenuLayer?.getMovableCenterPoint(tileBox)
        getMapMarkerBitmap(movableMarker.colorIndex)?.let { bitmap ->
            val marginX = bitmap.width / 2
            val marginY = bitmap.height
            canvas.save()
            canvas.rotate(
                -tileBox.rotate, tileBox.centerPixelX.toFloat(), tileBox.centerPixelY.toFloat()
            )
            point?.let {
                canvas.drawBitmap(bitmap, it.x - marginX, it.y - marginY, bitmapPaint)
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent, tileBox: RotatedTileBox): Boolean {
        if (longTapDetector != null && longTapDetector?.onTouchEvent(event) == false) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val x = event.x
                    val y = event.y
                    fingerLocation = NativeUtilities.getLatLonFromPixel(mapRenderer, tileBox, x, y)
                    hasMoved = false
                    moving = true
                }
                MotionEvent.ACTION_MOVE -> if (!hasMoved) {
                    if (handler?.hasMessages(MAP_REFRESH_MESSAGE) == false) {
                        val msg = Message.obtain(handler) {
                            handler?.removeMessages(MAP_REFRESH_MESSAGE)
                            if (moving) {
                                if (!useFingerLocation) {
                                    useFingerLocation = true
                                    application.osmandMap?.refreshMap()
                                }
                            }
                        }
                        msg.what = MAP_REFRESH_MESSAGE
                        handler?.sendMessageDelayed(msg, USE_FINGER_LOCATION_DELAY)
                    }
                    hasMoved = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelFingerAction()
            }
        }
        return super.onTouchEvent(event, tileBox)
    }

    private fun cancelFingerAction() {
        handler?.removeMessages(MAP_REFRESH_MESSAGE)
        useFingerLocation = false
        moving = false
        fingerLocation = null
        application.osmandMap?.refreshMap()
    }

    override fun drawInScreenPixels(): Boolean = false

    override fun disableSingleTap(): Boolean = isInPlanRouteMode


    override fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean =
        isInPlanRouteMode


    override fun isObjectClickable(o: Any): Boolean = false

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        val mapActivity = mapActivity
        val settings = application.settings
        if (unknownLocation || mapActivity == null || o !is MapMarker || settings?.SELECT_MARKER_ON_SINGLE_TAP?.get() == false || settings?.SHOW_MAP_MARKERS?.get() == false) {
            return false
        }
        application.mapMarkersHelper?.moveMarkerToTop(o as MapMarker?)
        return true
    }

    override fun collectObjectsFromPoint(
        point: PointF, tileBox: RotatedTileBox, o: MutableList<Any>, unknownLocation: Boolean
    ) {
        if (tileBox.zoom < 3 || application.settings?.SHOW_MAP_MARKERS?.get() == false) {
            return
        }
        amenities.clear()
        val app = application
        val r = tileBox.defaultRadiusPoi
        val selectMarkerOnSingleTap = app.settings?.SELECT_MARKER_ON_SINGLE_TAP?.get() ?: false
        app.mapMarkersHelper?.mapMarkers?.let { mapMarkers ->
            for (marker in mapMarkers) {
                if (!unknownLocation && selectMarkerOnSingleTap || !isSynced(marker)) {
                    val latLon = marker.point
                    if (latLon != null) {
                        val pixel = NativeUtilities.getPixelFromLatLon(
                            mapRenderer, tileBox, latLon.latitude, latLon.longitude
                        )
                        if (calculateBelongs(
                                point.x.toInt(),
                                point.y.toInt(),
                                pixel.x.toInt(),
                                pixel.y.toInt(),
                                r
                            )
                        ) {
                            if (!unknownLocation && selectMarkerOnSingleTap) {
                                o.add(marker)
                            } else {
                                if (isMarkerOnFavorite(marker) && app.settings?.SHOW_FAVORITES?.get() == false || isMarkerOnWaypoint(
                                        marker
                                    ) && app.settings?.SHOW_WPT?.get() == false
                                ) {
                                    continue
                                }
                                val mapObj = getMapObjectByMarker(marker)
                                if (mapObj != null) {
                                    amenities.add(mapObj)
                                    o.add(mapObj)
                                } else {
                                    o.add(marker)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isMarkerOnWaypoint(marker: MapMarker): Boolean =
        marker.point != null && application.selectedGpxHelper?.getVisibleWayPointByLatLon(
            marker.point
        ) != null


    private fun isMarkerOnFavorite(marker: MapMarker): Boolean =
        marker.point != null && application.favoritesHelper?.getVisibleFavByLatLon(marker.point) != null


    fun getMapObjectByMarker(marker: MapMarker): Amenity? {
        if (marker.mapObjectName != null && marker.point != null) {
            val mapObjName = marker.mapObjectName.split("_").toTypedArray()[0]
            return MapSelectionHelper.findAmenity(
                application, marker.point, listOf(mapObjName), -1, 15
            )
        }
        return null
    }

    private fun calculateBelongs(ex: Int, ey: Int, objx: Int, objy: Int, radius: Int): Boolean {
        return abs(objx - ex) <= radius * 1.5 && ey - objy <= radius * 1.5 && objy - ey <= 2.5 * radius
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        if (o is MapMarker) {
            return o.point
        } else if (o is Amenity && amenities.contains(o)) {
            return o.location
        }
        return null
    }

    override fun getObjectName(o: Any?): PointDescription? {
        return if (o is MapMarker) {
            o.getPointDescription(context)
        } else null
    }

    override fun getOrder(o: Any?): Int {
        return 0
    }

    override fun setSelectedObject(o: Any?) {}
    override fun clearSelectedObject() {}
    override fun isObjectMovable(o: Any?): Boolean {
        return o is MapMarker
    }

    override fun applyNewObjectPosition(
        o: Any?,
        position: LatLon,
        callback: ApplyMovedObjectCallback?
    ) {
        var result = false
        var newObject: MapMarker? = null
        if (o is MapMarker) {
            val markersHelper = application.mapMarkersHelper
            val originalDescription = o.originalPointDescription
            if (originalDescription.isLocation) {
                originalDescription.name = PointDescription.getSearchAddressStr(context)
            }
            markersHelper?.moveMapMarker(o, position)
            val index = markersHelper?.mapMarkers?.indexOf(o)
            if (index != null && index != -1) {
                newObject = markersHelper.mapMarkers[index]
            }
            result = true
            if (displayedMarkers.contains(o)) {
                clearVectorLinesCollections()
            }
        }
        callback?.onApplyMovedObject(result, newObject ?: o)
        applyMovableObject(position)
    }

    /**
     * OpenGL
     */
    private fun initMarkersCollection() {
        val mapRenderer = mapRenderer ?: return
        if (mapMarkersCollection != null && mapRenderer.hasSymbolsProvider(mapMarkersCollection)) {
            return
        }
        mapMarkersCollection = MapMarkersCollection()
        val markersHelper = application.mapMarkersHelper
        updateBitmaps(false)
        markersHelper?.let {
            for (marker in markersHelper.mapMarkers) {
                if (!overlappedByWaypoint(marker) && !isSynced(marker)) {
                    getMapMarkerBitmap(marker.colorIndex)?.let { bmp ->
                        val mapMarkerBuilder = MapMarkerBuilder()
                        val pointI =
                            NativeUtilities.getPoint31FromLatLon(marker.latitude, marker.longitude)
                        val color = getColorByIndex(marker.colorIndex)
                        val isMoveable = isInMotion(marker)
                        mapMarkerBuilder.setIsAccuracyCircleSupported(false)
                            .setBaseOrder(getPointsOrder()).setIsHidden(isMoveable)
                            .setPinIcon(NativeUtilities.createSkImageFromBitmap(bmp))
                            .setPosition(pointI)
                            .setPinIconVerticalAlignment(net.osmand.core.jni.MapMarker.PinIconVerticalAlignment.Top)
                            .setPinIconHorisontalAlignment(net.osmand.core.jni.MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
                            .setPinIconOffset(PointI(bmp.width / 3, 0))
                            .setAccuracyCircleBaseColor(NativeUtilities.createFColorRGB(color))
                            .buildAndAddToCollection(mapMarkersCollection)
                    }
                }
            }
        }
        mapRenderer.addSymbolsProvider(mapMarkersCollection)
    }

    /**
     * OpenGL
     */
    private fun initVectorLinesCollection(
        loc: LatLon, marker: MapMarker, color: Int, isLast: Boolean
    ) {
        val mapRenderer = mapRenderer
        if (mapRenderer == null || !needDrawLines) {
            return
        }
        val start = NativeUtilities.getPoint31FromLatLon(loc.latitude, loc.longitude)
        val end = NativeUtilities.getPoint31FromLatLon(marker.latitude, marker.longitude)
        if (vectorLinesCollection == null) {
            vectorLinesCollection = VectorLinesCollection()
        }
        val points = QVectorPointI()
        points.add(start)
        points.add(end)
        val outlineBuilder = VectorLineBuilder()
        val outlinePattern = VectorDouble()
        outlinePattern.add(75.0 / mapDensity.toDouble())
        outlinePattern.add(55.0 / mapDensity.toDouble())
        val outlineColor = FColorARGB(1.0f, 1.0f, 1.0f, 1.0f)
        val strokeWidth = 20.0
        val outlineId = if (isLast) 20 else 10
        val lineId = if (isLast) 21 else 11
        outlineBuilder.setBaseOrder(baseOrder + lineId + 1).setIsHidden(false).setLineId(outlineId)
            .setLineWidth(strokeWidth * 1.5).setLineDash(outlinePattern)
            .setPoints(points).fillColor = outlineColor
        outlineBuilder.buildAndAddToCollection(vectorLinesCollection)
        val inlineBuilder = VectorLineBuilder()
        val inlinePattern = VectorDouble()
        inlinePattern.add(-strokeWidth / 2 / mapDensity)
        inlinePattern.add((75 - strokeWidth) / mapDensity)
        inlinePattern.add((55 + strokeWidth) / mapDensity)
        inlineBuilder.setBaseOrder(baseOrder + lineId).setIsHidden(false).setLineId(lineId)
            .setLineWidth(strokeWidth).setLineDash(inlinePattern).setPoints(points).fillColor =
            NativeUtilities.createFColorARGB(color)
        inlineBuilder.buildAndAddToCollection(vectorLinesCollection)
        if (isLast) {
            mapRenderer.addSymbolsProvider(vectorLinesCollection)
            needDrawLines = false
        }
    }

    /**
     * OpenGL
     */
    protected fun clearVectorLinesCollections() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && vectorLinesCollection != null) {
            mapRenderer.removeSymbolsProvider(vectorLinesCollection)
            vectorLinesCollection = null
            needDrawLines = true
        }
    }

    private fun calculateLineInScreenRect(
        tileBox: RotatedTileBox, marker: MapMarker, loc: LatLon
    ): Array<PointF>? {
        val locPointI = NativeUtilities.getPoint31FromLatLon(loc.latitude, loc.longitude)
        val markerPointI = NativeUtilities.getPoint31FromLatLon(marker.latitude, marker.longitude)
        return calculateLineInScreenRect(tileBox, markerPointI, locPointI)
    }

    /**
     * OpenGL
     */
    private fun calculateLineInScreenRect(
        tileBox: RotatedTileBox, markerPointI: PointI, locPointI: PointI
    ): Array<PointF>? {
        val mapRenderer = mapRenderer ?: return null
        val screenBbox = mapRenderer.visibleBBox31
        var firstPoint: PointI? = null
        var secondPoint: PointI? = null
        if (screenBbox.contains(locPointI)) {
            firstPoint = locPointI
        }
        if (screenBbox.contains(markerPointI)) {
            secondPoint = markerPointI
        }
        if (firstPoint == null && secondPoint == null) {
            firstPoint = PointI(0, 0)
            secondPoint = PointI(0, 0)
            if (Utilities.calculateIntersection(locPointI, markerPointI, screenBbox, firstPoint)) {
                Utilities.calculateIntersection(markerPointI, locPointI, screenBbox, secondPoint)
            } else {
                return null
            }
        } else if (firstPoint == null) {
            firstPoint = PointI(0, 0)
            if (!Utilities.calculateIntersection(locPointI, markerPointI, screenBbox, firstPoint)) {
                return null
            }
        } else if (secondPoint == null) {
            secondPoint = PointI(0, 0)
            if (!Utilities.calculateIntersection(
                    markerPointI, locPointI, screenBbox, secondPoint
                )
            ) {
                return null
            }
        }
        val l = NativeUtilities.getPixelFrom31(mapRenderer, tileBox, firstPoint)
        val m = NativeUtilities.getPixelFrom31(mapRenderer, tileBox, secondPoint!!)
        return arrayOf(l, m)
    }

    /**
     * OpenGL
     */
    private fun getAngleBetween(start: PointF, end: PointF): Double {
        val dx = (start.x - end.x).toDouble()
        val dy = (start.y - end.y).toDouble()
        val radians = if (dx != 0.0) atan(dy / dx) else if (dy < 0) Math.PI / 2 else -Math.PI / 2
        return Math.toDegrees(radians - if (start.x > end.x) Math.PI else 0.0)
    }

    @ColorInt
    private fun getColorByIndex(colorIndex: Int): Int {
        val colorResId: Int = when (colorIndex) {
            1 -> R.color.marker_green
            2 -> R.color.marker_orange
            3 -> R.color.marker_red
            4 -> R.color.marker_yellow
            5 -> R.color.marker_teal
            6 -> R.color.marker_purple
            else -> R.color.marker_blue
        }
        return context.resources.getColor(colorResId, null)
    }

    private fun drawLineAndText(canvas: Canvas, tileBox: RotatedTileBox, nightMode: DrawSettings) {
        val myLoc: Location?
        val app = application
        val mapRenderer = mapRenderer
        if (useFingerLocation) {
            myLoc = Location("")
            fingerLocation?.let {
                myLoc.latitude = it.latitude
                myLoc.longitude = it.longitude
            }
        } else {
            myLoc = app.locationProvider?.lastStaleKnownLocation
        }
        if (myLoc == null) {
            clearVectorLinesCollections()
            return
        }
        if (savedLoc != null && !MapUtils.areLatLonEqual(myLoc, savedLoc)) {
            clearVectorLinesCollections()
        }
        savedLoc = myLoc
        val settings = app.settings
        val markersHelper = app.mapMarkersHelper
        val activeMapMarkers = markersHelper?.mapMarkers
        if (settings?.DISPLAYED_MARKERS_WIDGETS_COUNT?.get() != null && displayedWidgets != settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get()) {
            displayedWidgets = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get()
            clearVectorLinesCollections()
        } else {
            var i = 0
            activeMapMarkers?.let {
                while (mapRenderer != null && i < activeMapMarkers.size && i < displayedMarkers.size) {
                    if (displayedMarkers[i] !== activeMapMarkers[i]) {
                        clearVectorLinesCollections()
                        break
                    }
                    i++
                }
            }
        }

        displayedMarkers.clear()
        textAttrs.paint.textSize = textSize
        textAttrs.paint2.textSize = textSize
        lineAttrs.updatePaints(app, nightMode, tileBox)
        textAttrs.updatePaints(app, nightMode, tileBox)
        textAttrs.paint.style = Paint.Style.FILL
        textPaint.set(textAttrs.paint)
        val drawMarkerName = settings?.DISPLAYED_MARKERS_WIDGETS_COUNT?.get() == 1
        var locX: Float
        var locY: Float
        val loc: LatLon
        val mapViewTrackingUtilities = app.mapViewTrackingUtilities
        loc =
            if (mapViewTrackingUtilities?.isMapLinkedToLocation == true && !isSmallSpeedForAnimation(
                    myLoc
                ) && !mapViewTrackingUtilities.isMovingToMyLocation
            ) {
                LatLon(tileBox.latitude, tileBox.longitude)
            } else {
                LatLon(myLoc.latitude, myLoc.longitude)
            }
        val colors = MapMarker.getColors(context)
        var i = 0
        activeMapMarkers?.let {
            while (i < activeMapMarkers.size && i < displayedWidgets) {
                val tx: MutableList<Float> = ArrayList()
                val ty: MutableList<Float> = ArrayList()
                var linePath = Path()
                val marker = activeMapMarkers[i]
                var markerX: Float
                var markerY: Float
                val color = colors[marker.colorIndex]
                if (mapRenderer != null) {
                    val isLast = i == activeMapMarkers.size - 1 || i == displayedWidgets - 1
                    //draw line in OpenGL
                    initVectorLinesCollection(loc, marker, color, isLast)
                    displayedMarkers.add(marker)
                    val line = calculateLineInScreenRect(tileBox, marker, loc)
                    if (line != null) {
                        locX = line[0].x
                        locY = line[0].y
                        markerX = line[1].x
                        markerY = line[1].y
                    } else {
                        i++
                        continue
                    }
                } else {
                    locX = tileBox.getPixXFromLatLon(loc.latitude, loc.longitude)
                    locY = tileBox.getPixYFromLatLon(loc.latitude, loc.longitude)
                    markerX = tileBox.getPixXFromLatLon(marker.latitude, marker.longitude)
                    markerY = tileBox.getPixYFromLatLon(marker.latitude, marker.longitude)
                }
                if (mapRenderer != null) {
                    val target31 = mapRenderer.target
                    if (cachedTarget31 != null && cachedTarget31?.x == target31.x && cachedTarget31?.y == target31.y) {
                        cachedPaths.clear()
                    }
                    cachedTarget31 = target31
                    if (view.zoom != cachedZoom) {
                        cachedPaths.clear()
                        cachedZoom = view.zoom
                    }
                }
                if (mapRenderer == null || !cachedPaths.containsKey(i)) {
                    tx.add(locX)
                    ty.add(locY)
                    tx.add(markerX)
                    ty.add(markerY)
                    GeometryWay.calculatePath(tileBox, tx, ty, linePath)
                    cachedPaths[i] = linePath
                } else {
                    cachedPaths[i]?.let {
                        linePath = it
                    }
                }
                val pm = PathMeasure(linePath, false)
                val pos = FloatArray(2)
                pm.getPosTan(pm.length / 2, pos, null)
                val dist = MapUtils.getDistance(
                    myLoc.latitude, myLoc.longitude, marker.latitude, marker.longitude
                ).toFloat()
                var text = if (drawMarkerName) " • " + marker.getName(context) else ""
                text =
                    TextUtils.ellipsize(text, textPaint, pm.length, TextUtils.TruncateAt.END)
                        .toString()
                val bounds = Rect()
                textAttrs.paint.getTextBounds(text, 0, text.length, bounds)
                val hOffset = pm.length / 2 - bounds.width() / 2f
                lineAttrs.paint.color = color
                canvas.rotate(
                    -tileBox.rotate, tileBox.centerPixelX.toFloat(), tileBox.centerPixelY.toFloat()
                )
                if (mapRenderer == null) {
                    canvas.drawPath(linePath, lineAttrs.paint)
                }
                if (locX >= markerX) {
                    canvas.rotate(180f, pos[0], pos[1])
                    canvas.drawTextOnPath(
                        text, linePath, hOffset, bounds.height() + verticalOffset, textAttrs.paint2
                    )
                    canvas.drawTextOnPath(
                        text, linePath, hOffset, bounds.height() + verticalOffset, textAttrs.paint
                    )
                    canvas.rotate(-180f, pos[0], pos[1])
                } else {
                    canvas.drawTextOnPath(
                        text,
                        linePath,
                        hOffset,
                        -verticalOffset,
                        textAttrs.paint2
                    )
                    canvas.drawTextOnPath(text, linePath, hOffset, -verticalOffset, textAttrs.paint)
                }
                canvas.rotate(
                    tileBox.rotate, tileBox.centerPixelX.toFloat(), tileBox.centerPixelY.toFloat()
                )
                i++
            }
        }
    }

    companion object {
        private const val USE_FINGER_LOCATION_DELAY: Long = 1000
        private const val MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 6
        protected const val DIST_TO_SHOW = 80
    }
}