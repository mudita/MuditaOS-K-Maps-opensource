package net.osmand.plus.views.layers

import kotlin.math.ceil as ceil1
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.Pair
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.mudita.maps.R
import java.io.File
import kotlin.math.abs
import net.osmand.GPXUtilities.GPXFile
import net.osmand.GPXUtilities.TrkSegment
import net.osmand.GPXUtilities.WptPt
import net.osmand.GPXUtilities.calculateBounds
import net.osmand.GPXUtilities.calculateTrackBounds
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.GpxAdditionalIconsProvider
import net.osmand.core.jni.GpxAdditionalIconsProvider.SplitLabel
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.QListPointI
import net.osmand.core.jni.SplitLabelList
import net.osmand.core.jni.TextRasterizer
import net.osmand.core.jni.Utilities
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox
import net.osmand.plus.ChartPointsHelper
import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.ConfigureMapMenu
import net.osmand.plus.helpers.GpxUiHelper
import net.osmand.plus.mapcontextmenu.other.SelectedGpxPoint
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints
import net.osmand.plus.mapmarkers.MapMarker
import net.osmand.plus.mapmarkers.MapMarkersHelper
import net.osmand.plus.render.OsmandDashPathEffect
import net.osmand.plus.render.OsmandRenderer
import net.osmand.plus.routing.ColoringType
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.track.CachedTrack
import net.osmand.plus.track.GradientScaleType
import net.osmand.plus.track.SaveGpxAsyncTask
import net.osmand.plus.track.TrackDrawInfo
import net.osmand.plus.track.helpers.GPXDatabase
import net.osmand.plus.track.helpers.GpxDbHelper
import net.osmand.plus.track.helpers.GpxDisplayItem
import net.osmand.plus.track.helpers.GpxSelectionHelper
import net.osmand.plus.track.helpers.NetworkRouteSelectionTask
import net.osmand.plus.track.helpers.SelectedGpxFile
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BooleanUtils.toNotNull
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.LetUtils.safeLet
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.PointImageDrawable
import net.osmand.plus.views.Renderable.CurrentTrack
import net.osmand.plus.views.Renderable.RenderableSegment
import net.osmand.plus.views.Renderable.StandardTrack
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.core.LocationPointsTileProvider
import net.osmand.plus.views.layers.core.WptPtTileProvider
import net.osmand.plus.views.layers.geometry.GpxGeometryWay
import net.osmand.plus.views.layers.geometry.GpxGeometryWayContext
import net.osmand.render.RenderingRuleSearchRequest
import net.osmand.render.RenderingRulesStorage
import net.osmand.router.network.NetworkRouteSelector.RouteKey
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

class GPXLayer(ctx: Context) : OsmandMapLayer(ctx), IContextMenuProvider, IMoveObjectProvider,
    MapTextProvider<WptPt?> {
    private var paint: Paint? = null
    private var borderPaint: Paint? = null
    private var shadowPaint: Paint? = null
    private var cachedHash = 0

    @ColorInt
    private var cachedColor = 0
    private var defaultTrackWidth = 0f
    private val cachedTrackWidth: MutableMap<String, Float?> = HashMap()
    private var startPointIcon: Drawable? = null
    private var finishPointIcon: Drawable? = null
    private var startAndFinishIcon: Drawable? = null
    private var startPointImage: Bitmap? = null
    private var finishPointImage: Bitmap? = null
    private var startAndFinishImage: Bitmap? = null
    private var highlightedPointImage: Bitmap? = null
    private var trackDrawInfo: TrackDrawInfo? = null

    private var gpxTextScale = 1f
    private var nightMode = false
    private var chartPointsHelper: ChartPointsHelper? = null
    private var trackChartPoints: TrackChartPoints? = null
    private var xAxisPointsCached: List<LatLon?> = ArrayList()
    private var app: OsmandApplication? = null
    private var gpxDbHelper: GpxDbHelper? = null
    private var mapMarkersHelper: MapMarkersHelper? = null
    private var selectedGpxHelper: GpxSelectionHelper? = null
    private var visibleGPXFilesMap: Map<SelectedGpxFile, Long> = HashMap()
    private val segmentsCache: MutableMap<String, CachedTrack> = HashMap()
    private val renderedSegmentsCache: MutableMap<String, MutableSet<TrkSegment>> = HashMap()
    private var tmpVisibleTrack: SelectedGpxFile? = null
    private val pointsCache: MutableList<WptPt?> = ArrayList()
    private var pointFileMap: Map<WptPt, SelectedGpxFile> = HashMap()
    private var textLayer: MapTextLayer? = null
    private var paintOuterRect: Paint? = null
    private var paintInnerRect: Paint? = null
    private var paintTextIcon: Paint? = null
    private var wayContext: GpxGeometryWayContext? = null
    private var osmandRenderer: OsmandRenderer? = null

    //OpenGl
    private var additionalIconsProvider: GpxAdditionalIconsProvider? = null
    private var startFinishPointsCountCached = 0
    private var splitLabelsCountCached = 0
    private var pointCountCached = 0
    private var hiddenGroupsCountCached = 0
    private var textVisibleCached = false
    private var pointsTileProvider: WptPtTileProvider? = null
    private var trackChartPointsProvider: LocationPointsTileProvider? = null
    private var highlightedPointCollection: MapMarkersCollection? = null
    private var highlightedPointMarker: net.osmand.core.jni.MapMarker? = null
    private var highlightedPointLocationCached: LatLon? = null
    private var trackMarkersChangedTime: Long = 0
    private var contextMenuLayer: ContextMenuLayer? = null
    private var networkRouteSelectionTask: NetworkRouteSelectionTask? = null

    @ColorInt
    private var visitedColor = 0

    @ColorInt
    private var defPointColor = 0

    @ColorInt
    private var grayColor = 0

    @ColorInt
    private var disabledColor = 0
    private var defaultTrackColorPref: CommonPreference<String>? = null
    private var defaultTrackWidthPref: CommonPreference<String>? = null
    private var currentTrackColorPref: CommonPreference<Int>? = null
    private var currentTrackColoringTypePref: CommonPreference<ColoringType>? = null
    private var currentTrackRouteInfoAttributePref: CommonPreference<String>? = null
    private var currentTrackWidthPref: CommonPreference<String>? = null
    private var currentTrackShowArrowsPref: CommonPreference<Boolean>? = null
    private var currentTrackShowStartFinishPref: OsmandPreference<Boolean>? = null
    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        app = view.application
        gpxDbHelper = app?.gpxDbHelper
        selectedGpxHelper = app?.selectedGpxHelper
        mapMarkersHelper = app?.mapMarkersHelper
        osmandRenderer = app?.resourceManager?.renderer?.renderer
        chartPointsHelper = ChartPointsHelper(context)
        currentTrackColorPref = view.settings?.CURRENT_TRACK_COLOR
        currentTrackColoringTypePref = view.settings?.CURRENT_TRACK_COLORING_TYPE
        currentTrackRouteInfoAttributePref = view.settings?.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE
        currentTrackWidthPref = view.settings?.CURRENT_TRACK_WIDTH
        currentTrackShowArrowsPref = view.settings?.CURRENT_TRACK_SHOW_ARROWS
        currentTrackShowStartFinishPref = view.settings?.CURRENT_TRACK_SHOW_START_FINISH
        defaultTrackColorPref =
            view.settings?.getCustomRenderProperty(ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR)
                ?.cache()
        defaultTrackWidthPref =
            view.settings?.getCustomRenderProperty(ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR)
                ?.cache()
        initUI()
    }

    fun setTrackChartPoints(trackChartPoints: TrackChartPoints?) {
        this.trackChartPoints = trackChartPoints
    }

    val isInTrackAppearanceMode: Boolean
        get() = trackDrawInfo != null

    fun setTrackDrawInfo(trackDrawInfo: TrackDrawInfo?) {
        this.trackDrawInfo = trackDrawInfo
    }

    private fun initUI() {
        paint = Paint()
        safeLet(view, app) { view, app ->

            paint?.style = Paint.Style.STROKE
            paint?.isAntiAlias = true
            borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            borderPaint?.style = Paint.Style.STROKE
            borderPaint?.strokeJoin = Paint.Join.ROUND
            borderPaint?.strokeCap = Paint.Cap.ROUND
            borderPaint?.color = -0x80000000
            shadowPaint = Paint()
            shadowPaint?.style = Paint.Style.STROKE
            shadowPaint?.isAntiAlias = true
            paintTextIcon = Paint()
            paintTextIcon?.textSize = 10 * (view.density ?: 1f)
            paintTextIcon?.textAlign = Paint.Align.CENTER
            paintTextIcon?.isFakeBoldText = true
            paintTextIcon?.isAntiAlias = true
            textLayer = view.getLayerByClass(MapTextLayer::class.java)
            paintInnerRect = Paint()
            paintInnerRect?.style = Paint.Style.FILL
            paintInnerRect?.isAntiAlias = true
            paintOuterRect = Paint()
            paintOuterRect?.style = Paint.Style.STROKE
            paintOuterRect?.isAntiAlias = true
            paintOuterRect?.strokeWidth = 3f
            paintOuterRect?.alpha = 255
            val iconsCache = app.uIUtilities
            startPointIcon = iconsCache.getIcon(R.drawable.map_track_point_start)
            finishPointIcon = iconsCache.getIcon(R.drawable.map_track_point_finish)
            startAndFinishIcon = iconsCache.getIcon(R.drawable.map_track_point_start_finish)
            contextMenuLayer = view.getLayerByClass(ContextMenuLayer::class.java)
            visitedColor = ContextCompat.getColor(app, R.color.color_ok)
            defPointColor = ContextCompat.getColor(app, R.color.gpx_color_point)
            grayColor = ContextCompat.getColor(app, R.color.color_favorite_gray)
            disabledColor = ContextCompat.getColor(app, R.color.gpx_disabled_color)
            wayContext = GpxGeometryWayContext(context, view.density ?: 1f)
        }
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) {
        drawMovableWpt(canvas, tileBox)
    }

    private fun drawMovableWpt(canvas: Canvas, tileBox: RotatedTileBox) {
        val movableObject = contextMenuLayer?.moveableObject
        if (movableObject is WptPt) {
            val gpxFile = pointFileMap[movableObject]
            if (gpxFile != null) {
                val pf = contextMenuLayer?.getMovableCenterPoint(tileBox)
                val mapMarker = mapMarkersHelper?.getMapMarker(movableObject)
                val textScale = textScale
                val fileColor = getFileColor(gpxFile)
                val pointColor = getPointColor(movableObject, fileColor)
                canvas.save()
                canvas.rotate(
                    -tileBox.rotate,
                    tileBox.centerPixelX.toFloat(),
                    tileBox.centerPixelY.toFloat()
                )
                pf?.let {
                    drawBigPoint(canvas, movableObject, pointColor, pf.x, pf.y, mapMarker, textScale)
                }
                canvas.restore()
            }
        }
    }

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings?
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        val visibleGPXFiles: MutableList<SelectedGpxFile> = ArrayList(
            selectedGpxHelper!!.selectedGPXFiles
        )
        val tmpVisibleTrackChanged = updateTmpVisibleTrack(visibleGPXFiles)
        pointsCache.clear()
        removeCachedUnselectedTracks(visibleGPXFiles)
        val visibleGPXFilesMap: MutableMap<SelectedGpxFile, Long> = HashMap()
        var pointsModified = false
        for (selectedGpxFile in visibleGPXFiles) {
            val pointsModifiedTime = this.visibleGPXFilesMap[selectedGpxFile]
            val newPointsModifiedTime = selectedGpxFile.pointsModifiedTime
            if (pointsModifiedTime == null || pointsModifiedTime != newPointsModifiedTime) {
                pointsModified = true
            }
            visibleGPXFilesMap[selectedGpxFile] = newPointsModifiedTime
        }
        this.visibleGPXFilesMap = visibleGPXFilesMap
        val nightMode = settings != null && settings.isNightMode
        val nightModeChanged = this.nightMode != nightMode
        this.nightMode = nightMode
        val trackMarkersChangedTime = mapMarkersHelper?.trackMarkersModifiedTime
        val trackMarkersChanged = this.trackMarkersChangedTime != trackMarkersChangedTime
        this.trackMarkersChangedTime = trackMarkersChangedTime ?: 0L
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            val forceUpdate =
                updateBitmaps() || nightModeChanged || pointsModified || tmpVisibleTrackChanged
            if (visibleGPXFiles.isNotEmpty()) {
                settings?.let { drawSelectedFilesSegments(canvas, tileBox, visibleGPXFiles, it) }
            }
            drawXAxisPointsOpenGl(trackChartPoints, mapRenderer, tileBox)
            drawSelectedFilesSplitsOpenGl(mapRenderer, tileBox, visibleGPXFiles, forceUpdate)
            drawSelectedFilesPointsOpenGl(
                mapRenderer,
                tileBox,
                visibleGPXFiles,
                forceUpdate || trackMarkersChanged
            )
        } else {
            if (visibleGPXFiles.isNotEmpty()) {
                settings?.let { drawSelectedFilesSegments(canvas, tileBox, visibleGPXFiles, it) }
                canvas.rotate(
                    -tileBox.rotate,
                    tileBox.centerPixelX.toFloat(),
                    tileBox.centerPixelY.toFloat()
                )
                drawXAxisPoints(trackChartPoints, canvas, tileBox)
                drawDirectionArrows(canvas, tileBox, visibleGPXFiles)
                drawSelectedFilesSplits(canvas, tileBox, visibleGPXFiles)
                drawSelectedFilesPoints(canvas, tileBox, visibleGPXFiles)
                drawSelectedFilesStartEndPoints(canvas, tileBox, visibleGPXFiles)
            }
            if (textLayer != null && isTextVisible()) {
                textLayer?.putData(this, pointsCache)
            }
        }
        mapActivityInvalidated = false
    }

    private fun updateTmpVisibleTrack(visibleGPXFiles: MutableList<SelectedGpxFile>): Boolean {
        var tmpVisibleTrackChanged = false
        val selectedGpxFile = getTmpVisibleTrack(visibleGPXFiles)
        if (selectedGpxFile != null) {
            visibleGPXFiles.add(selectedGpxFile)
            tmpVisibleTrackChanged = tmpVisibleTrack !== selectedGpxFile
            tmpVisibleTrack = selectedGpxFile
            if (tmpVisibleTrackChanged) {
                val cachedTrack = segmentsCache.remove(selectedGpxFile.gpxFile.path)
                if (hasMapRenderer() && cachedTrack != null) {
                    resetSymbolProviders(selectedGpxFile.pointsToDisplay)
                    resetSymbolProviders(cachedTrack.allCachedTrackSegments)
                }
            }
        }
        return tmpVisibleTrackChanged
    }

    private fun getTmpVisibleTrack(selectedGpxFiles: List<SelectedGpxFile>): SelectedGpxFile? {
        return null
    }

    private fun updatePaints(
        color: Int,
        width: String,
        routePoints: Boolean,
        currentTrack: Boolean,
        drawSettings: DrawSettings?,
        tileBox: RotatedTileBox
    ): Boolean {
        val rrs = app?.rendererRegistry?.currentSelectedRenderer
        val nightMode = drawSettings != null && drawSettings.isNightMode
        var hashChanged = false

        safeLet(defaultTrackColorPref, defaultTrackWidthPref) { defaultTrackColorPref, defaultTrackWidthPref ->
            val hash: Int = if (!hasMapRenderer()) {
                calculateHash(
                    rrs, cachedTrackWidth, routePoints, nightMode, tileBox.mapDensity, tileBox.zoom,
                    defaultTrackColorPref.get(), defaultTrackWidthPref.get()
                )
            } else {
                calculateHash(
                    rrs, cachedTrackWidth, routePoints, nightMode, tileBox.mapDensity,
                    defaultTrackColorPref.get(), defaultTrackWidthPref.get()
                )
            }
            hashChanged = hash != cachedHash
            if (hashChanged) {
                cachedHash = hash
                cachedColor = ContextCompat.getColor(app!!, R.color.gpx_track)
                defaultTrackWidth = DEFAULT_WIDTH_MULTIPLIER * (view.density ?: 1f)
                if (rrs != null) {
                    val req = RenderingRuleSearchRequest(rrs)
                    req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode)
                    if (defaultTrackColorPref.isSet) {
                        val ctColor = rrs.PROPS[ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR]
                        if (ctColor != null) {
                            req.setStringFilter(ctColor, defaultTrackColorPref.get())
                        }
                    }
                    if (defaultTrackWidthPref.isSet) {
                        val ctWidth = rrs.PROPS[ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR]
                        if (ctWidth != null) {
                            req.setStringFilter(ctWidth, defaultTrackWidthPref.get())
                        }
                    }
                    var additional = ""
                    if (routePoints) {
                        additional = "routePoints=true"
                    }
                    if (currentTrack) {
                        additional = (if (additional.isEmpty()) {
                            ""
                        } else {
                            ";"
                        }) + "currentTrack=true"
                    }
                    req.setIntFilter(rrs.PROPS.R_MINZOOM, tileBox.zoom)
                    req.setIntFilter(rrs.PROPS.R_MAXZOOM, tileBox.zoom)
                    if (additional.isNotEmpty()) {
                        req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional)
                    }
                    if (req.searchRenderingAttribute("gpx")) {
                        val rc = OsmandRenderer.RenderingContext(context)
                        rc.setDensityValue(tileBox.mapDensity.toFloat())
                        cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR)
                        defaultTrackWidth = rc.getComplexValue(req, req.ALL.R_STROKE_WIDTH)
                        osmandRenderer?.updatePaint(req, paint, 0, false, rc)
                        if (req.isSpecified(rrs.PROPS.R_SHADOW_RADIUS)) {
                            val shadowColor = req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR)
                            val shadowRadius = rc.getComplexValue(req, rrs.PROPS.R_SHADOW_RADIUS)
                            shadowPaint?.colorFilter =
                                PorterDuffColorFilter(shadowColor, PorterDuff.Mode.SRC_IN)
                            shadowPaint?.strokeWidth = (paint?.strokeWidth ?: 0f) + 2 * shadowRadius
                        }
                        for (key in cachedTrackWidth.keys) {
                            acquireTrackWidth(key, rrs, req, rc)
                        }
                    } else {
                        log.error("Rendering attribute gpx is not found !")
                        for (key in cachedTrackWidth.keys) {
                            cachedTrackWidth[key] = defaultTrackWidth
                        }
                    }
                }
            }
        }
        paint?.color = if (color == 0) cachedColor else color
        paint?.strokeWidth = getTrackWidth(width, defaultTrackWidth)
        borderPaint?.strokeWidth = (paint?.strokeWidth ?: 0f) + AndroidUtils.dpToPx(
            context, 2f
        )
        return hashChanged
    }

    private fun acquireTrackWidth(
        widthKey: String,
        rrs: RenderingRulesStorage,
        req: RenderingRuleSearchRequest,
        rc: OsmandRenderer.RenderingContext
    ) {
        if (!Algorithms.isEmpty(widthKey) && Algorithms.isInt(widthKey)) {
            try {
                val widthDp = widthKey.toInt()
                val widthF = AndroidUtils.dpToPx(app!!, widthDp.toFloat()).toFloat()
                cachedTrackWidth[widthKey] = widthF
            } catch (e: NumberFormatException) {
                log.error(e.message, e)
                cachedTrackWidth[widthKey] = defaultTrackWidth
            }
        } else {
            val ctWidth = rrs.PROPS[ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR]
            if (ctWidth != null) {
                req.setStringFilter(ctWidth, widthKey)
            }
            if (req.searchRenderingAttribute("gpx")) {
                val widthF = rc.getComplexValue(req, req.ALL.R_STROKE_WIDTH)
                cachedTrackWidth[widthKey] = widthF
            }
        }
    }

    private fun calculateHash(vararg o: Any?): Int {
        return o.contentHashCode()
    }

    private fun drawSelectedFilesSplits(
        canvas: Canvas, tileBox: RotatedTileBox,
        selectedGPXFiles: List<SelectedGpxFile>
    ) {
        if (tileBox.zoom >= START_ZOOM) {
            // request to load
            for (selectedGpxFile in selectedGPXFiles) {
                val groups = selectedGpxFile.getDisplayGroups(app)
                if (!Algorithms.isEmpty(groups)) {
                    val color = getTrackColor(selectedGpxFile.gpxFile, cachedColor)
                    paintInnerRect?.color = color
                    paintInnerRect?.alpha = 179
                    val contrastColor = ColorUtilities.getContrastColor(app, color, false)
                    paintTextIcon?.color = contrastColor
                    paintOuterRect?.color = contrastColor
                    groups[0]?.modifiableList?.let {
                        drawSplitItems(canvas, tileBox, it)
                    }
                }
            }
        }
    }

    private fun drawSelectedFilesSplitsOpenGl(
        mapRenderer: MapRendererView, tileBox: RotatedTileBox,
        selectedGPXFiles: List<SelectedGpxFile>, forceUpdate: Boolean
    ) {
        if (tileBox.zoom >= START_ZOOM) {
            var changed = forceUpdate
            var startFinishPointsCount = 0
            var splitLabelsCount = 0
            for (selectedGpxFile in selectedGPXFiles) {
                if (isShowStartFinishForTrack(selectedGpxFile.gpxFile)) {
                    val segments = selectedGpxFile.pointsToDisplay
                    for (segment in segments) {
                        if (segment.points.size >= 2) {
                            startFinishPointsCount += 2
                        }
                    }
                }
                val groups = selectedGpxFile.getDisplayGroups(app)
                if (!Algorithms.isEmpty(groups)) {
                    val items = groups[0].modifiableList
                    for (item in items) {
                        if (item.splitName != null) {
                            splitLabelsCount++
                        }
                    }
                }
            }
            changed = changed or (startFinishPointsCount != startFinishPointsCountCached)
            changed = changed or (splitLabelsCount != splitLabelsCountCached)
            if (!changed && !mapActivityInvalidated) {
                return
            }
            startFinishPointsCountCached = startFinishPointsCount
            splitLabelsCountCached = splitLabelsCount
            clearSelectedFilesSplits()
            val startFinishPoints = QListPointI()
            val splitLabels = SplitLabelList()
            for (selectedGpxFile in selectedGPXFiles) {
                if (isShowStartFinishForTrack(selectedGpxFile.gpxFile)) {
                    val segments = selectedGpxFile.pointsToDisplay
                    for (segment in segments) {
                        if (segment.points.size >= 2) {
                            val start = segment.points[0]
                            val finish = segment.points[segment.points.size - 1]
                            startFinishPoints.add(
                                PointI(
                                    Utilities.get31TileNumberX(start.lon),
                                    Utilities.get31TileNumberY(start.lat)
                                )
                            )
                            startFinishPoints.add(
                                PointI(
                                    Utilities.get31TileNumberX(finish.lon),
                                    Utilities.get31TileNumberY(finish.lat)
                                )
                            )
                        }
                    }
                }
                val groups = selectedGpxFile.getDisplayGroups(app)
                if (!Algorithms.isEmpty(groups)) {
                    val color = getTrackColor(selectedGpxFile.gpxFile, cachedColor)
                    val items = groups[0]!!.modifiableList
                    for (item in items) {
                        val point = item.locationEnd
                        var name = item.splitName
                        if (name != null) {
                            val ind = name.indexOf(' ')
                            if (ind > 0) {
                                name = name.substring(0, ind)
                            }
                            val point31 = PointI(
                                Utilities.get31TileNumberX(point.lon),
                                Utilities.get31TileNumberY(point.lat)
                            )
                            splitLabels.add(
                                SplitLabel(
                                    point31,
                                    name,
                                    NativeUtilities.createColorARGB(color, 179)
                                )
                            )
                        }
                    }
                }
            }
            if (!startFinishPoints.isEmpty || !splitLabels.isEmpty) {
                additionalIconsProvider = GpxAdditionalIconsProvider(
                    getPointsOrder() - selectedGPXFiles.size - 101, tileBox.density
                        .toDouble(),
                    startFinishPoints, splitLabels,
                    NativeUtilities.createSkImageFromBitmap(startPointImage!!),
                    NativeUtilities.createSkImageFromBitmap(finishPointImage!!),
                    NativeUtilities.createSkImageFromBitmap(startAndFinishImage!!)
                )
                mapRenderer.addSymbolsProvider(additionalIconsProvider)
            }
        } else {
            startFinishPointsCountCached = 0
            splitLabelsCountCached = 0
            clearSelectedFilesSplits()
        }
    }

    private fun updateBitmaps(): Boolean {
        if (hasMapRenderer()) {
            val textScale = textScale
            if (this.gpxTextScale != textScale || startPointImage == null) {
                this.gpxTextScale = textScale
                recreateBitmaps()
                return true
            }
        }
        return false
    }

    private fun recreateBitmaps() {
        if (hasMapRenderer()) {
            startPointImage = getScaledBitmap(R.drawable.map_track_point_start)
            finishPointImage = getScaledBitmap(R.drawable.map_track_point_finish)
            startAndFinishImage = getScaledBitmap(R.drawable.map_track_point_start_finish)
            highlightedPointImage = chartPointsHelper!!.createHighlightedPointBitmap()
            recreateHighlightedPointCollection()
        }
    }

    private val textStyle: TextRasterizer.Style
        get() = MapTextLayer.getTextStyle(context, nightMode, gpxTextScale, view.density!!)

    private fun clearSelectedFilesSplits() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && additionalIconsProvider != null) {
            mapRenderer.removeSymbolsProvider(additionalIconsProvider)
            additionalIconsProvider = null
        }
    }

    override fun getScaledBitmap(drawableId: Int): Bitmap? {
        return getScaledBitmap(drawableId, gpxTextScale)
    }

    private fun drawSplitItems(
        canvas: Canvas, tileBox: RotatedTileBox,
        items: List<GpxDisplayItem>
    ) {
        val latLonBounds = tileBox.latLonBounds
        val r = (12 * tileBox.density).toInt()
        paintTextIcon!!.textSize = r.toFloat()
        val dr = r * 3 / 2
        var px = -1f
        var py = -1f
        for (k in items.indices) {
            val i = items[k]
            val point = i.locationEnd
            if (point != null && point.lat >= latLonBounds.bottom && point.lat <= latLonBounds.top && point.lon >= latLonBounds.left && point.lon <= latLonBounds.right) {
                val x = tileBox.getPixXFromLatLon(point.lat, point.lon)
                val y = tileBox.getPixYFromLatLon(point.lat, point.lon)
                if (px != -1f || py != -1f) {
                    if (abs(x - px) <= dr && abs(y - py) <= dr) {
                        continue
                    }
                }
                px = x
                py = y
                var name = i.splitName
                if (name != null) {
                    val ind = name.indexOf(' ')
                    if (ind > 0) {
                        name = name.substring(0, ind)
                    }
                    val bounds = Rect()
                    paintTextIcon!!.getTextBounds(name, 0, name.length, bounds)
                    val nameHalfWidth = bounds.width() / 2f
                    val nameHalfHeight = bounds.height() / 2f
                    val density = ceil1(x = tileBox.density.toDouble()).toFloat()
                    val rect = RectF(
                        x - nameHalfWidth - 2 * density,
                        y + nameHalfHeight + 3 * density,
                        x + nameHalfWidth + 3 * density,
                        y - nameHalfHeight - 2 * density
                    )
                    paintInnerRect?.let {
                        canvas.drawRoundRect(rect, 0f, 0f, it)
                    }
                    paintOuterRect?.let {
                        canvas.drawRoundRect(rect, 0f, 0f, it)
                    }
                    paintTextIcon?.let {
                        canvas.drawText(name, x, y + nameHalfHeight, it)
                    }
                }
            }
        }
    }

    private fun drawDirectionArrows(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        selectedGPXFiles: List<SelectedGpxFile>
    ) {
        if (!tileBox.isZoomAnimated) {
            val correctedQuadRect = getCorrectedQuadRect(tileBox.latLonBounds)
            for (selectedGpxFile in selectedGPXFiles) {
                val showArrows = isShowArrowsForTrack(selectedGpxFile.gpxFile)
                val coloringTypeName = getAvailableOrDefaultColoringType(selectedGpxFile)
                val coloringType = ColoringType.getNonNullTrackColoringTypeByName(coloringTypeName)
                if (!showArrows || coloringType.isRouteInfoAttribute
                    || !QuadRect.trivialOverlap(
                        correctedQuadRect,
                        calculateTrackBounds(selectedGpxFile.pointsToDisplay)
                    )
                ) {
                    continue
                }
                val width =
                    getTrackWidthName(selectedGpxFile.gpxFile, defaultTrackWidthPref?.get() ?: "")
                val trackWidth = getTrackWidth(width, defaultTrackWidth)
                val trackColor = getTrackColor(selectedGpxFile.gpxFile, cachedColor)
                val segments = if (coloringType.isGradient) getCachedSegments(
                    selectedGpxFile,
                    coloringType.toGradientScaleType()
                ) else selectedGpxFile.pointsToDisplay
                for (segment in segments) {
                    if (segment.renderer is RenderableSegment) {
                        (segment.renderer as RenderableSegment)
                            .drawGeometry(
                                canvas,
                                tileBox,
                                correctedQuadRect,
                                trackColor,
                                trackWidth,
                                null,
                                true
                            )
                    }
                }
            }
        }
    }

    private fun drawSelectedFilesStartEndPoints(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        selectedGPXFiles: List<SelectedGpxFile>
    ) {
        if (tileBox.zoom >= START_ZOOM) {
            for (selectedGpxFile in selectedGPXFiles) {
                if (isShowStartFinishForTrack(selectedGpxFile.gpxFile)) {
                    val segments = selectedGpxFile.pointsToDisplay
                    for (segment in segments) {
                        if (segment.points.size >= 2) {
                            val start = segment.points[0]
                            val end = segment.points[segment.points.size - 1]
                            drawStartEndPoints(
                                canvas,
                                tileBox,
                                start,
                                if (selectedGpxFile.isShowCurrentTrack) null else end
                            )
                        }
                    }
                }
            }
        }
    }

    private fun drawStartEndPoints(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        start: WptPt?,
        end: WptPt?
    ) {
        val startX =
            if (start != null) tileBox.getPixXFromLatLon(start.lat, start.lon).toInt() else 0
        val startY =
            if (start != null) tileBox.getPixYFromLatLon(start.lat, start.lon).toInt() else 0
        val endX = if (end != null) tileBox.getPixXFromLatLon(end.lat, end.lon).toInt() else 0
        val endY = if (end != null) tileBox.getPixYFromLatLon(end.lat, end.lon).toInt() else 0
        val iconSize = AndroidUtils.dpToPx(context, 14f)
        val startRectWithoutShadow = calculateRect(
            startX.toFloat(),
            startY.toFloat(),
            iconSize.toFloat(),
            iconSize.toFloat()
        )
        val endRectWithoutShadow =
            calculateRect(endX.toFloat(), endY.toFloat(), iconSize.toFloat(), iconSize.toFloat())
        if (start != null && end != null && QuadRect.intersects(
                startRectWithoutShadow,
                endRectWithoutShadow
            )
        ) {
            startAndFinishIcon?.let {
                val startAndFinishRect = calculateRect(
                    startX.toFloat(),
                    startY.toFloat(),
                    it.intrinsicWidth.toFloat(),
                    it.intrinsicHeight.toFloat()
                )
                drawPoint(canvas, startAndFinishRect, it)
            }
        } else {
            startPointIcon?.let {
                if (start != null) {
                    val startRect = calculateRect(
                        startX.toFloat(),
                        startY.toFloat(),
                        it.intrinsicWidth.toFloat(),
                        it.intrinsicHeight.toFloat()
                    )
                    drawPoint(canvas, startRect, it)
                }
            }
            finishPointIcon?.let {
                if (end != null) {
                    val endRect = calculateRect(
                        endX.toFloat(),
                        endY.toFloat(),
                        it.intrinsicWidth.toFloat(),
                        it.intrinsicHeight.toFloat()
                    )
                    drawPoint(canvas, endRect, it)
                }
            }
        }
    }

    private fun drawPoint(canvas: Canvas, rect: QuadRect, icon: Drawable) {
        icon.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
        icon.draw(canvas)
    }

    private fun drawSelectedFilesPoints(
        canvas: Canvas, tileBox: RotatedTileBox,
        selectedGPXFiles: List<SelectedGpxFile>
    ) {
        if (tileBox.zoom >= START_ZOOM) {
            val textScale = textScale
            val iconSize = getIconSize(app)
            val boundIntersections = initBoundIntersections(tileBox)
            val fullObjectsLatLon: MutableList<LatLon> = ArrayList()
            val smallObjectsLatLon: MutableList<LatLon> = ArrayList()
            val pointFileMap: MutableMap<WptPt, SelectedGpxFile> = HashMap()
            // request to load
            val latLonBounds = tileBox.latLonBounds
            for (g in selectedGPXFiles) {
                val fullObjects: MutableList<Pair<WptPt, MapMarker?>> = ArrayList()
                val fileColor = getFileColor(g)
                val synced = isSynced(g.gpxFile)
                val selected = GpxSelectionHelper.isGpxFileSelected(app!!, g.gpxFile)
                for (wpt in getSelectedFilePoints(g)) {
                    if (wpt.lat >= latLonBounds.bottom && wpt.lat <= latLonBounds.top && wpt.lon >= latLonBounds.left && wpt.lon <= latLonBounds.right && wpt !== contextMenuLayer!!.moveableObject && !isPointHidden(
                            g,
                            wpt
                        )
                    ) {
                        pointFileMap[wpt] = g
                        var marker: MapMarker? = null
                        if (synced) {
                            marker = mapMarkersHelper?.getMapMarker(wpt)
                            if (marker == null || marker.history && view.settings?.KEEP_PASSED_MARKERS_ON_MAP?.get()?.not().toNotNull()) {
                                continue
                            }
                        }
                        pointsCache.add(wpt)
                        val x = tileBox.getPixXFromLatLon(wpt.lat, wpt.lon)
                        val y = tileBox.getPixYFromLatLon(wpt.lat, wpt.lon)
                        if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
                            @ColorInt val color: Int = if (selected) {
                                if (marker != null && marker.history) {
                                    grayColor
                                } else {
                                    getPointColor(wpt, fileColor)
                                }
                            } else {
                                disabledColor
                            }
                            val pointImageDrawable = PointImageDrawable.getFromWpt(
                                context, color,
                                true, wpt
                            )
                            pointImageDrawable.drawSmallPoint(canvas, x, y, textScale)
                            smallObjectsLatLon.add(LatLon(wpt.lat, wpt.lon))
                        } else {
                            fullObjects.add(Pair(wpt, marker))
                            fullObjectsLatLon.add(LatLon(wpt.lat, wpt.lon))
                        }
                    }
                    if (wpt === contextMenuLayer?.moveableObject) {
                        pointFileMap[wpt] = g
                    }
                }
                for (pair in fullObjects) {
                    val wpt = pair.first
                    val x = tileBox.getPixXFromLatLon(wpt.lat, wpt.lon)
                    val y = tileBox.getPixYFromLatLon(wpt.lat, wpt.lon)
                    val pointColor = if (selected) getPointColor(wpt, fileColor) else disabledColor
                    drawBigPoint(canvas, wpt, pointColor, x, y, pair.second, textScale)
                }
            }
            trackChartPoints?.highlightedPoint?.let {
                val highlightedPoint = it
                chartPointsHelper?.drawHighlightedPoint(highlightedPoint, canvas, tileBox)
            }
            this.fullObjectsLatLon = fullObjectsLatLon
            this.smallObjectsLatLon = smallObjectsLatLon
            this.pointFileMap = pointFileMap
        }
    }

    private fun drawSelectedFilesPointsOpenGl(
        mapRenderer: MapRendererView, tileBox: RotatedTileBox,
        selectedGPXFiles: List<SelectedGpxFile>, forceUpdate: Boolean
    ) {
        if (tileBox.zoom >= START_ZOOM) {
            trackChartPoints?.highlightedPoint?.let {
                val highlightedPoint = it
                if (!Algorithms.objectEquals(highlightedPointLocationCached, highlightedPoint)) {
                    highlightedPointLocationCached = highlightedPoint
                    setHighlightedPointMarkerLocation(highlightedPoint)
                    setHighlightedPointMarkerVisibility(true)
                }
            } ?: run {
                setHighlightedPointMarkerVisibility(false)
            }
            var pointsCount = 0
            var hiddenGroupsCount = 0
            for (g in selectedGPXFiles) {
                pointsCount += getSelectedFilePointsSize(g)
                hiddenGroupsCount += g.hiddenGroupsCount
            }
            val textVisible = isTextVisible()
            if (!forceUpdate && pointCountCached == pointsCount && hiddenGroupsCountCached == hiddenGroupsCount && textVisible == textVisibleCached && !mapActivityInvalidated) {
                return
            }
            pointCountCached = pointsCount
            hiddenGroupsCountCached = hiddenGroupsCount
            textVisibleCached = textVisible
            clearPoints()
            pointsTileProvider = WptPtTileProvider(
                context, getPointsOrder() - 300,
                textVisible, textStyle, view.density ?: 1f
            )
            val textScale = textScale
            val pointFileMap: MutableMap<WptPt, SelectedGpxFile> = HashMap()
            for (g in selectedGPXFiles) {
                val fileColor = getFileColor(g)
                val synced = isSynced(g.gpxFile)
                val selected = GpxSelectionHelper.isGpxFileSelected(app!!, g.gpxFile)
                for (wpt in getSelectedFilePoints(g)) {
                    if (wpt !== contextMenuLayer?.moveableObject && !isPointHidden(g, wpt)) {
                        pointFileMap[wpt] = g
                        var marker: MapMarker? = null
                        if (synced) {
                            marker = mapMarkersHelper?.getMapMarker(wpt)
                            if (marker == null || marker.history && view.settings?.KEEP_PASSED_MARKERS_ON_MAP?.get()?.not().toNotNull()) {
                                continue
                            }
                        }
                        var history = false
                        var color: Int
                        if (selected) {
                            if (marker != null && marker.history) {
                                color = grayColor
                                history = true
                            } else {
                                color = getPointColor(wpt, fileColor)
                            }
                        } else {
                            color = disabledColor
                        }
                        pointsTileProvider?.addToData(
                            wpt,
                            color,
                            true,
                            marker != null,
                            history,
                            textScale
                        )
                    }
                    if (wpt === contextMenuLayer?.moveableObject) {
                        pointFileMap[wpt] = g
                    }
                }
            }
            this.pointFileMap = pointFileMap
            pointsTileProvider?.pointsCount?.takeIf { it > 0 }?.let {
                pointsTileProvider?.drawSymbols(mapRenderer)

            }
        } else {
            highlightedPointLocationCached = null
            setHighlightedPointMarkerVisibility(false)
            pointCountCached = 0
            hiddenGroupsCountCached = 0
            clearPoints()
        }
    }

    private fun setHighlightedPointMarkerLocation(latLon: LatLon) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && highlightedPointMarker != null) {
            highlightedPointMarker?.position = PointI(
                MapUtils.get31TileNumberX(latLon.longitude),
                MapUtils.get31TileNumberY(latLon.latitude)
            )
        }
    }

    private fun setHighlightedPointMarkerVisibility(visible: Boolean) {
        highlightedPointMarker?.setIsHidden(!visible)
    }

    private fun clearPoints() {
        val mapRenderer = mapRenderer
        if (mapRenderer == null || pointsTileProvider == null) {
            return
        }
        pointsTileProvider?.clearSymbols(mapRenderer)
        pointsTileProvider = null
    }

    private fun isSynced(gpxFile: GPXFile): Boolean {
        val markersGroup = mapMarkersHelper?.getMarkersGroup(gpxFile)
        return markersGroup != null && !markersGroup.isDisabled
    }

    private fun drawXAxisPoints(
        chartPoints: TrackChartPoints?, canvas: Canvas,
        tileBox: RotatedTileBox
    ) {
        if (chartPoints != null) {
            val xAxisPoints = chartPoints.xAxisPoints
            if (!Algorithms.isEmpty(xAxisPoints)) {
                var pointColor = trackChartPoints?.segmentColor
                if (pointColor == 0) {
                    pointColor = getTrackColor(trackChartPoints!!.gpx, cachedColor)
                    trackChartPoints?.segmentColor = pointColor
                }
                pointColor?.let {
                    chartPointsHelper?.drawXAxisPoints(xAxisPoints, pointColor, canvas, tileBox)
                }
            }
        }
    }

    private fun drawXAxisPointsOpenGl(
        chartPoints: TrackChartPoints?, mapRenderer: MapRendererView,
        tileBox: RotatedTileBox
    ) {
        if (chartPoints != null) {
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
                trackChartPoints?.let {
                    var pointColor = it.segmentColor
                    if (pointColor == 0) {
                        pointColor = getTrackColor(it.gpx, cachedColor)
                    }
                    chartPointsHelper?.createXAxisPointBitmap(pointColor, tileBox.density)?.let {
                        trackChartPointsProvider =
                            LocationPointsTileProvider(getPointsOrder() - 500, xAxisPoints, it)
                    }
                    trackChartPointsProvider?.drawPoints(mapRenderer)
                }
            }
        } else {
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
            clearHighlightedPointCollection()
            highlightedPointCollection = MapMarkersCollection()
            val builder = MapMarkerBuilder()
            builder.baseOrder = getPointsOrder() - 600
            builder.setIsAccuracyCircleSupported(false)
            builder.setIsHidden(true)
            highlightedPointImage?.let {
                builder.pinIcon = NativeUtilities.createSkImageFromBitmap(it)
            }
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

    private fun getFileColor(g: SelectedGpxFile): Int {
        return if (g.color == 0) defPointColor else g.color
    }

    private fun drawBigPoint(
        canvas: Canvas,
        wpt: WptPt,
        pointColor: Int,
        x: Float,
        y: Float,
        marker: MapMarker?,
        textScale: Float
    ) {
        val pointImageDrawable: PointImageDrawable
        var history = false
        if (marker != null) {
            pointImageDrawable = PointImageDrawable.getOrCreateSyncedIcon(context, pointColor, wpt)
            history = marker.history
        } else {
            pointImageDrawable = PointImageDrawable.getFromWpt(context, pointColor, true, wpt)
        }
        pointImageDrawable.drawPoint(canvas, x, y, textScale, history)
    }

    @ColorInt
    private fun getPointColor(o: WptPt, @ColorInt fileColor: Int): Int {
        val visit = isPointVisited(o)
        return if (visit) visitedColor else o.getColor(fileColor)
    }

    private fun drawSelectedFilesSegments(
        canvas: Canvas, tileBox: RotatedTileBox,
        selectedGPXFiles: List<SelectedGpxFile>, settings: DrawSettings
    ) {
        var currentTrack: SelectedGpxFile? = null
        var baseOrder = baseOrder
        for (selectedGpxFile in selectedGPXFiles) {
            val width = getTrackWidthName(selectedGpxFile.gpxFile, defaultTrackWidthPref!!.get())
            if (!cachedTrackWidth.containsKey(width)) {
                cachedTrackWidth[width] = null
            }
            if (selectedGpxFile.isShowCurrentTrack) {
                currentTrack = selectedGpxFile
            } else {
                drawSelectedFileSegments(
                    selectedGpxFile,
                    false,
                    canvas,
                    tileBox,
                    settings,
                    baseOrder--
                )
            }
            val gpxPath = selectedGpxFile.gpxFile.path
            if (!renderedSegmentsCache.containsKey(gpxPath)) {
                renderedSegmentsCache.remove(gpxPath)
            }
        }
        if (currentTrack != null) {
            drawSelectedFileSegments(currentTrack, true, canvas, tileBox, settings, baseOrder)
        }
    }

    private fun drawSelectedFileSegments(
        selectedGpxFile: SelectedGpxFile, currentTrack: Boolean,
        canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings,
        baseOrder: Int
    ) {
        var mutableBaseOrder = baseOrder
        val hasMapRenderer = hasMapRenderer()
        val gpxFile = selectedGpxFile.gpxFileToDisplay
        val gpxFilePath = selectedGpxFile.gpxFile.path
        val correctedQuadRect = getCorrectedQuadRect(tileBox.latLonBounds)
        val coloringTypeName = getAvailableOrDefaultColoringType(selectedGpxFile)
        val coloringType = ColoringType.getNonNullTrackColoringTypeByName(coloringTypeName)
        val routeIndoAttribute = ColoringType.getRouteInfoAttribute(coloringTypeName)
        val visible = hasMapRenderer || QuadRect.trivialOverlap(
            tileBox.latLonBounds,
            calculateTrackBounds(selectedGpxFile.pointsToDisplay)
        )
        if (!gpxFile.hasTrkPt() && coloringType.isGradient || !visible) {
            segmentsCache.remove(gpxFilePath)
            return
        }
        val segments: MutableList<TrkSegment> = ArrayList()
        if (coloringType.isTrackSolid || coloringType.isRouteInfoAttribute) {
            segments.addAll(selectedGpxFile.pointsToDisplay)
        } else {
            segments.addAll(getCachedSegments(selectedGpxFile, coloringType.toGradientScaleType()))
        }
        var renderedSegments = renderedSegmentsCache[gpxFilePath]
        if (renderedSegments != null) {
            val it = renderedSegments.iterator()
            while (it.hasNext()) {
                val renderedSegment = it.next()
                if (!segments.contains(renderedSegment)) {
                    resetSymbolProviders(renderedSegment)
                    it.remove()
                }
            }
        } else {
            renderedSegments = HashSet()
            renderedSegmentsCache[gpxFilePath] = renderedSegments
        }
        val width = getTrackWidthName(gpxFile, defaultTrackWidthPref!!.get())
        for (segmentIdx in segments.indices) {
            val ts = segments[segmentIdx]
            val color = getTrackColor(gpxFile, ts.getColor(cachedColor))
            var newTsRenderer = false
            if (ts.renderer == null && ts.points.isNotEmpty()) {
                val renderer: RenderableSegment = if (currentTrack) {
                    CurrentTrack(ts.points)
                } else {
                    StandardTrack(ts.points, 17.2)
                }
                renderer.setBorderPaint(borderPaint)
                ts.renderer = renderer
                val geometryWay = GpxGeometryWay(wayContext)
                geometryWay.baseOrder = mutableBaseOrder--
                renderer.geometryWay = geometryWay
                newTsRenderer = true
            }
            var updated = (updatePaints(
                color,
                width,
                selectedGpxFile.isRoutePoints,
                currentTrack,
                settings,
                tileBox
            )
                    || mapActivityInvalidated || newTsRenderer || !renderedSegments.contains(ts))
            if (ts.renderer is RenderableSegment) {
                val renderableSegment = ts.renderer as RenderableSegment
                updated = updated or renderableSegment.setTrackParams(
                    color,
                    width,
                    coloringType,
                    routeIndoAttribute
                )
                if (hasMapRenderer || coloringType.isRouteInfoAttribute) {
                    updated = updated or renderableSegment.setRoute(
                        getCachedTrack(selectedGpxFile).getCachedRouteSegments(segmentIdx)
                    )
                    updated = updated or renderableSegment.setDrawArrows(
                        isShowArrowsForTrack(selectedGpxFile.gpxFile)
                    )
                    if (updated || !hasMapRenderer) {
                        var intervals: FloatArray? = null
                        val pathEffect = paint?.pathEffect
                        if (pathEffect is OsmandDashPathEffect) {
                            intervals = pathEffect.intervals
                        }
                        paint?.let {
                            renderableSegment.drawGeometry(
                                canvas, tileBox, correctedQuadRect,
                                it.color, it.strokeWidth, intervals
                            )
                        }
                        renderedSegments.add(ts)
                    }
                } else {
                    renderableSegment.drawSegment(view.zoom.toDouble(), paint, canvas, tileBox)
                }
            }
        }
    }

    private fun getCachedSegments(
        selectedGpxFile: SelectedGpxFile,
        scaleType: GradientScaleType?
    ): List<TrkSegment> {
        val cachedTrack = getCachedTrack(selectedGpxFile)
        return cachedTrack.getCachedTrackSegments(view.zoom, scaleType ?: GradientScaleType.SPEED)
    }

    private fun getTrackWidth(width: String, defaultTrackWidth: Float): Float {
        val trackWidth = cachedTrackWidth[width]
        return trackWidth ?: defaultTrackWidth
    }

    private fun getTrackColor(gpxFile: GPXFile, defaultColor: Int): Int {
        var color = 0
        if (!GpxSelectionHelper.isGpxFileSelected(app!!, gpxFile)) {
            color = ColorUtilities.getColorWithAlpha(disabledColor, 0.5f)
        } else if (hasTrackDrawInfoForTrack(gpxFile)) {
            color = trackDrawInfo?.color ?: 0
        } else if (gpxFile.showCurrentTrack) {
            color = currentTrackColorPref?.get() ?: 0
        } else {
            val dataItem = gpxDbHelper?.getItem(File(gpxFile.path))
            if (dataItem != null) {
                color = dataItem.color
            }
        }
        return if (color != 0) color else gpxFile.getColor(defaultColor)
    }

    private fun getAvailableOrDefaultColoringType(selectedGpxFile: SelectedGpxFile): String? {
        val gpxFile = selectedGpxFile.gpxFileToDisplay
        app?.let {
            if (!GpxSelectionHelper.isGpxFileSelected(it, gpxFile)) {
                return ColoringType.TRACK_SOLID.getName(null)
            }
        }
        if (hasTrackDrawInfoForTrack(gpxFile)) {
            return trackDrawInfo?.coloringType?.getName(trackDrawInfo?.routeInfoAttribute)
        }
        var dataItem: GPXDatabase.GpxDataItem? = null
        val defaultColoringType = ColoringType.TRACK_SOLID.getName(null)
        var coloringType: ColoringType? = null
        var routeInfoAttribute: String? = null
        val isCurrentTrack = gpxFile.showCurrentTrack
        if (isCurrentTrack) {
            coloringType = currentTrackColoringTypePref?.get()
            routeInfoAttribute = currentTrackRouteInfoAttributePref?.get()
        } else {
            dataItem = gpxDbHelper?.getItem(File(gpxFile.path))
            if (dataItem != null) {
                coloringType = ColoringType.getNonNullTrackColoringTypeByName(dataItem.coloringType)
                routeInfoAttribute = ColoringType.getRouteInfoAttribute(dataItem.coloringType)
            }
        }
        return if (coloringType == null) {
            defaultColoringType
        } else if (!coloringType.isAvailableInSubscription(app!!, routeInfoAttribute, false)) {
            defaultColoringType
        } else if (getCachedTrack(selectedGpxFile).isColoringTypeAvailable(
                coloringType,
                routeInfoAttribute
            )
        ) {
            coloringType.getName(routeInfoAttribute)
        } else {
            if (!isCurrentTrack && dataItem != null) {
                gpxDbHelper?.updateColoringType(dataItem, defaultColoringType)
            }
            defaultColoringType
        }
    }

    private fun getTrackWidthName(gpxFile: GPXFile, defaultWidth: String): String {
        var width: String? = null
        if (hasTrackDrawInfoForTrack(gpxFile)) {
            width = trackDrawInfo?.width
        } else if (gpxFile.showCurrentTrack) {
            width = currentTrackWidthPref?.get()
        } else {
            val dataItem = gpxDbHelper?.getItem(File(gpxFile.path))
            if (dataItem != null) {
                width = dataItem.width
            }
        }
        return width ?: gpxFile.getWidth(defaultWidth)
    }

    private fun isShowArrowsForTrack(gpxFile: GPXFile): Boolean {
        return if (!GpxSelectionHelper.isGpxFileSelected(app ?: return false, gpxFile)) {
            false
        } else if (hasTrackDrawInfoForTrack(gpxFile)) {
            trackDrawInfo?.isShowArrows.toNotNull()
        } else if (gpxFile.showCurrentTrack) {
            currentTrackShowArrowsPref?.get().toNotNull()
        } else {
            val dataItem = gpxDbHelper?.getItem(File(gpxFile.path))
            dataItem?.isShowArrows ?: gpxFile.isShowArrows
        }
    }

    private fun isShowStartFinishForTrack(gpxFile: GPXFile): Boolean {
        return if (!GpxSelectionHelper.isGpxFileSelected(app ?: return false, gpxFile)) {
            false
        } else if (hasTrackDrawInfoForTrack(gpxFile)) {
            trackDrawInfo?.isShowStartFinish.toNotNull()
        } else if (gpxFile.showCurrentTrack) {
            currentTrackShowStartFinishPref?.get().toNotNull()
        } else {
            val dataItem = gpxDbHelper?.getItem(File(gpxFile.path))
            dataItem?.isShowStartFinish ?: gpxFile.isShowStartFinish
        }
    }

    private fun hasTrackDrawInfoForTrack(gpxFile: GPXFile): Boolean {
        return trackDrawInfo != null && (trackDrawInfo?.isCurrentRecording.toNotNull() && gpxFile.showCurrentTrack
                || gpxFile.path == trackDrawInfo?.filePath)
    }

    private fun getCachedTrack(selectedGpxFile: SelectedGpxFile): CachedTrack {
        val path = selectedGpxFile.gpxFile.path
        var cachedTrack = segmentsCache[path]
        if (cachedTrack == null) {
            cachedTrack = CachedTrack(app!!, selectedGpxFile)
            segmentsCache[path] = cachedTrack
        }
        return cachedTrack
    }

    private fun isPointVisited(o: WptPt): Boolean {
        var visit = false
        val visited = o.extensionsToRead["VISITED_KEY"]
        if (visited != null && visited != "0") {
            visit = true
        }
        return visit
    }

    private fun getSelectedFilePoints(g: SelectedGpxFile): List<WptPt> {
        return g.gpxFile.points
    }

    private fun getSelectedFilePointsSize(g: SelectedGpxFile): Int {
        return g.gpxFile.pointsSize
    }

    private fun isPointHidden(selectedGpxFile: SelectedGpxFile, point: WptPt): Boolean {
        return selectedGpxFile.isGroupHidden(point.category)
    }

    private fun calculateBelongs(ex: Int, ey: Int, objx: Int, objy: Int, radius: Int): Boolean {
        return abs(objx - ex) <= radius && abs(objy - ey) <= radius
    }

    private fun getWptFromPoint(tb: RotatedTileBox, point: PointF, res: MutableList<in WptPt>) {
        val r = (getScaledTouchRadius(app!!, tb.defaultRadiusPoi) * TOUCH_RADIUS_MULTIPLIER).toInt()
        val ex = point.x.toInt()
        val ey = point.y.toInt()
        val visibleGpxFiles: List<SelectedGpxFile> = ArrayList(
            selectedGpxHelper?.selectedGPXFiles
        )
        for (g in visibleGpxFiles) {
            val pts = getSelectedFilePoints(g)
            // int fcolor = g.getColor() == 0 ? clr : g.getColor();
            for (n in pts) {
                if (isPointHidden(g, n)) {
                    continue
                }
                val pixel = NativeUtilities.getPixelFromLatLon(mapRenderer, tb, n.lat, n.lon)
                if (calculateBelongs(ex, ey, pixel.x.toInt(), pixel.y.toInt(), r)) {
                    res.add(n)
                }
            }
        }
    }

    fun getTracksFromPoint(
        tb: RotatedTileBox,
        point: PointF,
        res: MutableList<Any>,
        showTrackPointMenu: Boolean
    ) {
        val r = getScaledTouchRadius(app!!, tb.defaultRadiusPoi)
        val mx = point.x.toInt()
        val my = point.y.toInt()
        val visibleGpxFiles: List<SelectedGpxFile> = ArrayList(
            selectedGpxHelper?.selectedGPXFiles
        )
        for (selectedGpxFile in visibleGpxFiles) {
            val points = findPointsNearSegments(selectedGpxFile.pointsToDisplay, tb, r, mx, my)
            points?.let {
                val latLon = NativeUtilities.getLatLonFromPixel(mapRenderer, tb, mx, my)
                if (latLon != null) {
                    res.add(
                        createSelectedGpxPoint(
                            selectedGpxFile, it.first, it.second, latLon,
                            showTrackPointMenu
                        )
                    )
                }
            }
        }
    }

    private fun findPointsNearSegments(
        segments: List<TrkSegment>, tileBox: RotatedTileBox,
        radius: Int, x: Int, y: Int
    ): Pair<WptPt, WptPt>? {
        for (segment in segments) {
            val trackBounds = calculateBounds(segment.points)
            if (QuadRect.trivialOverlap(tileBox.latLonBounds, trackBounds)) {
                val points =
                    findPointsNearSegment(mapRenderer, tileBox, segment.points, radius, x, y)
                if (points != null) {
                    return points
                }
            }
        }
        return null
    }

    private fun createSelectedGpxPoint(
        selectedGpxFile: SelectedGpxFile, prevPoint: WptPt,
        nextPoint: WptPt, latLon: LatLon, showTrackPointMenu: Boolean
    ): SelectedGpxPoint {
        val projectionPoint = createProjectionPoint(prevPoint, nextPoint, latLon)
        val prevPointLocation = Location("")
        prevPointLocation.latitude = prevPoint?.lat ?: 0.0
        prevPointLocation.longitude = prevPoint?.lon ?: 0.0
        val nextPointLocation = Location("")
        nextPointLocation.latitude = nextPoint?.lat ?: 0.0
        nextPointLocation.longitude = nextPoint?.lon ?: 0.0
        val bearing = prevPointLocation.bearingTo(nextPointLocation)
        return SelectedGpxPoint(
            selectedGpxFile, projectionPoint, prevPoint, nextPoint, bearing,
            showTrackPointMenu
        )
    }

    override fun getObjectName(o: Any?): PointDescription? {
        if (o is WptPt) {
            return PointDescription(PointDescription.POINT_TYPE_WPT, o.name)
        } else if (o is SelectedGpxPoint) {
            val selectedGpxFile = o.selectedGpxFile
            val gpxFile = selectedGpxFile.gpxFile
            val name: String = if (selectedGpxFile.isShowCurrentTrack) {
                context.getString(R.string.shared_string_currently_recording_track)
            } else if (!Algorithms.isEmpty(gpxFile.articleTitle)) {
                gpxFile.articleTitle
            } else {
                GpxUiHelper.getGpxTitle(Algorithms.getFileWithoutDirs(gpxFile.path))
            }
            return PointDescription(PointDescription.POINT_TYPE_GPX, name)
        } else if (o is Pair<*, *>) {
            if (o.first is RouteKey && o.second is QuadRect) {
                val routeKey = o.first as RouteKey
                return PointDescription(PointDescription.POINT_TYPE_ROUTE, routeKey.routeName)
            }
        }
        return null
    }

    private fun removeCachedUnselectedTracks(selectedGpxFiles: List<SelectedGpxFile>) {
        val selectedTracksPaths: MutableList<String> = ArrayList()
        for (gpx in selectedGpxFiles) {
            selectedTracksPaths.add(gpx.gpxFile.path)
        }
        val unselectedGpxFiles: MutableList<SelectedGpxFile> = ArrayList()
        for (gpx in visibleGPXFilesMap.keys) {
            if (!selectedTracksPaths.contains(gpx.gpxFile.path)) {
                unselectedGpxFiles.add(gpx)
            }
        }
        for (gpx in unselectedGpxFiles) {
            resetSymbolProviders(gpx.pointsToDisplay)
        }
        val cachedTracksPaths = segmentsCache.keys
        val iterator = cachedTracksPaths.iterator()
        while (iterator.hasNext()) {
            val cachedTrackPath = iterator.next()
            val trackHidden = !selectedTracksPaths.contains(cachedTrackPath)
            if (trackHidden) {
                if (hasMapRenderer()) {
                    val cachedTrack = segmentsCache[cachedTrackPath]
                    if (cachedTrack != null) {
                        resetSymbolProviders(cachedTrack.allCachedTrackSegments)
                    }
                }
                iterator.remove()
            }
        }
    }

    private fun resetSymbolProviders(segment: TrkSegment) {
        if (segment.renderer is RenderableSegment) {
            val renderableSegment = segment.renderer as RenderableSegment
            val geometryWay = renderableSegment.geometryWay
            if (geometryWay != null && geometryWay.hasMapRenderer()) {
                geometryWay.resetSymbolProviders()
            }
        }
    }

    private fun resetSymbolProviders(segments: List<TrkSegment>) {
        for (segment in segments) {
            resetSymbolProviders(segment)
        }
    }

    override fun disableSingleTap(): Boolean {
        return isInTrackAppearanceMode
    }

    override fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean {
        if (isInTrackAppearanceMode) {
            return true
        }
        if (tileBox.zoom >= START_ZOOM) {
            val res: MutableList<Any> = ArrayList()
            getTracksFromPoint(tileBox, point, res, false)
            return !Algorithms.isEmpty(res)
        }
        return false
    }

    override fun isObjectClickable(o: Any): Boolean {
        return o is WptPt || o is SelectedGpxFile
    }

    override fun runExclusiveAction(param: Any?, unknownLocation: Boolean): Boolean {
        return false
    }

    override fun collectObjectsFromPoint(
        point: PointF, tileBox: RotatedTileBox, res: MutableList<Any>,
        unknownLocation: Boolean
    ) {
        if (tileBox.zoom >= START_ZOOM) {
            getWptFromPoint(tileBox, point, res)
            getTracksFromPoint(tileBox, point, res, false)
        }
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        if (o is WptPt) {
            return LatLon(o.lat, o.lon)
        } else if (o is SelectedGpxPoint) {
            val point = o.selectedPoint
            return LatLon(point.lat, point.lon)
        } else if (o is Pair<*, *>) {
            if (o.first is RouteKey && o.second is QuadRect) {
                val rect = o.second as QuadRect
                return LatLon(rect.centerY(), rect.centerX())
            }
        }
        return null
    }

    override fun drawInScreenPixels(): Boolean {
        return false
    }

    override fun onLongPressEvent(point: PointF, tileBox: RotatedTileBox): Boolean {
        if (tileBox.zoom >= START_ZOOM) {
            val trackPoints: MutableList<Any> = ArrayList()
            getTracksFromPoint(tileBox, point, trackPoints, true)
            if (!Algorithms.isEmpty(trackPoints)) {
                val latLon =
                    NativeUtilities.getLatLonFromPixel(mapRenderer, tileBox, point.x, point.y)
                if (trackPoints.size == 1) {
                    val gpxPoint = trackPoints[0] as SelectedGpxPoint
                    contextMenuLayer?.showContextMenu(
                        latLon
                    )
                }
                return true
            }
        }
        return false
    }

    override fun isTextVisible(): Boolean {
        return view.settings?.SHOW_POI_LABEL?.get().toNotNull()
    }

    override fun isFakeBoldText(): Boolean {
        return false
    }

    override fun isObjectMovable(o: Any?): Boolean {
        return o is WptPt
    }

    override fun applyNewObjectPosition(
        o: Any?,
        position: LatLon,
        callback: ApplyMovedObjectCallback?
    ) {
        if (o is WptPt) {
            val selectedGpxFile = pointFileMap[o]
            if (selectedGpxFile != null) {
                val gpxFile = selectedGpxFile.gpxFile
                gpxFile.updateWptPt(
                    o, position.latitude, position.longitude,
                    o.desc, o.name, o.category,
                    o.color, o.iconName, o.backgroundType
                )
                syncGpx(gpxFile)
                if (gpxFile.showCurrentTrack) {
                    callback?.onApplyMovedObject(true, o)
                } else {
                    SaveGpxAsyncTask(
                        File(gpxFile.path),
                        gpxFile,
                        object : SaveGpxAsyncTask.SaveGpxListener {
                            override fun gpxSavingStarted() {}
                            override fun gpxSavingFinished(errorMessage: Exception?) {
                                callback?.onApplyMovedObject(errorMessage == null, o)
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            }
        } else callback?.onApplyMovedObject(false, o)
    }

    private fun syncGpx(gpxFile: GPXFile) {
        val group = app!!.mapMarkersHelper!!.getMarkersGroup(gpxFile)
        if (group != null) {
            mapMarkersHelper!!.runSynchronization(group)
        }
    }

    companion object {
        private val log = PlatformUtil.getLog(GPXLayer::class.java)
        private const val TOUCH_RADIUS_MULTIPLIER = 1.5
        private const val DEFAULT_WIDTH_MULTIPLIER = 7
        private const val START_ZOOM = 7

        @JvmStatic
        fun findPointsNearSegment(
            mapRenderer: MapRendererView?,
            tb: RotatedTileBox, points: List<WptPt?>,
            r: Int, mx: Int, my: Int
        ): Pair<WptPt, WptPt>? {
            if (Algorithms.isEmpty(points)) {
                return null
            }
            var prevPoint = points[0]
            val pixelPrev =
                NativeUtilities.getPixelFromLatLon(mapRenderer, tb, prevPoint!!.lat, prevPoint.lon)
            var ppx = pixelPrev.x.toInt()
            var ppy = pixelPrev.y.toInt()
            var pcross = placeInBbox(ppx, ppy, mx, my, r, r)
            for (i in 1 until points.size) {
                val point = points[i]
                val pixel =
                    NativeUtilities.getPixelFromLatLon(mapRenderer, tb, point!!.lat, point.lon)
                val px = pixel.x.toInt()
                val py = pixel.y.toInt()
                val cross = placeInBbox(px, py, mx, my, r, r)
                if (cross == 0) {
                    return Pair(prevPoint, point)
                }
                if (pcross and cross == 0) {
                    var mpx = px
                    var mpy = py
                    var mcross = cross
                    while (abs(mpx - ppx) > r || abs(mpy - ppy) > r) {
                        val mpxnew = mpx / 2 + ppx / 2
                        val mpynew = mpy / 2 + ppy / 2
                        val mcrossnew = placeInBbox(mpxnew, mpynew, mx, my, r, r)
                        if (mcrossnew == 0) {
                            return Pair(prevPoint, point)
                        }
                        if (mcrossnew and mcross != 0) {
                            mpx = mpxnew
                            mpy = mpynew
                            mcross = mcrossnew
                        } else if (mcrossnew and pcross != 0) {
                            ppx = mpxnew
                            ppy = mpynew
                            pcross = mcrossnew
                        } else {
                            // this should never happen theoretically
                            break
                        }
                    }
                }
                pcross = cross
                ppx = px
                ppy = py
                prevPoint = point
            }
            return null
        }

        @JvmStatic
        fun createProjectionPoint(prevPoint: WptPt, nextPoint: WptPt, latLon: LatLon): WptPt {
            val projection = MapUtils.getProjection(
                latLon.latitude,
                latLon.longitude,
                prevPoint.lat,
                prevPoint.lon,
                nextPoint.lat,
                nextPoint.lon
            )
            val projectionPoint = WptPt()
            projectionPoint.lat = projection.latitude
            projectionPoint.lon = projection.longitude
            projectionPoint.heading = prevPoint.heading
            projectionPoint.distance =
                prevPoint.distance + MapUtils.getDistance(projection, prevPoint.lat, prevPoint.lon)
            projectionPoint.ele = getValueByDistInterpolation(
                projectionPoint.distance,
                prevPoint.distance,
                prevPoint.ele,
                nextPoint.distance,
                nextPoint.ele
            )
            projectionPoint.speed = getValueByDistInterpolation(
                projectionPoint.distance,
                prevPoint.distance,
                prevPoint.speed,
                nextPoint.distance,
                nextPoint.speed
            )
            if (prevPoint.time != 0L && nextPoint.time != 0L) {
                projectionPoint.time = getValueByDistInterpolation(
                    projectionPoint.distance,
                    prevPoint.distance,
                    prevPoint.time.toDouble(),
                    nextPoint.distance,
                    nextPoint.time.toDouble()
                ).toLong()
            }
            return projectionPoint
        }

        private fun getValueByDistInterpolation(
            projectionDist: Double,
            prevDist: Double,
            prevVal: Double,
            nextDist: Double,
            nextVal: Double
        ): Double {
            return prevVal + (projectionDist - prevDist) * ((nextVal - prevVal) / (nextDist - prevDist))
        }

        private fun placeInBbox(x: Int, y: Int, mx: Int, my: Int, halfw: Int, halfh: Int): Int {
            var cross = 0
            cross = cross or if (x < mx - halfw) 1 else 0
            cross = cross or if (x > mx + halfw) 2 else 0
            cross = cross or if (y < my - halfh) 4 else 0
            cross = cross or if (y > my + halfh) 8 else 0
            return cross
        }
    }

    override fun getTextLocation(o: WptPt?): LatLon? {
        return o?.let { LatLon(it.lat, it.lon) }
    }

    override fun getTextShift(o: WptPt?, rb: RotatedTileBox): Int {
        return (16 * rb.density * textScale).toInt()
    }

    override fun getText(o: WptPt?): String? {
        return o?.name
    }
}