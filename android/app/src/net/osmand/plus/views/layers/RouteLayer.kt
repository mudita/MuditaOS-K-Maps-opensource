package net.osmand.plus.views.layers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.mudita.map.common.model.routing.RouteDirectionInfo
import com.mudita.maps.R
import kotlin.math.abs
import net.osmand.Location
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.data.TransportStop
import net.osmand.plus.ChartPointsHelper
import net.osmand.plus.OsmandApplication
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints
import net.osmand.plus.profiles.LocationIcon
import net.osmand.plus.routing.ColoringType
import net.osmand.plus.routing.ColoringTypeAvailabilityCache
import net.osmand.plus.routing.RouteService
import net.osmand.plus.routing.RoutingHelper
import net.osmand.plus.routing.RoutingHelperUtils
import net.osmand.plus.routing.TransportRoutingHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BooleanUtils.toNotNull
import net.osmand.plus.utils.LetUtils.safeLet
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.base.BaseRouteLayer
import net.osmand.plus.views.layers.core.LocationPointsTileProvider
import net.osmand.plus.views.layers.geometry.PublicTransportGeometryWay
import net.osmand.plus.views.layers.geometry.PublicTransportGeometryWayContext
import net.osmand.plus.views.layers.geometry.RouteGeometryWay
import net.osmand.plus.views.layers.geometry.RouteGeometryWayContext
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

class RouteLayer(context: Context) : BaseRouteLayer(context), IContextMenuProvider {
    val helper: RoutingHelper?
    private val transportHelper: TransportRoutingHelper
    private var currentAnimatedRoute = 0
    var lastRouteProjection: Location? = null
        private set
    private var lastFixedLocation: Location? = null
    private val chartPointsHelper: ChartPointsHelper
    private var trackChartPoints: TrackChartPoints? = null
    private var attrsPT: RenderingLineAttributes? = null
    private var attrsW: RenderingLineAttributes? = null
    private var routeWayContext: RouteGeometryWayContext? = null
    private var publicTransportWayContext: PublicTransportGeometryWayContext? = null
    private var routeGeometry: RouteGeometryWay? = null
    private var publicTransportRouteGeometry: PublicTransportGeometryWay? = null
    private val coloringAvailabilityCache: ColoringTypeAvailabilityCache
    private var projectionIcon: LayerDrawable? = null

    //OpenGL
    private val renderState = RenderState()
    private var trackChartPointsProvider: LocationPointsTileProvider? = null
    private var highlightedPointCollection: MapMarkersCollection? = null
    private var highlightedPointLocationCached: LatLon? = null
    private var xAxisPointsCached: List<LatLon?> = ArrayList()

    init {
        val app = context.applicationContext as OsmandApplication
        helper = app.routingHelper
        transportHelper = helper!!.transportRoutingHelper
        chartPointsHelper = ChartPointsHelper(app)
        coloringAvailabilityCache = ColoringTypeAvailabilityCache(app)
    }

    fun setTrackChartPoints(trackChartPoints: TrackChartPoints?) {
        this.trackChartPoints = trackChartPoints
    }

    override fun initAttrs(density: Float) {
        super.initAttrs(density)
        attrsPT = RenderingLineAttributes("publicTransportLine")
        attrsPT?.defaultWidth = (12 * density).toInt()
        attrsPT?.defaultWidth3 = (7 * density).toInt()
        attrsPT?.defaultColor = ContextCompat.getColor(context, R.color.nav_track)
        attrsPT?.paint3?.strokeCap = Paint.Cap.BUTT
        attrsPT?.paint3?.color = Color.WHITE
        attrsPT?.paint2?.strokeCap = Paint.Cap.BUTT
        attrsPT?.paint2?.color = Color.BLACK
        attrsW = RenderingLineAttributes("walkingRouteLine")
        attrsW?.defaultWidth = (12 * density).toInt()
        attrsW?.defaultWidth3 = (7 * density).toInt()
        attrsW?.defaultColor = ContextCompat.getColor(context, R.color.nav_track_walk_fill)
        attrsW?.paint3?.strokeCap = Paint.Cap.BUTT
        attrsW?.paint3?.color = Color.WHITE
        attrsW?.paint2?.strokeCap = Paint.Cap.BUTT
        attrsW?.paint2?.color = Color.BLACK
    }

    override fun initGeometries(density: Float) {
        routeWayContext = RouteGeometryWayContext(context, density)
        routeWayContext?.updatePaints(nightMode, attrs)
        routeGeometry = RouteGeometryWay(routeWayContext)
        routeGeometry?.baseOrder = baseOrder
        publicTransportWayContext = PublicTransportGeometryWayContext(context, density)
        safeLet(attrsPT, attrsW) { attrsPT, attrsW ->
            publicTransportWayContext?.updatePaints(nightMode, attrs, attrsPT, attrsW)
        }
        publicTransportRouteGeometry = PublicTransportGeometryWay(publicTransportWayContext)
        publicTransportRouteGeometry?.baseOrder = baseOrder
    }

    override fun areMapRendererViewEventsAllowed(): Boolean {
        return true
    }

    override fun onUpdateFrame(mapRenderer: MapRendererView) {
        super.onUpdateFrame(mapRenderer)
        if (hasMapRenderer() && helper?.isPublicTransportMode?.not().toNotNull()
            && helper?.finalLocation != null && helper.route.isCalculated
        ) {
            application.runInUIThread { drawLocations(null, view.rotatedTileBox) }
        }
    }

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        val mapRenderer = mapRenderer
        if (helper?.isPublicTransportMode.toNotNull() && transportHelper.routes != null ||
            helper?.finalLocation != null && helper.route.isCalculated
        ) {
            updateRouteColoringType()
            updateAttrs(settings, tileBox)
            drawLocations(canvas, tileBox)
            if (mapRenderer == null) {
                drawXAxisPoints(trackChartPoints, canvas, tileBox)
            } else {
                if (highlightedPointCollection == null || mapActivityInvalidated) {
                    recreateHighlightedPointCollection()
                }
                drawXAxisPointsOpenGl(trackChartPoints, mapRenderer, tileBox)
            }
        } else {
            resetLayer()
        }
        mapActivityInvalidated = false
    }

    private fun drawLocations(canvas: Canvas?, tileBox: RotatedTileBox?) {
        tileBox?.let {
            val w = tileBox.pixWidth
            val h = tileBox.pixHeight
            val lastProjection = helper?.lastProjection
            val cp: RotatedTileBox?
            if (lastProjection != null &&
                tileBox.containsLatLon(lastProjection.latitude, lastProjection.longitude)
            ) {
                cp = tileBox.copy()
                cp.increasePixelDimensions(w / 2, h)
            } else {
                cp = tileBox
            }
            val latlonRect = cp?.latLonBounds
            val correctedQuadRect = getCorrectedQuadRect(latlonRect)
            drawLocations(
                tileBox,
                canvas,
                correctedQuadRect.top,
                correctedQuadRect.left,
                correctedQuadRect.bottom,
                correctedQuadRect.right
            )
        }
    }

    private fun useMapCenter(): Boolean {
        val settings = application.settings
        val mapViewTrackingUtilities = application.mapViewTrackingUtilities
        return (settings?.ANIMATE_MY_LOCATION?.get().toNotNull(true)
                && mapViewTrackingUtilities?.isMovingToMyLocation?.not().toNotNull()
                && mapViewTrackingUtilities?.isMapLinkedToLocation.toNotNull())
    }

    private fun drawXAxisPoints(
        chartPoints: TrackChartPoints?, canvas: Canvas,
        tileBox: RotatedTileBox
    ) {
        if (chartPoints != null) {
            canvas.rotate(
                -tileBox.rotate,
                tileBox.centerPixelX.toFloat(),
                tileBox.centerPixelY.toFloat()
            )
            val xAxisPoints = chartPoints.xAxisPoints
            if (!Algorithms.isEmpty(xAxisPoints)) {
                chartPointsHelper.drawXAxisPoints(xAxisPoints, attrs.defaultColor, canvas, tileBox)
            }
            val highlightedPoint = chartPoints.highlightedPoint
            if (highlightedPoint != null) {
                chartPointsHelper.drawHighlightedPoint(highlightedPoint, canvas, tileBox)
            }
            canvas.rotate(
                tileBox.rotate,
                tileBox.centerPixelX.toFloat(),
                tileBox.centerPixelY.toFloat()
            )
        }
    }

    private fun drawXAxisPointsOpenGl(
        chartPoints: TrackChartPoints?, mapRenderer: MapRendererView,
        tileBox: RotatedTileBox
    ) {
        if (chartPoints != null) {
            if (trackChartPoints != null && trackChartPoints?.highlightedPoint != null) {
                trackChartPoints?.highlightedPoint?.let {
                    if (!Algorithms.objectEquals(highlightedPointLocationCached, it)) {
                        highlightedPointLocationCached = it
                    }
                }
            }
            val xAxisPoints = chartPoints.xAxisPoints
            if (Algorithms.objectEquals(
                    xAxisPointsCached,
                    xAxisPoints
                ) && trackChartPointsProvider != null && !mapActivityInvalidated
            ) {
                return
            }
            xAxisPointsCached = xAxisPoints
            clearXAxisPoints()
            if (!Algorithms.isEmpty(xAxisPoints)) {
                val pointBitmap =
                    chartPointsHelper.createXAxisPointBitmap(attrs.defaultColor, tileBox.density)
                trackChartPointsProvider =
                    LocationPointsTileProvider(getPointsOrder() - 500, xAxisPoints, pointBitmap)
                trackChartPointsProvider?.drawPoints(mapRenderer)
            }
        } else {
            xAxisPointsCached = ArrayList()
            highlightedPointLocationCached = null
            clearXAxisPoints()
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
            if (highlightedPointCollection != null) {
                mapRenderer.removeSymbolsProvider(highlightedPointCollection)
            }
            highlightedPointCollection = MapMarkersCollection()
            val builder = MapMarkerBuilder()
            builder.baseOrder = getPointsOrder() - 600
            builder.setIsAccuracyCircleSupported(false)
            builder.setIsHidden(true)
            builder.pinIcon = NativeUtilities.createSkImageFromBitmap(
                chartPointsHelper.createHighlightedPointBitmap()
            )
            mapRenderer.addSymbolsProvider(highlightedPointCollection)
        }
    }

    override fun updateAttrs(settings: DrawSettings?, tileBox: RotatedTileBox) {
        val updatePaints = attrs.updatePaints(view.application, settings, tileBox)
        attrs.isPaint3 = false
        attrs.isPaint2 = false
        attrsPT?.updatePaints(view.application, settings, tileBox)
        attrsPT?.isPaint3 = false
        attrsPT?.isPaint2 = false
        attrsW?.updatePaints(view.application, settings, tileBox)
        attrsPT?.isPaint3 = false
        attrsPT?.isPaint2 = false
        nightMode = settings != null && settings.isNightMode
        if (updatePaints) {
            routeWayContext?.updatePaints(nightMode, attrs)
            publicTransportWayContext?.updatePaints(nightMode, attrs, attrsPT!!, attrsW!!)
        }
    }

    private fun drawActionArrows(
        tb: RotatedTileBox?,
        canvas: Canvas?,
        actionPoints: List<Location?>
    ) {
        tb?.let { tb ->
            if (actionPoints.isNotEmpty()) {
                canvas?.rotate(-tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
                try {
                    val routeWidth = routeGeometry?.defaultWayStyle?.getWidth(0) ?: 0f
                    val pth = Path()
                    val matrix = Matrix()
                    var first = true
                    var x = 0f
                    var px = 0f
                    var py = 0f
                    var y = 0f
                    for (i in actionPoints.indices) {
                        val o = actionPoints[i]
                        if (o == null) {
                            first = true
                            val defaultTurnArrowColor = attrs.paint3.color
                            if (customTurnArrowColor != 0) {
                                attrs.paint3.color = customTurnArrowColor
                            }
                            if (routeWidth != 0f) {
                                attrs.paint3.strokeWidth = routeWidth / 2
                            }
                            canvas?.drawPath(pth, attrs.paint3)
                            drawTurnArrow(canvas, matrix, x, y, px, py)
                            attrs.paint3.color = defaultTurnArrowColor
                        } else {
                            px = x
                            py = y
                            x = tb.getPixXFromLatLon(o.latitude, o.longitude)
                            y = tb.getPixYFromLatLon(o.latitude, o.longitude)
                            if (first) {
                                pth.reset()
                                pth.moveTo(x, y)
                                first = false
                            } else {
                                pth.lineTo(x, y)
                            }
                        }
                    }
                } finally {
                    canvas?.rotate(tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
                }
            }
        }
    }

    private fun drawProjectionPoint(canvas: Canvas?, projectionXY: DoubleArray) {
        if (projectionIcon == null) {
            projectionIcon = AppCompatResources.getDrawable(
                context,
                LocationIcon.DEFAULT.iconId
            ) as LayerDrawable?
        }
        if (projectionIcon != null) {
            val locationX = projectionXY[0].toInt()
            val locationY = projectionXY[1].toInt()
            drawIcon(canvas, projectionIcon, locationX, locationY)
        }
    }

    private fun drawLocations(
        tb: RotatedTileBox,
        canvas: Canvas?,
        topLatitude: Double,
        leftLongitude: Double,
        bottomLatitude: Double,
        rightLongitude: Double
    ) {
        if (helper?.isPublicTransportMode.toNotNull()) {
            val currentRoute = transportHelper.currentRoute
            if (publicTransportRouteGeometry?.hasMapRenderer().toNotNull()) {
                renderState.updateTransportRouteState(currentRoute)
                if (renderState.shouldRebuildTransportRoute) {
                    publicTransportRouteGeometry?.resetSymbolProviders()
                }
            }
            val routes = transportHelper.routes
            val route =
                if (routes != null && routes.size > currentRoute) routes[currentRoute] else null
            routeGeometry?.clearRoute()
            val routeUpdated = publicTransportRouteGeometry?.updateRoute(tb, route)
            val draw = (routeUpdated.toNotNull() || renderState.shouldRebuildTransportRoute
                    || publicTransportRouteGeometry?.hasMapRenderer()?.not().toNotNull() || mapActivityInvalidated)
            if (route != null && draw) {
                val start = transportHelper.startLocation
                val startLocation = Location("transport")
                startLocation.latitude = start.latitude
                startLocation.longitude = start.longitude
                publicTransportRouteGeometry?.drawSegments(
                    tb, canvas, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
                    startLocation, 0
                )
            }
        } else {
            helper?.let {
                val route = helper.route
                val directTo = route.routeService == RouteService.DIRECT_TO
                val straight = route.routeService == RouteService.STRAIGHT
                publicTransportRouteGeometry?.clearRoute()
                val actualColoringType = if (isColoringAvailable(
                        routeColoringType,
                        routeInfoAttribute
                    )
                ) routeColoringType else ColoringType.DEFAULT
                val routeLineColor = getRouteLineColor()
                val routeLineWidth = routeLineWidth
                routeGeometry?.setRouteStyleParams(
                    routeLineColor, routeLineWidth,
                    directionArrowsColor, actualColoringType, routeInfoAttribute
                )
                val routeUpdated = routeGeometry?.updateRoute(tb, route).toNotNull()
                val shouldShowTurnArrows = shouldShowTurnArrows()
                if (routeUpdated) {
                    currentAnimatedRoute = 0
                }
                val lastProjection: Location?
                val startLocationIndex: Int
                if (directTo) {
                    lastProjection = null
                    startLocationIndex = 0
                } else if (route.currentStraightAngleRoute > 0) {
                    val lastFixedLocation = helper.lastFixedLocation ?: this.lastFixedLocation
                    val lastFixedLocationChanged =
                        !MapUtils.areLatLonEqual(this.lastFixedLocation, lastFixedLocation)
                    this.lastFixedLocation = lastFixedLocation
                    val currentLocation = Location(lastFixedLocation)
                    val mapRenderer = mapRenderer
                    val app = application
                    val useMapCenter = useMapCenter()
                    if (mapRenderer != null) {
                        if (useMapCenter) {
                            val target31 = mapRenderer.target
                            currentLocation.latitude = MapUtils.get31LatitudeY(target31.y)
                            currentLocation.longitude = MapUtils.get31LongitudeX(target31.x)
                        } else {
                            val lastMarkerLocation =
                                app.osmandMap!!.mapLayers.locationLayer.lastMarkerLocation
                            if (lastMarkerLocation != null) {
                                currentLocation.latitude = lastMarkerLocation.latitude
                                currentLocation.longitude = lastMarkerLocation.longitude
                            }
                        }
                    } else if (useMapCenter) {
                        currentLocation.latitude = tb.latitude
                        currentLocation.longitude = tb.longitude
                    }
                    val locations = route.immutableAllLocations
                    val posTolerance =
                        RoutingHelper.getPosTolerance(if (currentLocation.hasAccuracy()) currentLocation.accuracy else 0f)
                    var currentAnimatedRoute = helper.calculateCurrentRoute(
                        currentLocation, posTolerance.toDouble(),
                        locations, currentAnimatedRoute, false
                    )
                    // calculate projection of current location
                    lastProjection = if (currentAnimatedRoute > 0) RoutingHelperUtils.getProject(
                        currentLocation,
                        locations[currentAnimatedRoute - 1], locations[currentAnimatedRoute]
                    ) else null
                    startLocationIndex = currentAnimatedRoute
                    if (lastFixedLocationChanged) {
                        if (currentAnimatedRoute > route.currentRoute + 1) {
                            currentAnimatedRoute = route.currentRoute
                        }
                        this.currentAnimatedRoute = currentAnimatedRoute
                    }
                } else if (straight) {
                    lastProjection = helper.lastFixedLocation
                    startLocationIndex = route.currentStraightAngleRoute
                } else {
                    lastProjection = helper.lastProjection
                    startLocationIndex = route.currentStraightAngleRoute
                }
                lastRouteProjection = lastProjection
                var draw = true
                if (routeGeometry?.hasMapRenderer().toNotNull()) {
                    renderState.updateRouteState(
                        lastProjection, startLocationIndex, actualColoringType, routeLineColor,
                        routeLineWidth, route.currentRoute, tb.zoom, shouldShowTurnArrows
                    )
                    draw = routeUpdated || renderState.shouldRebuildRoute || mapActivityInvalidated
                    if (draw) {
                        routeGeometry?.resetSymbolProviders()
                    } else {
                        draw = renderState.shouldUpdateRoute
                    }
                }
                if (draw) {
                    routeGeometry?.drawSegments(
                        tb, canvas, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
                        lastProjection, startLocationIndex
                    )
                }
                val rd = helper.routeDirections
                val iterator: Iterator<RouteDirectionInfo> = rd.iterator()
                if (!directTo && tb.zoom >= 14 && shouldShowTurnArrows) {
                    if (routeGeometry?.hasMapRenderer().toNotNull()) {
                        if (routeUpdated || renderState.shouldUpdateActionPoints || mapActivityInvalidated) {
                            val actionPoints = calculateActionPoints(
                                helper.lastProjection,
                                route.routeLocations, route.currentRoute, iterator, tb.zoom
                            )
                            routeGeometry?.buildActionArrows(actionPoints, customTurnArrowColor)
                        }
                    } else {
                        val actionPoints = calculateActionPoints(
                            topLatitude, leftLongitude,
                            bottomLatitude, rightLongitude, helper.lastProjection,
                            route.routeLocations, route.currentRoute, iterator, tb.zoom
                        )
                        drawActionArrows(tb, canvas, actionPoints)
                    }
                } else {
                    if (routeGeometry?.hasMapRenderer().toNotNull()) {
                        routeGeometry?.resetActionLines()
                    }
                }
                if (directTo) {
                    //add projection point on original route
                    val projectionOnRoute = calculateProjectionOnRoutePoint(helper, tb)
                    projectionOnRoute?.let { drawProjectionPoint(canvas, it) }
                }

            }
        }
    }

    private fun calculateProjectionOnRoutePoint(
        helper: RoutingHelper?,
        box: RotatedTileBox
    ): DoubleArray? {
        helper?.let {
            var projectionXY: DoubleArray? = null
            val ll = helper.lastFixedLocation
            val route = helper.route
            val locs = route.immutableAllLocations
            val cr = route.currentRoute
            var locIndex = locs.size - 1
            if (route.intermediatePointsToPass > 0) {
                locIndex = route.getIndexOfIntermediate(route.intermediatePointsToPass - 1)
            }
            if (ll != null && cr > 0 && cr < locs.size && locIndex >= 0 && locIndex < locs.size) {
                val loc1 = locs[cr - 1]
                val loc2 = locs[cr]
                val distLeft =
                    (route.getDistanceFromPoint(cr) - route.getDistanceFromPoint(locIndex)).toDouble()
                val baDist =
                    (route.getDistanceFromPoint(cr - 1) - route.getDistanceFromPoint(cr)).toDouble()
                val target = locs[locIndex]
                val dTarget = ll.distanceTo(target).toDouble()
                val aX = box.getPixXFromLonNoRot(loc1.longitude)
                val aY = box.getPixYFromLatNoRot(loc1.latitude)
                val bX = box.getPixXFromLonNoRot(loc2.longitude)
                val bY = box.getPixYFromLatNoRot(loc2.latitude)
                if (baDist != 0.0) {
                    val cf = (dTarget - distLeft) / baDist
                    val rX = bX - cf * (bX - aX)
                    val rY = bY - cf * (bY - aY)
                    projectionXY = doubleArrayOf(rX, rY)
                }
            }
            if (projectionXY != null && ll != null) {
                val distanceLoc2Proj = MapUtils.getSqrtDistance(
                    projectionXY[0].toInt(), projectionXY[1].toInt(),
                    box.getPixXFromLonNoRot(ll.longitude), box.getPixYFromLatNoRot(ll.latitude)
                )
                val visible =
                    (box.containsPoint(projectionXY[0].toFloat(), projectionXY[1].toFloat(), 20.0f)
                            && distanceLoc2Proj > AndroidUtils.dpToPx(context, 52f) / 2.0)
                if (visible) {
                    return projectionXY
                }
            }
            return null
        } ?: return null
    }

    private fun calculateActionPoints(
        lastProjection: Location?, routeNodes: List<Location>, cd: Int,
        it: Iterator<RouteDirectionInfo>, zoom: Int
    ): List<Location?> {
        return calculateActionPoints(0.0, 0.0, 0.0, 0.0, lastProjection, routeNodes, cd, it, zoom)
    }

    private fun calculateActionPoints(
        topLatitude: Double, leftLongitude: Double, bottomLatitude: Double,
        rightLongitude: Double, lastProjection: Location?, routeNodes: List<Location>, cd: Int,
        it: Iterator<RouteDirectionInfo>, zoom: Int
    ): List<Location?> {
        var nf: RouteDirectionInfo? = null
        var distanceAction = 35.0
        if (zoom >= 17) {
            distanceAction = 15.0
        } else if (zoom == 15) {
            distanceAction = 70.0
        } else if (zoom < 15) {
            distanceAction = 110.0
        }
        var actionDist = 0.0
        var previousAction: Location? = null
        val actionPoints: MutableList<Location?> = ArrayList()
        var prevFinishPoint = -1
        for (routePoint in routeNodes.indices) {
            val loc = routeNodes[routePoint]
            if (nf != null) {
                val pnt =
                    if (nf.routeEndPointOffset == 0) nf.routePointOffset else nf.routeEndPointOffset
                if (pnt < routePoint + cd) {
                    nf = null
                }
            }
            while (nf == null && it.hasNext()) {
                nf = it.next()
                val pnt =
                    if (nf.routeEndPointOffset == 0) nf.routePointOffset else nf.routeEndPointOffset
                if (pnt < routePoint + cd) {
                    nf = null
                }
            }
            val action = nf != null && (nf.routePointOffset == routePoint + cd ||
                    nf.routePointOffset <= routePoint + cd && routePoint + cd <= nf.routeEndPointOffset)
            if (!action && previousAction == null) {
                // no need to check
                continue
            }
            val visible = leftLongitude == rightLongitude ||
                    loc.longitude in leftLongitude..rightLongitude && bottomLatitude <= loc.latitude && loc.latitude <= topLatitude
            if (action && !visible && previousAction == null) {
                continue
            }
            if (!action) {
                // previousAction != null
                val dist = loc.distanceTo(previousAction)
                actionDist += dist.toDouble()
                if (actionDist >= distanceAction) {
                    actionPoints.add(
                        calculateProjection(
                            1 - (actionDist - distanceAction) / dist,
                            previousAction,
                            loc
                        )
                    )
                    actionPoints.add(null)
                    prevFinishPoint = routePoint
                    previousAction = null
                    actionDist = 0.0
                } else {
                    actionPoints.add(loc)
                    previousAction = loc
                }
            } else {
                // action point
                if (previousAction == null) {
                    addPreviousToActionPoints(
                        actionPoints, lastProjection, routeNodes, distanceAction,
                        prevFinishPoint, routePoint, loc
                    )
                }
                actionPoints.add(loc)
                previousAction = loc
                prevFinishPoint = -1
                actionDist = 0.0
            }
        }
        if (previousAction != null) {
            actionPoints.add(null)
        }
        return actionPoints
    }

    private fun addPreviousToActionPoints(
        actionPoints: MutableList<Location?>,
        lastProjection: Location?,
        routeNodes: List<Location>,
        DISTANCE_ACTION: Double,
        prevFinishPoint: Int,
        routePoint: Int,
        loc: Location
    ) {
        // put some points in front
        val ind = actionPoints.size
        var lprevious: Location? = loc
        var dist = 0.0
        for (k in routePoint - 1 downTo -1) {
            val l = if (k == -1) lastProjection else routeNodes[k]
            val locDist = lprevious?.distanceTo(l) ?: 0f
            dist += locDist.toDouble()
            lprevious = if (dist >= DISTANCE_ACTION) {
                if (locDist > 1 && l != null) {
                    actionPoints.add(
                        ind,
                        calculateProjection(1 - (dist - DISTANCE_ACTION) / locDist, lprevious, l)
                    )
                }
                break
            } else {
                actionPoints.add(ind, l)
                l
            }
            if (prevFinishPoint == k) {
                if (ind >= 2) {
                    actionPoints.removeAt(ind - 2)
                    actionPoints.removeAt(ind - 2)
                }
                break
            }
        }
    }

    private fun calculateProjection(part: Double, lp: Location?, l: Location): Location {
        val p = Location(l)
        val lat = lp?.latitude ?: 0.0
        val lon = lp?.longitude ?: 0.0
        p.latitude = lat + part * (l.latitude - lat)
        p.longitude = lon + part * (l.longitude - lon)
        return p
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) {}
    override fun drawInScreenPixels(): Boolean {
        return false
    }

    override fun onLongPressEvent(point: PointF, tileBox: RotatedTileBox): Boolean {
        return false
    }

    override fun onSingleTap(point: PointF, tileBox: RotatedTileBox): Boolean {
        return false
    }

    private fun getRadiusPoi(tb: RotatedTileBox): Int {
        val zoom = tb.zoom.toDouble()
        val r: Int = if (zoom <= 15) {
            8
        } else if (zoom <= 16) {
            10
        } else if (zoom <= 17) {
            14
        } else {
            18
        }
        return (r * tb.density).toInt()
    }

    private val routeTransportStops: List<TransportStop?>?
        get() = if (helper?.isPublicTransportMode.toNotNull()) publicTransportRouteGeometry?.drawer?.routeTransportStops else null

    private fun getFromPoint(
        tb: RotatedTileBox,
        point: PointF,
        res: MutableList<in TransportStop>,
        routeTransportStops: List<TransportStop?>
    ) {
        val ex = point.x.toInt()
        val ey = point.y.toInt()
        val rp = getRadiusPoi(tb)
        var radius = rp * 3 / 2
        try {
            for (i in routeTransportStops.indices) {
                val n = routeTransportStops[i]
                if (n?.location == null) {
                    continue
                }
                val pixel = NativeUtilities.getPixelFromLatLon(
                    mapRenderer, tb,
                    n.location.latitude, n.location.longitude
                )
                if (abs(pixel.x - ex) <= radius && abs(pixel.y - ey) <= radius) {
                    radius = rp
                    res.add(n)
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            // ignore
        }
    }

    private fun isColoringAvailable(
        routeColoringType: ColoringType,
        routeInfoAttribute: String?
    ): Boolean {
        return coloringAvailabilityCache
            .isColoringAvailable(helper!!.route, routeColoringType, routeInfoAttribute)
    }

    override fun collectObjectsFromPoint(
        point: PointF,
        tileBox: RotatedTileBox,
        res: MutableList<Any>,
        unknownLocation: Boolean
    ) {
        val routeTransportStops = routeTransportStops
        if (!Algorithms.isEmpty(routeTransportStops)) {
            getFromPoint(tileBox, point, res, routeTransportStops!!)
        }
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        return if (o is TransportStop) {
            o.location
        } else null
    }

    override fun getObjectName(o: Any?): PointDescription? {
        return if (o is TransportStop) {
            PointDescription(
                PointDescription.POINT_TYPE_TRANSPORT_STOP,
                context.getString(R.string.transport_Stop),
                o.name
            )
        } else null
    }

    override fun disableSingleTap(): Boolean {
        return isPreviewRouteLineVisible
    }

    override fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean {
        return isPreviewRouteLineVisible
    }

    override fun isObjectClickable(o: Any): Boolean {
        return false
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        return false
    }

    /** OpenGL  */
    private fun resetLayer() {
        clearXAxisPoints()
        if (routeGeometry?.hasMapRenderer().toNotNull()) {
            routeGeometry?.resetSymbolProviders()
        }
        if (publicTransportRouteGeometry?.hasMapRenderer().toNotNull()) {
            publicTransportRouteGeometry?.resetSymbolProviders()
        }
        lastRouteProjection = null
    }

    private class RenderState {
        private var lastProjection: Location? = null
        private var startLocationIndex = -1
        private var publicTransportRoute = -1
        private var coloringType = ColoringType.DEFAULT
        private var routeColor = -1
        private var routeWidth = -1f
        private var currentRoute = -1
        private var zoom = -1
        private var shouldShowTurnArrows = false
        var shouldRebuildRoute = false
        var shouldRebuildTransportRoute = false
        var shouldUpdateRoute = false
        var shouldUpdateActionPoints = false
        fun updateRouteState(
            lastProjection: Location?,
            startLocationIndex: Int,
            coloringType: ColoringType,
            routeColor: Int,
            routeWidth: Float,
            currentRoute: Int,
            zoom: Int,
            shouldShowTurnArrows: Boolean
        ) {
            shouldRebuildRoute =
                this.coloringType != coloringType || this.routeColor != routeColor || this.routeWidth != routeWidth
            shouldUpdateRoute =
                ((!MapUtils.areLatLonEqualPrecise(this.lastProjection, lastProjection)
                        || this.startLocationIndex != startLocationIndex)
                        && this.coloringType == coloringType && this.routeColor == routeColor && this.routeWidth == routeWidth)
            shouldUpdateActionPoints = (shouldRebuildRoute
                    || shouldUpdateRoute
                    || this.shouldShowTurnArrows != shouldShowTurnArrows || this.currentRoute != currentRoute || this.zoom != zoom)
            this.lastProjection = lastProjection
            this.startLocationIndex = startLocationIndex
            this.coloringType = coloringType
            this.routeColor = routeColor
            this.routeWidth = routeWidth
            this.currentRoute = currentRoute
            this.zoom = zoom
            this.shouldShowTurnArrows = shouldShowTurnArrows
        }

        fun updateTransportRouteState(publicTransportRoute: Int) {
            shouldRebuildTransportRoute = this.publicTransportRoute != publicTransportRoute
            this.publicTransportRoute = publicTransportRoute
        }
    }
}