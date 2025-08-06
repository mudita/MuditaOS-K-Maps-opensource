package net.osmand.plus.measurementtool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import androidx.core.content.ContextCompat
import com.mudita.maps.R
import net.osmand.GPXUtilities
import net.osmand.Location
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.MapMarker
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.Utilities
import net.osmand.data.DataTileManager
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.plus.ChartPointsHelper
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints
import net.osmand.plus.measurementtool.MeasurementEditingContext.AdditionMode
import net.osmand.plus.render.OsmandDashPathEffect
import net.osmand.plus.routing.ColoringType
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.Renderable.RenderableSegment
import net.osmand.plus.views.Renderable.StandardTrack
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.core.LocationPointsTileProvider
import net.osmand.plus.views.layers.core.TilePointsProvider
import net.osmand.plus.views.layers.geometry.GeometryWay
import net.osmand.plus.views.layers.geometry.GpxGeometryWay
import net.osmand.plus.views.layers.geometry.GpxGeometryWayContext
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWayContext
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

class MeasurementToolLayer(ctx: Context) : OsmandMapLayer(ctx),
    IContextMenuProvider {

    private val centerIconDay: Bitmap by lazy {
        BitmapFactory.decodeResource(view.resources, R.drawable.map_ruler_center_day)
    }
    private val centerIconNight: Bitmap by lazy {
        BitmapFactory.decodeResource(view.resources, R.drawable.map_ruler_center_night)
    }
    private val pointIcon: Bitmap by lazy {
        BitmapFactory.decodeResource(view.resources, R.drawable.map_measure_point_day)
    }
    private val oldMovedPointIcon: Bitmap by lazy {
        BitmapFactory.decodeResource(view.resources, R.drawable.map_measure_point_day_disable)
    }
    private val applyingPointIcon: Bitmap by lazy {
        BitmapFactory.decodeResource(view.resources, R.drawable.map_measure_point_move_day)
    }
    private val highlightedPointImage: Bitmap by lazy {
        chartPointsHelper.createHighlightedPointBitmap()
    }
    private val chartPointsHelper: ChartPointsHelper by lazy {
        ChartPointsHelper(context)
    }
    private val multiProfileGeometry: MultiProfileGeometryWay by lazy {
        MultiProfileGeometryWay(multiProfileGeometryWayContext).apply {
            baseOrder -= 10
        }
    }
    private val bitmapPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
    }
    private val path = Path()
    private val lineAttrs = RenderingLineAttributes("measureDistanceLine")
    private val multiProfilePath = Path()
    private val multiProfilePathMeasure = PathMeasure(multiProfilePath, false)
    private var beforePointsCountCached = 0
    private var afterPointsCountCached = 0
    private var originalPointsCountCached = 0
    private var marginPointIconX = 0
    private var marginPointIconY = 0
    private var marginApplyingPointIconX = 0
    private var marginApplyingPointIconY = 0
    private var showPointsZoomCache = 0
    var isInMeasurementMode = false
    var isTapsDisabled = false
    private var showPointsMinZoom = false
    private var oldMovedPointRedraw = false
    private var segmentsRenderablesCached: List<RenderableSegment> = listOf()
    private var approximationRenderablesCached: List<RenderableSegment> = listOf()
    private val tx: MutableList<Float> = mutableListOf()
    private val ty: MutableList<Float> = mutableListOf()
    private var beforeAfterWpt: List<GPXUtilities.WptPt> = listOf()
    private var xAxisPointsCached: List<LatLon?> = listOf()
    private var multiProfileGeometryWayContext: MultiProfileGeometryWayContext? = null
    private var wayContext: GpxGeometryWayContext? = null
    private var pressedPointLatLon: LatLon? = null
    private var activePointsCollection: MapMarkersCollection? = null
    private var centerPointMarker: MapMarker? = null
    private var beforePointMarker: MapMarker? = null
    private var afterPointMarker: MapMarker? = null
    private var selectedPointMarker: MapMarker? = null
    private var trackChartPointsProvider: LocationPointsTileProvider? = null
    private var highlightedPointCollection: MapMarkersCollection? = null
    private var highlightedPointMarker: MapMarker? = null
    private var pointsProvider: TilePointsProvider<WptCollectionPoint>? = null
    private var beforeAfterRenderer: RenderableSegment? = null
    private var measureDistanceToCenterListener: OnMeasureDistanceToCenter? = null
    private var singleTapListener: OnSingleTapListener? = null
    private var enterMovePointModeListener: OnEnterMovePointModeListener? = null
    private var trackChartPoints: TrackChartPoints? = null
    var editingCtx: MeasurementEditingContext? = null

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        view.density?.let {
            multiProfileGeometryWayContext = MultiProfileGeometryWayContext(
                context,
                view.application?.uIUtilities, it
            )
            wayContext = GpxGeometryWayContext(context, it)
        }
        marginPointIconY = pointIcon.height / 2
        marginPointIconX = pointIcon.width / 2
        marginApplyingPointIconY = applyingPointIcon.height / 2
        marginApplyingPointIconX = applyingPointIcon.width / 2
    }

    fun setOnSingleTapListener(listener: OnSingleTapListener?) {
        singleTapListener = listener
    }

    fun setOnEnterMovePointModeListener(listener: OnEnterMovePointModeListener?) {
        enterMovePointModeListener = listener
    }

    fun setOnMeasureDistanceToCenterListener(listener: OnMeasureDistanceToCenter?) {
        measureDistanceToCenterListener = listener
    }

    fun setTrackChartPoints(trackChartPoints: TrackChartPoints?) {
        this.trackChartPoints = trackChartPoints
    }

    override fun onSingleTap(point: PointF, tileBox: RotatedTileBox): Boolean {
        if (isInMeasurementMode && !isTapsDisabled && editingCtx?.selectedPointPosition == -1) {
            val pointSelected = showPointsMinZoom && selectPoint(point.x, point.y, true)
            val profileIconSelected = !pointSelected && selectPointForAppModeChange(point, tileBox)
            if (!pointSelected && !profileIconSelected) {
                pressedPointLatLon =
                    NativeUtilities.getLatLonFromPixel(mapRenderer, tileBox, point.x, point.y)
                singleTapListener?.onAddPoint()
            }
        }
        return false
    }

    private fun selectPointForAppModeChange(point: PointF, tileBox: RotatedTileBox): Boolean {
        val pointIdx = getPointIdxByProfileIconOnMap(point, tileBox)
        if (pointIdx != -1 && singleTapListener != null) {
            editingCtx?.selectedPointPosition = pointIdx
            singleTapListener?.onSelectProfileIcon(pointIdx)
            return true
        }
        return false
    }

    private fun getPointIdxByProfileIconOnMap(point: PointF, tileBox: RotatedTileBox): Int {
        multiProfilePath.reset()
        val roadSegmentData = editingCtx?.roadSegmentData
        val points = editingCtx?.points
        var minDist =
            view.resources?.getDimension(R.dimen.measurement_tool_select_radius)?.toDouble()
        var indexOfMinDist = -1
        points?.let {
            for (i in 0 until points.size - 1) {
                val currentPoint = points[i]
                val nextPoint = points[i + 1]
                if (currentPoint.isGap) {
                    continue
                }
                val routeBetweenPoints = MultiProfileGeometryWay.getRoutePoints(
                    currentPoint, nextPoint, roadSegmentData
                )
                val profileIconPos = MultiProfileGeometryWay.getIconCenter(
                    tileBox, routeBetweenPoints,
                    path, multiProfilePathMeasure
                )
                if (profileIconPos != null && tileBox.containsPoint(
                        profileIconPos.x,
                        profileIconPos.y,
                        0f
                    )
                ) {
                    val dist =
                        MapUtils.getSqrtDistance(point.x, point.y, profileIconPos.x, profileIconPos.y)
                    minDist?.let {
                        if (dist < it) {
                            indexOfMinDist = i
                            minDist = dist
                        }
                    }
                }
            }
        }
        return indexOfMinDist
    }

    override fun onLongPressEvent(point: PointF, tileBox: RotatedTileBox): Boolean {
        editingCtx?.let {
            if (isInMeasurementMode && !isTapsDisabled) {
                if (showPointsMinZoom
                    && it.selectedPointPosition == -1 && it.pointsCount > 0
                ) {
                    selectPoint(point.x, point.y, false)
                    if (it.selectedPointPosition != -1) {
                        enterMovingPointMode()
                        enterMovePointModeListener?.onEnterMovePointMode()
                    }
                }
            }
        }
        return false
    }

    fun enterMovingPointMode() {
        editingCtx?.let {
            moveMapToPoint(it.selectedPointPosition)
            val pt = it.removePoint(it.selectedPointPosition, false)
            oldMovedPointRedraw = true
            it.originalPointToMove = pt
            it.splitSegments(it.selectedPointPosition)
        }
    }

    private fun selectPoint(x: Float, y: Float, singleTap: Boolean): Boolean {
        val tb = view.currentRotatedTileBox
        tb?.let {
            var lowestDistance =
                view.resources?.getDimension(R.dimen.measurement_tool_select_radius)?.toDouble()
            editingCtx?.let { ctx ->
                for (i in 0 until ctx.pointsCount) {
                    val pt = ctx.points[i]
                    val mapRenderer = mapRenderer
                    if (mapRenderer != null) {
                        val point31 =
                            Utilities.convertLatLonTo31(net.osmand.core.jni.LatLon(pt.lat, pt.lon))
                        if (mapRenderer.isPositionVisible(point31)) {
                            val pixel = NativeUtilities.getPixelFromLatLon(
                                mapRenderer,
                                tb, pt.lat, pt.lon
                            )
                            val distToPoint = MapUtils.getSqrtDistance(x, y, pixel.x, pixel.y)
                            lowestDistance?.let {
                                if (distToPoint < it) {
                                    lowestDistance = distToPoint
                                    ctx.selectedPointPosition = i
                                }
                            }
                        }
                    } else {
                        if (tb.containsLatLon(pt.latitude, pt.longitude)) {
                            val ptX = tb.getPixXFromLatLon(pt.lat, pt.lon)
                            val ptY = tb.getPixYFromLatLon(pt.lat, pt.lon)
                            val distToPoint = MapUtils.getSqrtDistance(x, y, ptX, ptY)
                            lowestDistance?.let {
                                if (distToPoint < it) {
                                    lowestDistance = distToPoint
                                    ctx.selectedPointPosition = i
                                }
                            }
                        }
                    }
                }
                if (singleTap) {
                    singleTapListener?.onSelectPoint(ctx.selectedPointPosition)
                }
            }
        }
        return editingCtx?.selectedPointPosition != -1
    }

    fun selectPoint(position: Int) {
        editingCtx?.selectedPointPosition = position
        editingCtx?.let {
            singleTapListener?.onSelectPoint(it.selectedPointPosition)
        }
    }

    override fun onPrepareBufferImage(canvas: Canvas, tb: RotatedTileBox, settings: DrawSettings) {
        super.onPrepareBufferImage(canvas, tb, settings)
        val hasMapRenderer = hasMapRenderer()
        if (isDrawingEnabled) {
            val updated =
                lineAttrs.updatePaints(view.application, settings, tb) || mapActivityInvalidated
            editingCtx?.let { ctx ->
                if (ctx.isInApproximationMode) {
                    drawApproximatedLines(canvas, tb, updated)
                }
                if (ctx.isInMultiProfileMode) {
                    if (hasMapRenderer) {
                        clearCachedSegmentsPointsCounters()
                        clearCachedSegmentsRenderables()
                    }
                    multiProfileGeometryWayContext?.let {
                        var changed = it.setNightMode(settings.isNightMode)
                        changed = changed or multiProfileGeometry.updateRoute(
                            tb, ctx.roadSegmentData,
                            ctx.beforeSegments, ctx.afterSegments
                        )
                        changed = changed or mapActivityInvalidated
                        if (hasMapRenderer) {
                            if (changed) {
                                multiProfileGeometry.resetSymbolProviders()
                                multiProfileGeometry.drawSegments(canvas, tb)
                            }
                        } else {
                            multiProfileGeometry.drawSegments(canvas, tb)
                        }
                    }
                } else {
                    multiProfileGeometry.clearWay()
                    drawSegmentLines(canvas, tb, updated)
                }
            }
            canvas.rotate(-tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
            drawPoints(canvas, tb)
            if (hasMapRenderer) {
                if (updated) {
                    recreateHighlightedPointCollection()
                }
                mapRenderer?.let { mRend ->
                    drawTrackChartPointsOpenGl(trackChartPoints, mRend, tb)
                }
            } else {
                drawTrackChartPoints(trackChartPoints, canvas, tb)
            }
            canvas.rotate(tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
        } else if (hasMapRenderer) {
            clearCachedCounters()
            clearCachedRenderables()
            clearPointsProvider()
            multiProfileGeometry.clearWay()
        }
        mapActivityInvalidated = false
    }

    private fun getDashPattern(paint: Paint): FloatArray? {
        var intervals: FloatArray? = null
        val pathEffect = paint.pathEffect
        if (pathEffect is OsmandDashPathEffect) {
            intervals = pathEffect.intervals
        }
        return intervals
    }

    override fun onDraw(canvas: Canvas, tb: RotatedTileBox, settings: DrawSettings) {
        val hasMapRenderer = hasMapRenderer()
        editingCtx?.let { ctx ->
            if (isDrawingEnabled) {
                val updated = lineAttrs.updatePaints(view.application, settings, tb)
                if (!ctx.isInApproximationMode) {
                    drawBeforeAfterPath(canvas, tb, updated)
                } else {
                    resetBeforeAfterRenderer()
                }
                if (hasMapRenderer) {
                    if (updated || activePointsCollection == null) {
                        recreateActivePointsCollection(settings.isNightMode)
                    }
                }
                if (ctx.selectedPointPosition == -1) {
                    if (hasMapRenderer) {
                        if (centerPointMarker != null) {
                            centerPointMarker?.position = PointI(tb.center31X, tb.center31Y)
                            centerPointMarker?.setIsHidden(false)
                        }
                    } else {
                        drawCenterIcon(canvas, tb, settings.isNightMode)
                    }
                    if (measureDistanceToCenterListener != null) {
                        var distance = 0f
                        var bearing = 0f
                        if (ctx.pointsCount > 0) {
                            val lastPoint = ctx.points[ctx.pointsCount - 1]
                            val centerLatLon = tb.centerLatLon
                            distance = MapUtils.getDistance(
                                lastPoint.lat,
                                lastPoint.lon,
                                centerLatLon.latitude,
                                centerLatLon.longitude
                            ).toFloat()
                            bearing = getLocationFromLL(lastPoint.lat, lastPoint.lon)
                                .bearingTo(
                                    getLocationFromLL(
                                        centerLatLon.latitude,
                                        centerLatLon.longitude
                                    )
                                )
                        }
                        measureDistanceToCenterListener?.onMeasure(distance, bearing)
                    }
                } else if (hasMapRenderer && centerPointMarker != null) {
                    centerPointMarker?.setIsHidden(true)
                }
                val beforePoints = ctx.beforePoints
                val afterPoints = ctx.afterPoints
                if (beforePoints.size > 0) {
                    val point = beforePoints[beforePoints.size - 1]
                    if (hasMapRenderer) {
                        if (beforePointMarker != null) {
                            beforePointMarker?.position = PointI(
                                MapUtils.get31TileNumberX(point.longitude),
                                MapUtils.get31TileNumberY(point.latitude)
                            )
                            beforePointMarker?.setIsHidden(false)
                        }
                    } else {
                        drawPointIcon(canvas, tb, point, true)
                    }
                } else if (hasMapRenderer && beforePointMarker != null) {
                    beforePointMarker?.setIsHidden(true)
                }
                if (afterPoints.size > 0) {
                    val point = afterPoints[0]
                    if (hasMapRenderer) {
                        if (afterPointMarker != null) {
                            afterPointMarker?.position = PointI(
                                MapUtils.get31TileNumberX(point.longitude),
                                MapUtils.get31TileNumberY(point.latitude)
                            )
                            afterPointMarker?.setIsHidden(false)
                        }
                    } else {
                        drawPointIcon(canvas, tb, point, true)
                    }
                } else if (hasMapRenderer && afterPointMarker != null) {
                    afterPointMarker?.setIsHidden(true)
                }
                if (ctx.selectedPointPosition != -1) {
                    if (hasMapRenderer) {
                        if (selectedPointMarker != null) {
                            selectedPointMarker?.position = PointI(tb.center31X, tb.center31Y)
                            selectedPointMarker?.setIsHidden(false)
                        }
                    } else {
                        canvas.rotate(-tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
                        val locX = tb.centerPixelX
                        val locY = tb.centerPixelY
                        canvas.drawBitmap(
                            applyingPointIcon,
                            (locX - marginApplyingPointIconX).toFloat(),
                            (locY - marginApplyingPointIconY).toFloat(),
                            bitmapPaint
                        )
                        canvas.rotate(tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
                    }
                } else if (hasMapRenderer && selectedPointMarker != null) {
                    selectedPointMarker?.setIsHidden(true)
                }
            } else if (hasMapRenderer) {
                resetBeforeAfterRenderer()
                clearActivePointsCollection()
            }
        }
    }

    private val isDrawingEnabled: Boolean
        get() {
            val mapActivity = mapActivity
            return mapActivity != null
        }

    private fun isInTileBox(tb: RotatedTileBox, point: GPXUtilities.WptPt): Boolean {
        val latLonBounds = tb.latLonBounds
        return point.latitude >= latLonBounds.bottom && point.latitude <= latLonBounds.top && point.longitude >= latLonBounds.left && point.longitude <= latLonBounds.right
    }

    private fun drawSegmentLines(canvas: Canvas, tb: RotatedTileBox, forceDraw: Boolean) {
        editingCtx?.let { ctx ->
            val beforeSegments: List<GPXUtilities.TrkSegment> = ArrayList(
                ctx.beforeTrkSegmentLine
            )
            val afterSegments: List<GPXUtilities.TrkSegment> = ArrayList(
                ctx.afterTrkSegmentLine
            )
            val segments: MutableList<GPXUtilities.TrkSegment> = ArrayList(beforeSegments)
            segments.addAll(afterSegments)
            val mapRenderer = mapRenderer
            if (mapRenderer != null) {
                val beforePointsCount = GPXUtilities.calculateTrackPoints(beforeSegments)
                val afterPointsCount = GPXUtilities.calculateTrackPoints(afterSegments)
                var draw = forceDraw
                draw = draw or (beforePointsCountCached != beforePointsCount)
                draw = draw or (afterPointsCountCached != afterPointsCount)
                clearCachedSegmentsPointsCounters()
                beforePointsCountCached = beforePointsCount
                afterPointsCountCached = afterPointsCount
                val cached: MutableList<RenderableSegment> = ArrayList()
                if (draw) {
                    clearCachedSegmentsRenderables()
                    var baseOrder = baseOrder - 10
                    val correctedQuadRect = getCorrectedQuadRect(tb.latLonBounds)
                    for (segment in segments) {
                        var renderer: RenderableSegment? = null
                        if (segment.points.isNotEmpty()) {
                            renderer = StandardTrack(ArrayList(segment.points), 17.2)
                            segment.renderer = renderer
                            val geometryWay = GpxGeometryWay(wayContext)
                            geometryWay.baseOrder = baseOrder--
                            renderer.setTrackParams(
                                lineAttrs.paint.color,
                                "",
                                ColoringType.TRACK_SOLID,
                                null
                            )
                            renderer.setDrawArrows(false)
                            renderer.setGeometryWay(geometryWay)
                            cached.add(renderer)
                        }
                        renderer?.drawGeometry(
                            canvas, tb, correctedQuadRect, lineAttrs.paint.color,
                            lineAttrs.paint.strokeWidth, getDashPattern(lineAttrs.paint)
                        )
                    }
                    segmentsRenderablesCached = cached
                }
            } else {
                for (segment in segments) {
                    StandardTrack(ArrayList(segment.points), 17.2)
                        .drawSegment(view.zoom.toDouble(), lineAttrs.paint, canvas, tb)
                }
            }
        }
    }

    private fun drawApproximatedLines(canvas: Canvas, tb: RotatedTileBox, forceDraw: Boolean) {
        val mapRenderer = mapRenderer
        val originalPointsList = editingCtx?.originalSegmentPointsList
        originalPointsList?.let {
            if (!Algorithms.isEmpty(originalPointsList)) {
                val color =
                    ContextCompat.getColor(context, R.color.activity_background_transparent_color_dark)
                if (mapRenderer != null) {
                    var originalPointsCount = 0
                    for (points in originalPointsList) {
                        originalPointsCount += points.size
                    }
                    var draw = forceDraw
                    draw = draw or (originalPointsCountCached != originalPointsCount)
                    clearCachedOriginalPointsCounter()
                    originalPointsCountCached = originalPointsCount
                    val cached: MutableList<RenderableSegment> = ArrayList()
                    if (draw) {
                        clearCachedApproximationRenderables()
                        var baseOrder = baseOrder - 10
                        val correctedQuadRect = getCorrectedQuadRect(tb.latLonBounds)
                        for (points in originalPointsList) {
                            var renderer: RenderableSegment? = null
                            if (points.isNotEmpty()) {
                                renderer = StandardTrack(ArrayList(points), 17.2)
                                val geometryWay = GpxGeometryWay(wayContext)
                                geometryWay.baseOrder = baseOrder--
                                renderer.setTrackParams(color, "", ColoringType.TRACK_SOLID, null)
                                renderer.setDrawArrows(false)
                                renderer.setGeometryWay(geometryWay)
                                cached.add(renderer)
                            }
                            renderer?.drawGeometry(
                                canvas, tb, correctedQuadRect, color,
                                lineAttrs.paint.strokeWidth, getDashPattern(lineAttrs.paint)
                            )
                        }
                        approximationRenderablesCached = cached
                    }
                } else {
                    lineAttrs.customColorPaint.color = color
                    for (points in originalPointsList) {
                        StandardTrack(ArrayList(points), 17.2).drawSegment(
                            view.zoom.toDouble(),
                            lineAttrs.customColorPaint,
                            canvas,
                            tb
                        )
                    }
                }
            } else if (mapRenderer != null) {
                clearCachedRenderables()
            }
        }
    }

    private fun clearCachedCounters() {
        clearCachedSegmentsPointsCounters()
        clearCachedOriginalPointsCounter()
    }

    private fun clearCachedSegmentsPointsCounters() {
        afterPointsCountCached = 0
        beforePointsCountCached = 0
    }

    private fun clearCachedOriginalPointsCounter() {
        originalPointsCountCached = 0
    }

    private fun clearCachedRenderables() {
        clearCachedSegmentsRenderables()
        clearCachedApproximationRenderables()
    }

    private fun clearCachedSegmentsRenderables() {
        clearCachedRenderables(segmentsRenderablesCached)
        segmentsRenderablesCached = ArrayList()
    }

    private fun clearCachedApproximationRenderables() {
        clearCachedRenderables(approximationRenderablesCached)
        approximationRenderablesCached = ArrayList()
    }

    private fun clearCachedRenderables(cached: List<RenderableSegment>) {
        for (renderer in cached) {
            val geometryWay = renderer.geometryWay
            geometryWay?.resetSymbolProviders()
        }
    }

    private fun drawTrackChartPoints(
        trackChartPoints: TrackChartPoints?,
        canvas: Canvas, tileBox: RotatedTileBox
    ) {
        if (trackChartPoints != null) {
            val highlightedPoint = trackChartPoints.highlightedPoint
            if (highlightedPoint != null) {
                chartPointsHelper.drawHighlightedPoint(highlightedPoint, canvas, tileBox)
            }
            val xAxisPoint = trackChartPoints.xAxisPoints
            if (!Algorithms.isEmpty(xAxisPoint)) {
                chartPointsHelper.drawXAxisPoints(
                    xAxisPoint,
                    lineAttrs.defaultColor,
                    canvas,
                    tileBox
                )
            }
        }
    }

    private fun drawPoints(canvas: Canvas, tileBox: RotatedTileBox) {
        val zoom = tileBox.zoom
        val mapRenderer = mapRenderer
        editingCtx?.let { ctx ->
            if (showPointsZoomCache != zoom || mapActivityInvalidated || oldMovedPointRedraw) {
                val points: MutableList<GPXUtilities.WptPt> = ArrayList(
                    ctx.beforePoints
                )
                points.addAll(ctx.afterPoints)
                showPointsZoomCache = zoom
                val showPointsMinZoom =
                    points.size > 0 && calcZoomToShowPoints(points, showPointsZoomCache)
                if (showPointsMinZoom && !this.showPointsMinZoom && mapRenderer != null || oldMovedPointRedraw) {
                    clearPointsProvider()
                    val tilePoints = DataTileManager<WptCollectionPoint>(zoom)
                    if (oldMovedPointRedraw && ctx.originalPointToMove != null) {
                        val point = ctx.originalPointToMove
                        tilePoints.registerObject(
                            point.latitude, point.longitude,
                            WptCollectionPoint(point, oldMovedPointIcon)
                        )
                    }
                    for (point in points) {
                        tilePoints.registerObject(
                            point.latitude, point.longitude,
                            WptCollectionPoint(point, pointIcon)
                        )
                    }
                    view.density?.let { density ->
                        pointsProvider = TilePointsProvider(
                            context, tilePoints,
                            getPointsOrder() - 500, false, null, textScale, density,
                            START_ZOOM, 31
                        )
                    }
                    mapRenderer?.let { mRend ->
                        pointsProvider?.drawSymbols(mRend)
                    }
                }
                this.showPointsMinZoom = showPointsMinZoom
                oldMovedPointRedraw = false
            }
            if (showPointsMinZoom) {
                if (!hasMapRenderer()) {
                    drawPoints(canvas, tileBox, ctx.beforePoints)
                    drawPoints(canvas, tileBox, ctx.afterPoints)
                }
            } else {
                clearPointsProvider()
            }
        }
    }

    private fun clearPointsProvider() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && pointsProvider != null) {
            pointsProvider?.clearSymbols(mapRenderer)
            pointsProvider = null
        }
    }

    private fun drawPoints(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        points: List<GPXUtilities.WptPt>
    ) {
        for (i in points.indices) {
            val point = points[i]
            if (isInTileBox(tileBox, point)) {
                drawPointIcon(canvas, tileBox, point, false)
            }
        }
    }

    private fun calcZoomToShowPoints(points: List<GPXUtilities.WptPt>, currentZoom: Int): Boolean {
        val distances: MutableList<Double> = ArrayList()
        if (currentZoom >= 21) {
            return true
        }
        if (currentZoom < START_ZOOM) {
            return false
        }
        var prev: GPXUtilities.WptPt? = null
        for (wptPt in points) {
            if (prev != null) {
                val dist = MapUtils.getDistance(wptPt.lat, wptPt.lon, prev.lat, prev.lon)
                distances.add(dist)
            }
            prev = wptPt
        }
        distances.sort()
        val dist = Algorithms.getPercentile(distances, MIN_POINTS_PERCENTILE)
        val zoomMultiplier = 1 shl currentZoom - START_ZOOM
        return dist > MIN_DISTANCE_TO_SHOW_REF_ZOOM / zoomMultiplier
    }

    private fun drawBeforeAfterPath(canvas: Canvas, tb: RotatedTileBox, forceDraw: Boolean) {
        val hasMapRenderer = hasMapRenderer()
        canvas.rotate(-tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
        editingCtx?.let { ctx ->
            val before = ctx.beforeSegments
            val after = ctx.afterSegments
            if (before.size > 0 || after.size > 0) {
                path.reset()
                tx.clear()
                ty.clear()
                val beforeAfterWpt: MutableList<GPXUtilities.WptPt> = ArrayList()
                val centerWpt = GPXUtilities.WptPt()
                centerWpt.lat = tb.centerLatLon.latitude
                centerWpt.lon = tb.centerLatLon.longitude
                var hasPointsBefore = false
                if (before.size > 0) {
                    val segment = before[before.size - 1]
                    if (segment.points.size > 0) {
                        hasPointsBefore = true
                        val pt = segment.points[segment.points.size - 1]
                        if (!pt.isGap || ctx.isInAddPointMode && !ctx.isInAddPointBeforeMode) {
                            if (hasMapRenderer) {
                                beforeAfterWpt.add(pt)
                            } else {
                                tx.add(tb.getPixXFromLatLon(pt.lat, pt.lon))
                                ty.add(tb.getPixYFromLatLon(pt.lat, pt.lon))
                            }
                        }
                        if (hasMapRenderer) {
                            beforeAfterWpt.add(centerWpt)
                        } else {
                            tx.add(tb.centerPixelX.toFloat())
                            ty.add(tb.centerPixelY.toFloat())
                        }
                    }
                }
                if (after.size > 0 && !isLastPointOfSegmentSelected) {
                    val segment = after[0]
                    if (segment.points.size > 0) {
                        if (!hasPointsBefore) {
                            if (hasMapRenderer) {
                                beforeAfterWpt.add(centerWpt)
                            } else {
                                tx.add(tb.centerPixelX.toFloat())
                                ty.add(tb.centerPixelY.toFloat())
                            }
                        }
                        val pt = segment.points[0]
                        if (hasMapRenderer) {
                            beforeAfterWpt.add(pt)
                        } else {
                            tx.add(tb.getPixXFromLatLon(pt.lat, pt.lon))
                            ty.add(tb.getPixYFromLatLon(pt.lat, pt.lon))
                        }
                    }
                }

                if (tx.isNotEmpty() && ty.isNotEmpty()) {
                    GeometryWay.calculatePath(tb, tx, ty, path)
                    canvas.drawPath(path, lineAttrs.paint)
                }
                if (beforeAfterWpt.isNotEmpty()) {
                    if (!Algorithms.objectEquals(this.beforeAfterWpt, beforeAfterWpt)) {
                        var renderer = beforeAfterRenderer
                        val geometryWay: GpxGeometryWay
                        if (renderer != null) {
                            geometryWay = renderer.geometryWay
                        } else {
                            geometryWay = GpxGeometryWay(wayContext)
                            geometryWay.baseOrder = baseOrder - 100
                        }
                        renderer = StandardTrack(ArrayList(beforeAfterWpt), 17.2)
                        renderer.setTrackParams(
                            lineAttrs.paint.color,
                            "",
                            ColoringType.TRACK_SOLID,
                            null
                        )
                        renderer.setDrawArrows(false)
                        renderer.setGeometryWay(geometryWay)
                        renderer.drawGeometry(
                            canvas, tb, tb.latLonBounds, lineAttrs.paint.color,
                            lineAttrs.paint.strokeWidth, getDashPattern(lineAttrs.paint)
                        )
                        beforeAfterRenderer = renderer
                    }
                } else {
                    resetBeforeAfterRenderer()
                }
                this.beforeAfterWpt = beforeAfterWpt
                canvas.rotate(tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
            } else {
                resetBeforeAfterRenderer()
            }
        }
    }

    private val isLastPointOfSegmentSelected: Boolean
        get() = editingCtx?.originalPointToMove != null && editingCtx?.originalPointToMove?.isGap == true

    private fun resetBeforeAfterRenderer() {
        if (beforeAfterRenderer != null) {
            val geometryWay = beforeAfterRenderer?.geometryWay
            geometryWay?.resetSymbolProviders()
            beforeAfterRenderer = null
        }
    }

    private fun recreateActivePointsCollection(nightMode: Boolean) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            clearActivePointsCollection()
            activePointsCollection = MapMarkersCollection()
            // Center marker
            var builder = MapMarkerBuilder()
            builder.baseOrder = getPointsOrder() - 600
            builder.setIsAccuracyCircleSupported(false)
            builder.setIsHidden(true)
            builder.pinIcon =
                NativeUtilities.createSkImageFromBitmap((if (nightMode) centerIconNight else centerIconDay))
            centerPointMarker = builder.buildAndAddToCollection(activePointsCollection)
            mapRenderer.addSymbolsProvider(activePointsCollection)
            // Before marker
            builder = MapMarkerBuilder()
            builder.baseOrder = getPointsOrder() - 600
            builder.setIsAccuracyCircleSupported(false)
            builder.setIsHidden(true)
            builder.pinIcon = NativeUtilities.createSkImageFromBitmap(pointIcon)
            beforePointMarker = builder.buildAndAddToCollection(activePointsCollection)
            // After marker
            builder = MapMarkerBuilder()
            builder.baseOrder = getPointsOrder() - 600
            builder.setIsAccuracyCircleSupported(false)
            builder.setIsHidden(true)
            builder.pinIcon = NativeUtilities.createSkImageFromBitmap(pointIcon)
            afterPointMarker = builder.buildAndAddToCollection(activePointsCollection)
            // Selected marker
            builder = MapMarkerBuilder()
            builder.baseOrder = getPointsOrder() - 600
            builder.setIsAccuracyCircleSupported(false)
            builder.setIsHidden(true)
            builder.pinIcon = NativeUtilities.createSkImageFromBitmap(applyingPointIcon)
            selectedPointMarker = builder.buildAndAddToCollection(activePointsCollection)
            mapRenderer.addSymbolsProvider(activePointsCollection)
        }
    }

    private fun clearActivePointsCollection() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && activePointsCollection != null) {
            mapRenderer.removeSymbolsProvider(activePointsCollection)
            activePointsCollection = null
        }
    }

    private fun drawCenterIcon(canvas: Canvas, tb: RotatedTileBox, nightMode: Boolean) {
        canvas.rotate(-tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
        val centerBmp = if (nightMode) centerIconNight else centerIconDay
        canvas.drawBitmap(
            centerBmp, tb.centerPixelX - centerBmp.width / 2f,
            tb.centerPixelY - centerBmp.height / 2f, bitmapPaint
        )
        canvas.rotate(tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
    }

    private fun drawPointIcon(
        canvas: Canvas,
        tb: RotatedTileBox,
        pt: GPXUtilities.WptPt,
        rotate: Boolean
    ) {
        if (rotate) {
            canvas.rotate(-tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
        }
        val locX = tb.getPixXFromLatLon(pt.lat, pt.lon)
        val locY = tb.getPixYFromLatLon(pt.lat, pt.lon)
        if (tb.containsPoint(locX, locY, 0f)) {
            canvas.drawBitmap(
                pointIcon,
                locX - marginPointIconX,
                locY - marginPointIconY,
                bitmapPaint
            )
        }
        if (rotate) {
            canvas.rotate(tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
        }
    }

    fun addCenterPoint(addPointBefore: Boolean): GPXUtilities.WptPt? {
        val tb = view.currentRotatedTileBox
        if (tb != null) {
            val l = tb.centerLatLon
            val pt = GPXUtilities.WptPt()
            pt.lat = l.latitude
            pt.lon = l.longitude
            val allowed = editingCtx?.let { ctx ->
                ctx.pointsCount == 0 || ctx.points[ctx.pointsCount - 1] != pt
            }
            if (allowed != null && allowed) {
                editingCtx?.addPoint(
                    pt,
                    if (addPointBefore) AdditionMode.ADD_BEFORE else AdditionMode.ADD_AFTER
                )
                return pt
            }
        }
        return null
    }

    fun addPoint(addPointBefore: Boolean): GPXUtilities.WptPt? {
        if (pressedPointLatLon != null) {
            val pt = GPXUtilities.WptPt()
            val lat = pressedPointLatLon?.latitude
            val lon = pressedPointLatLon?.longitude
            if (lat != null && lon != null) {
                pt.lat = lat
                pt.lon = lon
                pressedPointLatLon = null
                val allowed = editingCtx?.let { ctx ->
                    ctx.pointsCount == 0 || ctx.points[ctx.pointsCount - 1] != pt
                }
                if (allowed != null && allowed) {
                    editingCtx?.addPoint(
                        pt,
                        if (addPointBefore) AdditionMode.ADD_BEFORE else AdditionMode.ADD_AFTER
                    )
                    moveMapToLatLon(lat, lon)
                    return pt
                }
            }
        }
        return null
    }

    val movedPointToApply: GPXUtilities.WptPt
        get() {
            val tb = view.currentRotatedTileBox
            val latLon = tb?.centerLatLon
            val originalPoint = editingCtx?.originalPointToMove
            val point = GPXUtilities.WptPt(originalPoint)
            latLon?.let {
                point.lat = latLon.latitude
                point.lon = latLon.longitude
                point.copyExtensions(originalPoint)
            }
            return point
        }

    fun exitMovePointMode() {
        oldMovedPointRedraw = true
    }

    private fun moveMapToLatLon(lat: Double, lon: Double) {
        view.animatedDraggingThread?.startMoving(lat, lon, view.zoom, true)
    }

    fun moveMapToPoint(pos: Int) {
        var position = pos
        editingCtx?.let { ctx ->
            if (ctx.pointsCount > 0) {
                if (position >= ctx.pointsCount) {
                    position = ctx.pointsCount - 1
                } else if (position < 0) {
                    position = 0
                }
                val pt = ctx.points[position]
                moveMapToLatLon(pt.latitude, pt.longitude)
            }
        }
    }

    private fun drawTrackChartPointsOpenGl(
        chartPoints: TrackChartPoints?, mapRenderer: MapRendererView,
        tileBox: RotatedTileBox
    ) {
        if (chartPoints != null) {
            val highlightedPoint = trackChartPoints?.highlightedPoint
            var highlightedPosition: PointI? = null
            if (highlightedPoint != null) {
                highlightedPosition = PointI(
                    MapUtils.get31TileNumberX(highlightedPoint.longitude),
                    MapUtils.get31TileNumberY(highlightedPoint.latitude)
                )
            }
            val highlightedMarkerPosition =
                if (highlightedPointMarker != null) highlightedPointMarker?.position else null
            val highlightedPositionChanged =
                highlightedPosition != null && highlightedMarkerPosition != null && (highlightedPosition.x != highlightedMarkerPosition.x
                        || highlightedPosition.y != highlightedMarkerPosition.y)
            if (highlightedPosition == null) {
                if (highlightedPointMarker != null) {
                    highlightedPointMarker?.setIsHidden(true)
                }
            } else if (highlightedPositionChanged) {
                if (highlightedPointMarker != null) {
                    highlightedPointMarker?.position = highlightedPosition
                    highlightedPointMarker?.setIsHidden(false)
                }
            }
            val xAxisPoints = chartPoints.xAxisPoints
            if (Algorithms.objectEquals(xAxisPointsCached, xAxisPoints)
                && trackChartPointsProvider != null && !mapActivityInvalidated
            ) {
                return
            }
            xAxisPointsCached = xAxisPoints
            clearXAxisPoints()
            if (!Algorithms.isEmpty(xAxisPoints)) {
                val pointBitmap = chartPointsHelper.createXAxisPointBitmap(
                    lineAttrs.defaultColor,
                    tileBox.density
                )
                trackChartPointsProvider =
                    LocationPointsTileProvider(getPointsOrder() - 500, xAxisPoints, pointBitmap)
                trackChartPointsProvider?.drawPoints(mapRenderer)
            }
        } else {
            xAxisPointsCached = ArrayList()
            clearXAxisPoints()
            if (highlightedPointMarker != null) {
                highlightedPointMarker?.setIsHidden(true)
            }
        }
    }

    private fun clearXAxisPoints() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && trackChartPointsProvider != null) {
            trackChartPointsProvider?.clearPoints(mapRenderer)
            trackChartPointsProvider = null
        }
    }

    private fun recreateHighlightedPointCollection() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            clearHighlightedPointCollection()
            highlightedPointCollection = MapMarkersCollection()
            val builder = MapMarkerBuilder()
            builder.baseOrder = getPointsOrder() - 600
            builder.setIsAccuracyCircleSupported(false)
            builder.setIsHidden(true)
            builder.pinIcon = NativeUtilities.createSkImageFromBitmap(highlightedPointImage)
            highlightedPointMarker = builder.buildAndAddToCollection(highlightedPointCollection)
            mapRenderer.addSymbolsProvider(highlightedPointCollection)
        }
    }

    private fun clearHighlightedPointCollection() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && highlightedPointCollection != null) {
            mapRenderer.removeSymbolsProvider(highlightedPointCollection)
            highlightedPointCollection = null
        }
    }

    fun refreshMap() {
        showPointsZoomCache = 0
        showPointsMinZoom = false
        view.refreshMap()
    }

    override fun destroyLayer() {
        super.destroyLayer()
        clearCachedCounters()
        clearCachedRenderables()
        clearPointsProvider()
        multiProfileGeometry.clearWay()
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
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        return null
    }

    override fun getObjectName(o: Any?): PointDescription? {
        return null
    }

    override fun disableSingleTap(): Boolean {
        return isInMeasurementMode
    }

    override fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean {
        return isInMeasurementMode
    }

    override fun isObjectClickable(o: Any): Boolean {
        return !isInMeasurementMode
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        return false
    }

    private fun getLocationFromLL(lat: Double, lon: Double): Location {
        val l = Location("")
        l.latitude = lat
        l.longitude = lon
        return l
    }

    interface OnSingleTapListener {
        fun onAddPoint()
        fun onSelectPoint(selectedPointPos: Int)
        fun onSelectProfileIcon(startPointPos: Int)
    }

    interface OnEnterMovePointModeListener {
        fun onEnterMovePointMode()
    }

    interface OnMeasureDistanceToCenter {
        fun onMeasure(distance: Float, bearing: Float)
    }

    companion object {
        private const val START_ZOOM = 8
        private const val MIN_POINTS_PERCENTILE = 20

        // roughly 10 points per tile
        private val MIN_DISTANCE_TO_SHOW_REF_ZOOM = MapUtils.getTileDistanceWidth(
            START_ZOOM.toFloat()
        ) / 10
    }
}