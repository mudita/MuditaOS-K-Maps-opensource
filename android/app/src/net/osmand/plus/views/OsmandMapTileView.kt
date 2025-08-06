package net.osmand.plus.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.mudita.map.common.utils.MAX_ZOOM_LEVEL
import com.mudita.map.common.utils.MIN_ZOOM_LEVEL
import kotlin.math.abs
import kotlin.math.roundToInt
import net.osmand.PlatformUtil
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.PointI
import net.osmand.data.LatLon
import net.osmand.data.RotatedTileBox
import net.osmand.map.IMapLocationListener
import net.osmand.map.MapTileDownloader
import net.osmand.plus.AppInitializer
import net.osmand.plus.AppInitializer.AppInitializeListener
import net.osmand.plus.OsmAndConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BooleanUtils.toNotNull
import net.osmand.plus.utils.LetUtils.safeLet
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.DoubleTapScaleDetector.DoubleTapZoomListener
import net.osmand.plus.views.MultiTouchSupport.MultiTouchZoomListener
import net.osmand.plus.views.layers.base.BaseMapLayer
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.layers.base.OsmandMapLayer.MapGestureType
import net.osmand.plus.views.listeners.MapGestureListener
import net.osmand.render.RenderingRuleSearchRequest
import net.osmand.render.RenderingRuleStorageProperties
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

class OsmandMapTileView(val context: Context, w: Int, h: Int) :
    MapTileDownloader.IMapDownloaderCallback {
    var isMeasureFPS = false
    private val main = FPSMeasurement()
    private val additional = FPSMeasurement()
    private var view: View? = null
    var mapActivity: MapActivity? = null
    var application: OsmandApplication? = null
        private set
    var settings: OsmandSettings? = null
        protected set
    private var canvasColors: CanvasColors? = null
    private var nightMode: Boolean? = null

    private class CanvasColors {
        var colorDay = MAP_DEFAULT_COLOR
        var colorNight = MAP_DEFAULT_COLOR
    }

    private class FPSMeasurement {
        var fpsMeasureCount = 0
        var fpsMeasureMs = 0
        var fpsFirstMeasurement: Long = 0
        var secondaryFPS = 0f

        fun calculateFPS(start: Long, end: Long) {
            fpsMeasureMs += (end - start).toInt()
            fpsMeasureCount++
            if (fpsMeasureCount > 10 || start - fpsFirstMeasurement > 400) {
                fpsFirstMeasurement = start
                secondaryFPS = 1000f * fpsMeasureCount / fpsMeasureMs
                fpsMeasureCount = 0
                fpsMeasureMs = 0
            }
        }
    }

    interface OnLongClickListener {
        fun onLongPressEvent(point: PointF?): Boolean
    }

    interface OnClickListener {
        fun onPressEvent(point: PointF?): Boolean
    }

    interface OnDrawMapListener {
        fun onDrawOverMap()
    }

    private var currentViewport: RotatedTileBox? = null

    // // accumulate
    var rotate = 0f
    var mapPosition = OsmandSettings.CENTER_CONSTANT
    private var mapPositionX = 0
    private var mapRatioX = 0f
    private var mapRatioY = 0f
    private var isShowMapPosition = false
    private var locationListeners: List<IMapLocationListener> = ArrayList()
    private var onLongClickListener: OnLongClickListener? = null
    private var onClickListener: OnClickListener? = null
    private val layers: MutableList<OsmandMapLayer> = ArrayList()
    private var mainLayer: BaseMapLayer? = null
    private val zOrdersLegacy: MutableMap<OsmandMapLayer, Float> = HashMap()
    private val zOrdersOpenGL: MutableMap<OsmandMapLayer, Float> = HashMap()
    private var onDrawMapListener: OnDrawMapListener? = null

    // UI Part
    // handler to refresh map (in ui thread - ui thread is not necessary, but msg queue is required).
    protected var handler: Handler? = null
    private var baseHandler: Handler? = null
    var animatedDraggingThread: AnimateDraggingMapThread? = null
        private set
    var animatedMapMarkersThread: AnimateMapMarkersThread? = null
        private set
    var paintGrayFill: Paint? = null
    var paintBlackFill: Paint? = null
    var paintWhiteFill: Paint? = null
    var paintCenter: Paint? = null
    private var dm: DisplayMetrics? = null
    var mapRenderer: MapRendererView? = null
    private var bufferBitmap: Bitmap? = null
    private var bufferImgLoc: RotatedTileBox? = null
    private var bufferBitmapTmp: Bitmap? = null
    private var paintImg: Paint? = null
    private var mapGestureListener: MapGestureListener? = null
    private var gestureDetector: GestureDetector? = null
    private var multiTouchSupport: MultiTouchSupport? = null
    private var doubleTapScaleDetector: DoubleTapScaleDetector? = null

    //private boolean afterTwoFingersTap = false;
    private var afterDoubleTap = false
    private var wasMapLinkedBeforeGesture = false
    private var firstTouchPointLatLon: LatLon? = null
    private var secondTouchPointLatLon: LatLon? = null
    var isMultiTouch = false
        private set
    var multiTouchStartTime: Long = 0
        private set
    var multiTouchEndTime: Long = 0
        private set
    var isWasZoomInMultiTouch = false
        private set
    var elevationAngle = 0f

    init {
        init(context, w, h)
    }

    // ///////////////////////////// INITIALIZING UI PART ///////////////////////////////////
    fun init(ctx: Context, w: Int, h: Int) {
        application = ctx.applicationContext as OsmandApplication
        settings = application?.settings
        paintGrayFill = Paint()
        paintGrayFill?.color = Color.GRAY
        paintGrayFill?.style = Paint.Style.FILL
        // when map rotate
        paintGrayFill?.isAntiAlias = true
        paintBlackFill = Paint()
        paintBlackFill?.color = Color.BLACK
        paintBlackFill?.style = Paint.Style.FILL
        // when map rotate
        paintBlackFill?.isAntiAlias = true
        paintWhiteFill = Paint()
        paintWhiteFill?.color = Color.WHITE
        paintWhiteFill?.style = Paint.Style.FILL
        // when map rotate
        paintWhiteFill?.isAntiAlias = true
        paintCenter = Paint()
        paintCenter?.style = Paint.Style.STROKE
        paintCenter?.color = Color.rgb(60, 60, 60)
        paintCenter?.strokeWidth = 2f
        paintCenter?.isAntiAlias = true
        paintImg = Paint()
        paintImg?.isFilterBitmap = true
        //		paintImg.setDither(true);
        handler = Handler()
        application?.resourceManager?.renderingBufferImageThread?.looper?.let {
            baseHandler = Handler(it)
        } ?: run { throw IllegalStateException("RenderingBufferImageLooper is null") }
        animatedDraggingThread = AnimateDraggingMapThread(this)
        animatedMapMarkersThread = AnimateMapMarkersThread(this)
        val mgr = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dm = DisplayMetrics()
        mgr.defaultDisplay.getMetrics(dm)
        settings?.let {
            val ll = it.lastKnownMapLocation ?: LatLon(0.0, 0.0)
            currentViewport = RotatedTileBox.RotatedTileBoxBuilder()
                .setLocation(ll.latitude , ll.longitude)
                .setZoom(it.lastKnownMapZoom)
                .setRotate(it.lastKnownMapRotation)
                .setPixelDimensions(w, h)
                .build()
            currentViewport?.density = dm?.density ?: 0f
            setMapDensityImpl(settingsMapDensity)
            elevationAngle = it.lastKnownMapElevation
        }
    }

    fun setupTouchDetectors(ctx: Context) {
        mapGestureListener = ctx as? MapGestureListener
        gestureDetector = GestureDetector(ctx, MapTileViewOnGestureListener())
        application?.let {
            multiTouchSupport = MultiTouchSupport(it, MapTileViewMultiTouchZoomListener())
            doubleTapScaleDetector = DoubleTapScaleDetector(this, ctx, MapTileViewMultiTouchZoomListener())
        }
    }

    fun clearTouchDetectors() {
        mapGestureListener = null
        gestureDetector = null
        multiTouchSupport = null
        doubleTapScaleDetector = null
    }

    fun requireMapActivity(): MapActivity {
        return this.mapActivity
            ?: throw IllegalStateException("$this not attached to MapActivity.")
    }

    val rotatedTileBox: RotatedTileBox?
        get() = currentViewport?.copy()

    fun setView(view: View?) {
        this.view = view?.also {
            it.isClickable = true
            it.isLongClickable = true
            it.isFocusable = true
            if (Build.VERSION.SDK_INT >= 26) {
                it.defaultFocusHighlightEnabled = false
            }
            refreshMap(true)
        }
    }

    fun setupRenderingView() {
        application?.let {
            if (it.isApplicationInitializing) {
                it.appInitializer.addListener(object : AppInitializeListener {
                    override fun onFinish(init: AppInitializer) {
                        it.osmandMap?.setupRenderingView()
                        it.osmandMap?.refreshMap()
                    }
                })
            } else {
                it.osmandMap?.setupRenderingView()
            }
        }
    }

    fun getFPS(): Float = main.secondaryFPS

    fun getSecondaryFPS(): Float = additional.secondaryFPS

    fun backToLocation(zoom: Int = 15) {
        application?.mapViewTrackingUtilities?.backToLocationImpl(zoom)
    }

    fun zoomOut() {
        application?.osmandMap?.changeZoom(-1, System.currentTimeMillis())
    }

    fun zoomIn() {
        val map = application?.osmandMap
        if (isZooming) {
            map?.changeZoom(2, System.currentTimeMillis())
        } else {
            map?.changeZoom(1, System.currentTimeMillis())
        }
    }

    fun scrollMap(dx: Float, dy: Float) {
        moveTo(dx, dy, true)
    }

    fun flingMap(x: Float, y: Float, velocityX: Float, velocityY: Float) {
        animatedDraggingThread?.startDragging(velocityX / 3, velocityY / 3, 0f, 0f, x, y, true)
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean? {
        return if (application?.accessibilityEnabled().toNotNull()) false else null
    }

    @Synchronized
    fun isLayerExists(layer: OsmandMapLayer): Boolean {
        return layers.contains(layer)
    }

    fun getZorder(layer: OsmandMapLayer): Float {
        val useOpenGL = application?.useOpenGlRenderer().toNotNull()
        return (if (useOpenGL) zOrdersOpenGL[layer] else zOrdersLegacy[layer]) ?: return 10f
    }

    fun getLayerIndex(layer: OsmandMapLayer): Int {
        val zOrder = getZorder(layer)
        return (zOrder * 100.0f).toInt()
    }

    @Synchronized
    fun addLayer(layer: OsmandMapLayer, zOrder: Float) {
        addLayer(layer, zOrder, zOrder)
    }

    @Synchronized
    fun addLayer(layer: OsmandMapLayer, zOrderLegacy: Float, zOrderOpenGL: Float) {
        var i = 0
        i = 0
        while (i < layers.size) {
            val legacy = zOrdersLegacy[layers[i]]
            val openGL = zOrdersOpenGL[layers[i]]
            if (legacy != null && openGL != null && legacy > zOrderLegacy && openGL > zOrderOpenGL) {
                break
            }
            i++
        }
        zOrdersLegacy[layer] = zOrderLegacy
        zOrdersOpenGL[layer] = zOrderOpenGL
        layer.initLayer(this)
        layers.add(i, layer)
    }

    @Synchronized
    fun removeLayer(layer: OsmandMapLayer) {
        layer.destroyLayer()
        while (layers.remove(layer));
        zOrdersLegacy.remove(layer)
        zOrdersOpenGL.remove(layer)
    }

    @Synchronized
    fun removeAllLayers() {
        while (layers.size > 0) {
            removeLayer(layers[0])
        }
    }

    fun getLayers(): List<OsmandMapLayer> {
        return layers
    }

    fun <T : OsmandMapLayer?> getLayerByClass(cl: Class<T>): T? {
        for (lr in layers) {
            if (cl.isInstance(lr)) {
                return lr as T
            }
        }
        return null
    }

    val viewHeight: Int
        get() = view?.height ?: 0

    // ///////////////////////// NON UI PART (could be extracted in common) /////////////////////////////
    fun getFirstTouchPointLatLon(): LatLon? {
        return firstTouchPointLatLon
    }

    fun getSecondTouchPointLatLon(): LatLon? {
        return secondTouchPointLatLon
    }

    fun mapGestureAllowed(type: MapGestureType?): Boolean {
        for (layer in layers) {
            if (!layer.isMapGestureAllowed(type)) {
                return false
            }
        }
        return true
    }

    fun setIntZoom(zoom: Int) {
        val coercedZoom = zoom.coerceIn(minZoom .. maxZoom)
        if (mainLayer != null) {
            animatedDraggingThread?.stopAnimating()
            setZoomAndAnimationImpl(coercedZoom, 0.0, 0.0)
            setRotateImpl(rotate)
            refreshMap()
        }
    }

    fun setComplexZoom(zoom: Int, mapDensity: Double) {
        if (mainLayer != null && zoom <= maxZoom && zoom >= minZoom) {
            animatedDraggingThread?.stopAnimating()
            setZoomAndAnimationImpl(zoom, 0.0)
            setMapDensityImpl(mapDensity)
            setRotateImpl(rotate)
            refreshMap()
        }
    }

    fun setMapDensity(mapDensity: Double) {
        if (mainLayer != null) {
            setMapDensityImpl(mapDensity)
        }
    }

    fun resetManualRotation() {
        setRotateValue(0f, true)
    }

    fun setRotateValue(rotate: Float, force: Boolean) {
        if (isMultiTouch) {
            return
        }
        val diff = MapUtils.unifyRotationDiff(rotate, getRotateValue())
        if (abs(diff) > 5 || force) { // check smallest rotation
            animatedDraggingThread?.startRotate(rotate)
        }
    }

    fun showAndHideMapPosition() {
        isShowMapPosition = true
        application?.runMessageInUIThreadAndCancelPrevious(
            SHOW_POSITION_MSG_ID,
            {
                if (isShowMapPosition) {
                    isShowMapPosition = false
                    refreshMap()
                }
            }, 2500
        )
    }

    fun getRotateValue(): Float {
        return currentViewport?.rotate ?: 90f
    }

    fun setLatLon(latitude: Double, longitude: Double) {
        setLatLon(latitude, longitude, false)
    }

    fun setLatLon(latitude: Double, longitude: Double, ratiox: Float, ratioy: Float) {
        setLatLon(latitude, longitude, ratiox, ratioy, false)
    }

    fun setTarget31(x31: Int, y31: Int) {
        setTarget31(x31, y31, false)
    }

    fun setLatLon(latitude: Double, longitude: Double, notify: Boolean) {
        if (animatedDraggingThread?.isAnimatingMapTilt?.not().toNotNull()) {
            animatedDraggingThread?.stopAnimating()
        }
        setLatLonImpl(latitude, longitude)
        refreshMap()
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    fun setLatLon(
        latitude: Double,
        longitude: Double,
        ratiox: Float,
        ratioy: Float,
        notify: Boolean
    ) {
        if (animatedDraggingThread?.isAnimatingMapTilt?.not().toNotNull()) {
            animatedDraggingThread?.stopAnimating()
        }
        setLatLonImpl(latitude, longitude, ratiox, ratioy)
        refreshMap()
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    fun setTarget31(x31: Int, y31: Int, notify: Boolean) {
        animatedDraggingThread?.stopAnimating()
        setTarget31Impl(x31, y31)
        refreshMap()
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    val latitude: Double
        get() = currentViewport?.latitude ?: 0.0
    val longitude: Double
        get() = currentViewport?.longitude ?: 0.0
    val zoom: Int
        get() = currentViewport?.zoom ?: 0

    val zoomFractionalPart: Double?
        get() = currentViewport?.zoomFloatPart
    val settingsMapDensity: Double
        get() {
            val map = application?.osmandMap
            return ((map?.mapDensity ?: settings?.MAP_DENSITY?.get() ?: 1f) * 1f.coerceAtLeast(density ?: 1f)).toDouble()
        }
    val isZooming: Boolean
        get() = currentViewport?.isZoomAnimated ?: false

    fun addMapLocationListener(listener: IMapLocationListener) {
        if (!locationListeners.contains(listener)) {
            locationListeners = Algorithms.addToList(locationListeners, listener)
        }
    }

    fun removeMapLocationListener(listener: IMapLocationListener) {
        locationListeners = Algorithms.removeFromList(locationListeners, listener)
    }

    fun setOnDrawMapListener(listener: OnDrawMapListener?) {
        onDrawMapListener = listener
    }

    // ////////////////////////////// DRAWING MAP PART /////////////////////////////////////////////
    fun getMainLayer(): BaseMapLayer? {
        return mainLayer
    }

    fun setMainLayer(mainLayer: BaseMapLayer?) {
        this.mainLayer = mainLayer
        currentViewport?.zoom?.let {
            var zoom = it
            if (maxZoom < zoom) {
                zoom = maxZoom
            }
            if (minZoom > zoom) {
                zoom = minZoom
            }
            setZoomAndAnimationImpl(zoom, 0.0, 0.0)
            refreshMap()
        }
    }

    fun setMapPositionX(type: Int) {
        mapPositionX = type
    }

    fun setCustomMapRatio(ratioX: Float, ratioY: Float) {
        mapRatioX = ratioX
        mapRatioY = ratioY
    }

    fun restoreMapRatio() {
        currentViewport?.copy()?.let { box ->
            val screenCenter = NativeUtilities.getLatLonFromPixel(
                mapRenderer, box,
                box.pixWidth / 2f, box.pixHeight / 2f
            )
            mapRatioX = 0f
            mapRatioY = 0f
            val ratio = calculateRatio(mapRatioX, mapRatioY)
            setLatLon(screenCenter.latitude, screenCenter.longitude, ratio.x, ratio.y)
        }
    }

    fun hasCustomMapRatio(): Boolean {
        return mapRatioX != 0f && mapRatioY != 0f
    }

    val maxZoom: Int = MAX_ZOOM_LEVEL
    val minZoom: Int = MIN_ZOOM_LEVEL

    private fun drawBasemap(canvas: Canvas) {
        if (bufferImgLoc != null) {
            val rot = -(bufferImgLoc?.rotate ?: 0f)
            canvas.rotate(
                rot,
                currentViewport?.centerPixelX?.toFloat() ?: 0f,
                currentViewport?.centerPixelY?.toFloat() ?: 0f
            )
            safeLet(bufferImgLoc, currentViewport?.copy(), bufferBitmap) { bufferImgLoc, calc, bufferBitmap ->
                calc.rotate = bufferImgLoc.rotate
                val cz = zoom
                val lt = bufferImgLoc.getLeftTopTile(cz.toDouble())
                val rb = bufferImgLoc.getRightBottomTile(cz.toFloat())
                val x1 = calc.getPixXFromTile(lt.x, lt.y, cz.toFloat())
                val x2 = calc.getPixXFromTile(rb.x, rb.y, cz.toFloat())
                val y1 = calc.getPixYFromTile(lt.x, lt.y, cz.toFloat())
                val y2 = calc.getPixYFromTile(rb.x, rb.y, cz.toFloat())
                if (!bufferBitmap.isRecycled) {
                    val rct = RectF(x1, y1, x2, y2)
                    canvas.drawBitmap(bufferBitmap, null, rct, paintImg)
                }
                canvas.rotate(
                    -rot,
                    currentViewport?.centerPixelX?.toFloat() ?: 0f,
                    currentViewport?.centerPixelY?.toFloat() ?: 0f
                )
            }
        }
    }

    private fun refreshBaseMapInternal(tileBox: RotatedTileBox, drawSettings: DrawSettings) {
        if (tileBox.pixHeight == 0 || tileBox.pixWidth == 0) {
            return
        }
        if (bufferBitmapTmp == null || tileBox.pixHeight != bufferBitmapTmp?.height || tileBox.pixWidth != bufferBitmapTmp?.width) {
            bufferBitmapTmp =
                Bitmap.createBitmap(tileBox.pixWidth, tileBox.pixHeight, Bitmap.Config.ARGB_8888)
        }
        val start = SystemClock.elapsedRealtime()
        val c = tileBox.centerPixelPoint
        bufferBitmapTmp?.let {
            val canvas = Canvas(it)
            fillCanvas(canvas, drawSettings)
            for (i in layers.indices) {
                try {
                    val layer = layers[i]
                    canvas.save()
                    // rotate if needed
                    if (!layer.drawInScreenPixels()) {
                        canvas.rotate(tileBox.rotate, c.x, c.y)
                    }
                    layer.onPrepareBufferImage(canvas, tileBox, drawSettings)
                    canvas.restore()
                } catch (e: IndexOutOfBoundsException) {
                    // skip it
                    canvas.restore()
                }
            }
            val t = bufferBitmap
            synchronized(this) {
                bufferImgLoc = tileBox
                bufferBitmap = it
                bufferBitmapTmp = t
            }
            val end = SystemClock.elapsedRealtime()
            additional.calculateFPS(start, end)
        }
    }

    private fun calculateRatio(mapRatioX: Float, mapRatioY: Float): PointF {
        val ratioy: Float = if (mapRatioY != 0f) {
            mapRatioY
        } else if (mapPosition == OsmandSettings.BOTTOM_CONSTANT) {
            0.85f
        } else if (mapPosition == OsmandSettings.MIDDLE_BOTTOM_CONSTANT) {
            0.70f
        } else if (mapPosition == OsmandSettings.MIDDLE_TOP_CONSTANT) {
            0.25f
        } else {
            0.5f
        }
        val ratiox: Float = if (mapRatioX != 0f) {
            mapRatioX
        } else if (mapPosition == OsmandSettings.LANDSCAPE_MIDDLE_RIGHT_CONSTANT) {
            0.7f
        } else {
            if (mapPositionX == 0) 0.5f else if (isLayoutRtl) 0.25f else 0.75f
        }
        return PointF(ratiox, ratioy)
    }

    private fun refreshMapInternal(drawSettings: DrawSettings) {
        view?.let { view ->
            val ratio = calculateRatio(mapRatioX, mapRatioY)
            val cy = (ratio.y * view.height).toInt()
            val cx = (ratio.x * view.width).toInt()
            var updateMapRenderer = false
            val mapRenderer = mapRenderer
            if (mapRenderer != null) {
                val fixedPixel = mapRenderer.state.fixedPixel
                updateMapRenderer = fixedPixel.x <= 0 || fixedPixel.y <= 0
            }
            if (
                updateMapRenderer || currentViewport?.pixWidth != view.width
                || currentViewport?.pixHeight != view.height
                || currentViewport?.centerPixelY != cy
                || currentViewport?.centerPixelX != cx
            ) {
                currentViewport?.setPixelDimensions(view.width, view.height, ratio.x, ratio.y)
                mapRenderer?.setMapTarget(PointI(cx, cy), mapRenderer.target)
                setElevationAngleValue(elevationAngle)
                setMapDensityImpl(settingsMapDensity)
                refreshBufferImage(drawSettings)
            }
            if (view is SurfaceView) {
                val holder = (view as SurfaceView).holder
                val ms = SystemClock.elapsedRealtime()
                synchronized(holder) {
                    val canvas: Canvas? = holder.lockCanvas()
                    if (canvas != null) {
                        try {
                            // make copy to avoid concurrency
                            currentViewport?.copy()?.let {
                                drawOverMap(canvas, it, drawSettings)
                            }
                        } finally {
                            holder.unlockCanvasAndPost(canvas)
                        }
                    }
                    if (isMeasureFPS) {
                        main.calculateFPS(ms, SystemClock.elapsedRealtime())
                    }
                }
            } else {
                view.invalidate()
            }
        }
    }

    private fun fillCanvas(canvas: Canvas, drawSettings: DrawSettings) {
        var color = MAP_DEFAULT_COLOR
        var canvasColors = canvasColors
        if (canvasColors == null) {
            canvasColors = updateCanvasColors()
            this.canvasColors = canvasColors
        }
        if (canvasColors != null) {
            color = if (drawSettings.isNightMode) canvasColors.colorNight else canvasColors.colorDay
        }
        canvas.drawColor(color)
    }

    fun resetDefaultColor() {
        canvasColors = null
    }

    private fun updateCanvasColors(): CanvasColors? {
        var canvasColors: CanvasColors? = null
        val rrs = application?.rendererRegistry?.currentSelectedRenderer
        if (rrs != null) {
            canvasColors = CanvasColors()
            var req = RenderingRuleSearchRequest(rrs)
            req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, false)
            if (req.searchRenderingAttribute(RenderingRuleStorageProperties.A_DEFAULT_COLOR)) {
                canvasColors.colorDay = req.getIntPropertyValue(req.ALL.R_ATTR_COLOR_VALUE)
            }
            req = RenderingRuleSearchRequest(rrs)
            req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, true)
            if (req.searchRenderingAttribute(RenderingRuleStorageProperties.A_DEFAULT_COLOR)) {
                canvasColors.colorNight = req.getIntPropertyValue(req.ALL.R_ATTR_COLOR_VALUE)
            }
        }
        return canvasColors
    }

    val isAnimatingMapZoom: Boolean?
        get() = animatedDraggingThread?.isAnimatingMapZoom
    val isAnimatingMapMove: Boolean?
        get() = animatedDraggingThread?.isAnimatingMapMove
    val isAnimatingMapRotation: Boolean?
        get() = animatedDraggingThread?.isAnimatingMapRotation

    @SuppressLint("WrongCall")
    fun drawOverMap(canvas: Canvas, tileBox: RotatedTileBox, drawSettings: DrawSettings) {
        if (mapRenderer == null) {
            fillCanvas(canvas, drawSettings)
        }
        val c = tileBox.centerPixelPoint
        synchronized(this) {
            if ((bufferBitmap != null) && bufferBitmap?.isRecycled == false && (mapRenderer == null)) {
                canvas.save()
                canvas.rotate(tileBox.rotate, c.x, c.y)
                drawBasemap(canvas)
                canvas.restore()
            }
        }
        if (onDrawMapListener != null) {
            onDrawMapListener?.onDrawOverMap()
        }
        for (i in layers.indices) {
            try {
                val layer = layers[i]
                canvas.save()
                // rotate if needed
                if (!layer.drawInScreenPixels()) {
                    canvas.rotate(tileBox.rotate, c.x, c.y)
                }
                if (mapRenderer != null) {
                    layer.onPrepareBufferImage(canvas, tileBox, drawSettings)
                }
                layer.onDraw(canvas, tileBox, drawSettings)
                canvas.restore()
            } catch (e: IndexOutOfBoundsException) {
                // skip it
            }
        }

        if (isShowMapPosition || animatedDraggingThread?.isAnimatingMapZoom.toNotNull()) {
            drawMapPosition(canvas, c.x, c.y)
        } else if (multiTouchSupport != null && multiTouchSupport?.isInZoomMode.toNotNull()) {
            multiTouchSupport?.let {
                drawMapPosition(
                    canvas,
                    it.centerPoint.x,
                    it.centerPoint.y
                )
            }
        } else if (doubleTapScaleDetector != null && doubleTapScaleDetector?.isInZoomMode.toNotNull()) {
            doubleTapScaleDetector?.let {
                drawMapPosition(
                    canvas,
                    it.centerX,
                    it.centerY
                )
            }
        }
    }

    protected fun drawMapPosition(canvas: Canvas, x: Float, y: Float) {
        safeLet(dm, paintCenter) { dm, paintCenter ->
            canvas.drawCircle(x, y, 3 * dm.density, paintCenter)
            canvas.drawCircle(x, y, 7 * dm.density, paintCenter)
        }
    }

    private fun refreshBufferImage(drawSettings: DrawSettings) {
        if (mapRenderer != null) {
            return
        }
        if (baseHandler?.hasMessages(BASE_REFRESH_MESSAGE)?.not().toNotNull() || drawSettings.isUpdateVectorRendering) {
            val msg = Message.obtain(baseHandler) {
                baseHandler?.removeMessages(BASE_REFRESH_MESSAGE)
                try {
                    var param: DrawSettings = drawSettings
                    val currentNightMode: Boolean? = nightMode
                    if (currentNightMode != null && currentNightMode != param.isNightMode) {
                        param = DrawSettings(currentNightMode, true)
                        resetDefaultColor()
                    }
                    if (handler?.hasMessages(MAP_FORCE_REFRESH_MESSAGE).toNotNull()) {
                        if (!param.isUpdateVectorRendering) {
                            param = DrawSettings(drawSettings.isNightMode, true)
                        }
                        handler?.removeMessages(MAP_FORCE_REFRESH_MESSAGE)
                    }
                    param.mapRefreshTimestamp = System.currentTimeMillis()
                    currentViewport?.let {
                        refreshBaseMapInternal(it.copy(), param)
                    }
                    sendRefreshMapMsg(param, 0)
                } catch (e: Exception) {
                    LOG.error(e.message, e)
                }
            }
            msg.what =
                if (drawSettings.isUpdateVectorRendering) MAP_FORCE_REFRESH_MESSAGE else BASE_REFRESH_MESSAGE
            // baseHandler.sendMessageDelayed(msg, 0);
            baseHandler?.sendMessage(msg)
        }
    }

    // this method could be called in non UI thread
    @JvmOverloads
    fun refreshMap(updateVectorRendering: Boolean = false) {
        if (view != null && view?.isShown.toNotNull()) {
            val drawSettings =
                DrawSettings(false, updateVectorRendering)
            sendRefreshMapMsg(drawSettings, 20)
            refreshBufferImage(drawSettings)
        }
    }

    private fun sendRefreshMapMsg(drawSettings: DrawSettings, delay: Int) {
        if (handler?.hasMessages(MAP_REFRESH_MESSAGE)?.not().toNotNull() || drawSettings.isUpdateVectorRendering) {
            val msg = Message.obtain(handler) {
                val param: DrawSettings = drawSettings
                handler?.removeMessages(MAP_REFRESH_MESSAGE)
                refreshMapInternal(param)
            }
            msg.what = MAP_REFRESH_MESSAGE
            if (delay > 0) {
                handler?.sendMessageDelayed(msg, delay.toLong())
            } else {
                handler?.sendMessage(msg)
            }
        }
    }

    override fun tileDownloaded(request: MapTileDownloader.DownloadRequest?) {
        // force to refresh map because image can be loaded from different threads
        // and threads can block each other especially for sqlite images when they
        // are inserting into db they block main thread
        refreshMap()
    }

    // ///////////////////////////////// DRAGGING PART ///////////////////////////////////////
    val currentRotatedTileBox: RotatedTileBox?
        get() = (currentViewport)

    val density: Float?
        get() = currentViewport?.density

    // large screen
    val scaleCoefficient: Float
        get() {
            var scaleCoefficient = density ?: 1f
            dm?.let { dm ->
                if ((dm.widthPixels / (dm.density * 160)).coerceAtMost(dm.heightPixels / (dm.density * 160)) > 2.5f) {
                    // large screen
                    scaleCoefficient *= 1.5f
                }
            }
            return scaleCoefficient
        }

    /**
     * These methods do not consider rotating
     */
    protected fun dragToAnimate(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        notify: Boolean
    ) {
        //float dx = (fromX - toX);
        //float dy = (fromY - toY);
        //moveTo(dx, dy, false);
        moveTo(fromX, fromY, toX, toY, false)
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    protected fun dragToAnimate(toX31: Int, toY31: Int, notify: Boolean) {
        moveTo(toX31, toY31, false)
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    fun rotateToAnimate(rotate: Float) {
        this.rotate = MapUtils.unifyRotationTo360(rotate)
        setRotateImpl(this.rotate)
        refreshMap()
    }

    fun rotateToAnimate(rotate: Float, centerX: Int, centerY: Int) {
        this.rotate = MapUtils.unifyRotationTo360(rotate)
        setRotateImpl(this.rotate, centerX, centerY)
        refreshMap()
    }

    protected fun setLatLonAnimate(latitude: Double, longitude: Double, notify: Boolean) {
        setLatLonImpl(latitude, longitude)
        refreshMap()
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    protected fun setFractionalZoom(zoom: Int, zoomPart: Double, notify: Boolean) {
        setZoomAndAnimationImpl(zoom, 0.0, zoomPart)
        refreshMap()
        currentViewport?.also {
            mapGestureListener?.onMapGestureDetected(it.centerLatLon, zoom, false)
        }
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    // for internal usage
    private fun setLatLonImpl(latitude: Double, longitude: Double) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            NativeUtilities.calculateTarget31(mapRenderer, latitude, longitude, true)
        }
        currentViewport?.setLatLonCenter(latitude, longitude)
    }

    private fun setLatLonImpl(latitude: Double, longitude: Double, ratiox: Float, ratioy: Float) {
        safeLet(view, currentViewport) { view, currentViewport ->
            val cx = (ratiox * view.width).toInt()
            val cy = (ratioy * view.height).toInt()
            if (currentViewport.centerPixelY == cy && currentViewport.centerPixelX == cx) {
                setLatLonImpl(latitude, longitude)
            } else {
                currentViewport.setPixelDimensions(view.width, view.height, ratiox, ratioy)
                mapRenderer?.let {
                    val target31 =
                        NativeUtilities.calculateTarget31(it, latitude, longitude, false)
                    it.setMapTarget(PointI(cx, cy), target31)
                }
                currentViewport.setLatLonCenter(latitude, longitude)
            }
        }
    }

    private fun setTarget31Impl(x31: Int, y31: Int) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            mapRenderer.target = PointI(x31, y31)
        }
        currentViewport?.setLatLonCenter(
            MapUtils.get31LatitudeY(y31),
            MapUtils.get31LongitudeX(x31)
        )
    }

    private fun setRotateImpl(rotate: Float) {
        currentViewport?.copy()?.let { tb ->
            setRotateImpl(rotate, tb.centerPixelX, tb.centerPixelY)
        }
    }

    private fun setRotateImpl(rotate: Float, centerX: Int, centerY: Int) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            var centerX31 = 0
            var centerY31 = 0
            val center31 = PointI()
            if (mapRenderer.getLocationFromScreenPoint(PointI(centerX, centerY), center31)) {
                centerX31 = center31.x
                centerY31 = center31.y
            }
            val target31 = mapRenderer.target
            val azimuth = mapRenderer.azimuth
            val targetX = target31.x - centerX31
            val targetY = target31.y - centerY31
            val angleR = Math.toRadians((-azimuth - rotate).toDouble())
            val cosAngle = Math.cos(angleR)
            val sinAngle = Math.sin(angleR)
            val newTargetX = (targetX * cosAngle - targetY * sinAngle + centerX31).toInt()
            val newTargetY = (targetX * sinAngle + targetY * cosAngle + centerY31).toInt()
            mapRenderer.target = PointI(newTargetX, newTargetY)
            mapRenderer.azimuth = -rotate
        }
        currentViewport?.rotate = rotate
    }

    private fun setZoomAndAnimationImpl(zoom: Int, zoomAnimation: Double) {
        currentViewport?.let {
            setZoomAndAnimationImpl(zoom, zoomAnimation, it.zoomFloatPart)
        }
    }

    private fun setZoomAndAnimationImpl(
        zoom: Int,
        zoomAnimation: Double,
        centerX: Int,
        centerY: Int
    ) {
        currentViewport?.let {
            setZoomAndAnimationImpl(
                zoom,
                zoomAnimation,
                it.zoomFloatPart,
                centerX,
                centerY
            )
        }
    }

    private fun setZoomAndAnimationImpl(zoom: Int, zoomAnimation: Double, zoomFloatPart: Double) {
        currentViewport?.copy()?.let { tb ->
            setZoomAndAnimationImpl(
                zoom,
                zoomAnimation,
                zoomFloatPart,
                tb.centerPixelX,
                tb.centerPixelY
            )
        }
    }

    private fun setZoomAndAnimationImpl(
        zoom: Int,
        zoomAnimation: Double,
        zoomFloatPart: Double,
        centerX: Int,
        centerY: Int
    ) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            var centerX31Before = 0
            var centerY31Before = 0

            // Get map center in 31
            var center31 = PointI()
            if (mapRenderer.getLocationFromScreenPoint(PointI(centerX, centerY), center31)) {
                centerX31Before = center31.x
                centerY31Before = center31.y
            }

            // Zoom
            mapRenderer.zoom = (zoom + zoomAnimation + zoomFloatPart).toFloat()
            application?.osmandMap?.mapDensity?.let { zoomMagnifier ->
                mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f)
            }

            // Shift map to new center
            center31 = PointI()
            // Get new map center in 31
            if (mapRenderer.getLocationFromScreenPoint(PointI(centerX, centerY), center31)) {
                val centerX31After = center31.x
                val centerY31After = center31.y
                val target31 = mapRenderer.target
                val targetX = target31.x - (centerX31After - centerX31Before)
                val targetY = target31.y - (centerY31After - centerY31Before)
                // Shift map
                mapRenderer.target = PointI(targetX, targetY)
            }
        }
        currentViewport?.setZoomAndAnimation(zoom, zoomAnimation, zoomFloatPart)
        setElevationAngleValue(normalizeElevationAngle(elevationAngle))
    }

    private fun setMapDensityImpl(mapDensity: Double) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            application?.osmandMap?.mapDensity?.let { zoomMagnifier ->
                mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f)
            }
        }
        currentViewport?.mapDensity = mapDensity
    }

    fun normalizeElevationAngle(elevationAngle: Float): Float {
        return if (elevationAngle > 90) 90f else minAllowedElevationAngle.coerceAtLeast(elevationAngle)
    }

    val minAllowedElevationAngle: Float
        get() = 10f


    protected fun zoomToAnimate(
        zoom: Int,
        zoomToAnimate: Double,
        centerX: Int,
        centerY: Int,
        notify: Boolean
    ) {
        if (mainLayer != null && maxZoom >= zoom && minZoom <= zoom) {
            setZoomAndAnimationImpl(zoom, zoomToAnimate, centerX, centerY)
            setRotateImpl(rotate, centerX, centerY)
            refreshMap()
            if (notify) {
                notifyLocationListeners(latitude, longitude)
            }
        }
    }

    private fun zoomToAnimate(
        initialViewport: RotatedTileBox,
        deltaZoom: Float,
        centerX: Int,
        centerY: Int
    ) {
        var deltaZoom = deltaZoom
        var baseZoom = initialViewport.zoom
        while (initialViewport.zoomFloatPart + deltaZoom > 1 && isZoomingAllowed(
                baseZoom,
                deltaZoom
            )
        ) {
            deltaZoom--
            baseZoom++
        }
        while (initialViewport.zoomFloatPart + deltaZoom < 0 && isZoomingAllowed(
                baseZoom,
                deltaZoom
            )
        ) {
            deltaZoom++
            baseZoom--
        }
        if (!isZoomingAllowed(baseZoom, deltaZoom)) {
            deltaZoom = Math.signum(deltaZoom)
        }
        zoomToAnimate(
            baseZoom,
            deltaZoom.toDouble(),
            centerX,
            centerY,
            !(doubleTapScaleDetector != null && doubleTapScaleDetector?.isInZoomMode.toNotNull())
        )
    }

    fun moveTo(dx: Float, dy: Float, notify: Boolean) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            currentViewport?.copy()?.let { tb ->
                val windowSize = mapRenderer.state.windowSize
                val translationPoint31 = NativeUtilities.get31FromPixel(
                    mapRenderer,
                    tb,
                    (windowSize.x / 2 + dx).toInt(),
                    (windowSize.y / 2 + dy).toInt()
                )
                if (translationPoint31 != null) {
                    setTarget31Impl(translationPoint31.x, translationPoint31.y)
                }
            }
        } else {
            currentViewport?.let {
                val cp = it.centerPixelPoint
                val latlon = it.getLatLonFromPixel(cp.x + dx, cp.y + dy)
                setLatLonImpl(latlon.latitude, latlon.longitude)
            }
        }
        refreshMap()
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    fun moveTo(fromX: Float, fromY: Float, toX: Float, toY: Float, notify: Boolean) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            currentViewport?.copy()?.let { tb ->
                val from31 =
                    NativeUtilities.get31FromPixel(mapRenderer, tb, fromX.toInt(), fromY.toInt())
                val to31 = NativeUtilities.get31FromPixel(mapRenderer, tb, toX.toInt(), toY.toInt())
                if (from31 != null && to31 != null) {
                    val target31 = mapRenderer.target
                    setTarget31Impl(
                        target31.x - (to31.x - from31.x),
                        target31.y - (to31.y - from31.y)
                    )
                }
            }
        } else {
            val dx = fromX - toX
            val dy = fromY - toY
            currentViewport?.let {
                val cp = it.centerPixelPoint
                val latlon = it.getLatLonFromPixel(cp.x + dx, cp.y + dy)
                setLatLonImpl(latlon.latitude, latlon.longitude)
            }
        }
        refreshMap()
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    fun moveTo(toX31: Int, toY31: Int, notify: Boolean) {
        setTarget31Impl(toX31, toY31)
        refreshMap()
        if (notify) {
            notifyLocationListeners(latitude, longitude)
        }
    }

    @JvmOverloads
    fun fitRectToMap(
        left: Double, right: Double, top: Double, bottom: Double,
        tileBoxWidthPx: Int, tileBoxHeightPx: Int, marginTopPx: Int, marginLeftPx: Int = 0
    ) {
        currentViewport?.copy()?.let { tb ->
            val border = 0.8
            var dx = 0
            var dy = 0
            var tbw = (tb.pixWidth * border).toInt()
            var tbh = (tb.pixHeight * border).toInt()
            if (tileBoxWidthPx > 0) {
                tbw = (tileBoxWidthPx * border).toInt()
                if (marginLeftPx > 0) {
                    val offset = (tb.pixWidth - tileBoxWidthPx) / 2 - marginLeftPx
                    dx = if (isLayoutRtl) -offset else offset
                }
            } else if (tileBoxHeightPx > 0) {
                tbh = (tileBoxHeightPx * border).toInt()
                dy = (tb.pixHeight - tileBoxHeightPx) / 2 - marginTopPx
            }
            dy += tb.centerPixelY - tb.pixHeight / 2
            tb.setPixelDimensions(tbw, tbh)
            var clat = bottom / 2 + top / 2
            var clon = left / 2 + right / 2
            tb.setLatLonCenter(clat, clon)
            while (tb.zoom < 17 && tb.containsLatLon(top, left) && tb.containsLatLon(bottom, right)) {
                tb.zoom = tb.zoom + 1
            }
            while (tb.zoom >= 7 && (!tb.containsLatLon(top, left) || !tb.containsLatLon(
                    bottom,
                    right
                ))
            ) {
                tb.zoom = tb.zoom - 1
            }
            if (dy != 0 || dx != 0) {
                clat = tb.getLatFromPixel(tb.pixWidth / 2f, tb.pixHeight / 2f + dy)
                clon = tb.getLonFromPixel(tb.pixWidth / 2f + dx, tb.pixHeight / 2f)
            }
            animatedDraggingThread?.startMoving(clat, clon, tb.zoom, true)
        }
    }

    fun fitRectToMap(
        start: LatLon,
        end: LatLon,
        tileBoxWidthPx: Int,
        tileBoxHeightPx: Int,
        leftBottomRectOffset: Int,
        rightTopRectOffset: Int,
        marginTopPx: Int = 0,
        marginLeftPx: Int = 0
    ) {
        val left = start.longitude
        val top = start.latitude
        val right = end.longitude
        val bottom = end.latitude

        currentViewport?.copy()?.let { tileBox ->
            var clat = (bottom + top) / 2
            var clon = (left + right) / 2

            tileBox.setPixelDimensions(tileBoxWidthPx, tileBoxHeightPx)
            tileBox.setLatLonCenter(clat, clon)
            adjustZoomToFitRect(tileBox, left, top, right, bottom)

            if (leftBottomRectOffset != 0 || rightTopRectOffset != 0) {
                val startWithOffset = tileBox.getLatLonFromPixel(
                    tileBox.getPixXFromLatLon(start.latitude, start.longitude) - leftBottomRectOffset,
                    tileBox.getPixYFromLatLon(start.latitude, start.longitude) - rightTopRectOffset
                )
                val endWithOffset = tileBox.getLatLonFromPixel(
                    tileBox.getPixXFromLatLon(end.latitude, end.longitude) + rightTopRectOffset,
                    tileBox.getPixYFromLatLon(end.latitude, end.longitude) + leftBottomRectOffset
                )
                val leftWithOffset = startWithOffset.longitude
                val topWithOffset = startWithOffset.latitude
                val rightWithOffset = endWithOffset.longitude
                val bottomWithOffset = endWithOffset.latitude
                clat = (bottomWithOffset + topWithOffset) / 2
                clon = (leftWithOffset + rightWithOffset) / 2

                tileBox.setLatLonCenter(clat, clon)
                adjustZoomToFitRect(tileBox, leftWithOffset, topWithOffset, rightWithOffset, bottomWithOffset)
            }

            if (marginTopPx != 0 || marginLeftPx != 0) {
                clat = tileBox.getLatFromPixel(tileBox.pixWidth / 2f, tileBox.pixHeight / 2f + marginTopPx)
                clon = tileBox.getLonFromPixel(tileBox.pixWidth / 2f + marginLeftPx, tileBox.pixHeight / 2f)
            }

            animatedDraggingThread?.startMoving(clat, clon, tileBox.zoom, true)
        }
    }

    private fun adjustZoomToFitRect(tileBox: RotatedTileBox, left: Double, top: Double, right: Double, bottom: Double) {
        while (tileBox.zoom < PLAN_ROUTE_MAX_ZOOM && tileBox.containsLatLon(top, left) && tileBox.containsLatLon(bottom, right)) {
            tileBox.zoom += 1
        }
        while (tileBox.zoom > PLAN_ROUTE_MIN_ZOOM && (!tileBox.containsLatLon(top, left) || !tileBox.containsLatLon(bottom, right))) {
            tileBox.zoom -= 1
        }
    }

    fun getTileBox(tileBoxWidthPx: Int, tileBoxHeightPx: Int, marginTopPx: Int): RotatedTileBox? {
        return currentViewport?.copy()?.let { tb ->
            val border = 0.8
            var dy = 0
            var tbw = (tb.pixWidth * border).toInt()
            var tbh = (tb.pixHeight * border).toInt()
            if (tileBoxWidthPx > 0) {
                tbw = (tileBoxWidthPx * border).toInt()
            } else if (tileBoxHeightPx > 0) {
                tbh = (tileBoxHeightPx * border).toInt()
                dy = (tb.pixHeight - tileBoxHeightPx) / 2 - marginTopPx
            }
            dy += tb.centerPixelY - tb.pixHeight / 2
            tb.setPixelDimensions(tbw, tbh)
            if (dy != 0) {
                val clat =
                    tb.getLatFromPixel((tb.pixWidth / 2).toFloat(), (tb.pixHeight / 2 - dy).toFloat())
                val clon = tb.getLonFromPixel((tb.pixWidth / 2).toFloat(), (tb.pixHeight / 2).toFloat())
                tb.setLatLonCenter(clat, clon)
            }
            return tb
        }
    }

    fun fitLocationToMap(
        clat: Double, clon: Double, zoom: Int,
        tileBoxWidthPx: Int, tileBoxHeightPx: Int, marginTopPx: Int, animated: Boolean
    ) {
        var mutableClat = clat
        var mutableClon = clon
        currentViewport?.copy()?.let { tb ->
            var dy = 0
            val tbw = if (tileBoxWidthPx > 0) tileBoxWidthPx else tb.pixWidth
            var tbh = tb.pixHeight
            if (tileBoxHeightPx > 0) {
                tbh = tileBoxHeightPx
                dy = (tb.pixHeight - tileBoxHeightPx) / 2 - marginTopPx
            }
            dy += tb.centerPixelY - tb.pixHeight / 2
            tb.setPixelDimensions(tbw, tbh)
            tb.setLatLonCenter(mutableClat, mutableClon)
            tb.zoom = zoom
            if (dy != 0) {
                mutableClat =
                    tb.getLatFromPixel((tb.pixWidth / 2).toFloat(), (tb.pixHeight / 2 + dy).toFloat())
                mutableClon = tb.getLonFromPixel((tb.pixWidth / 2).toFloat(), (tb.pixHeight / 2).toFloat())
            }
            if (animated) {
                animatedDraggingThread?.startMoving(mutableClat, mutableClon, tb.zoom, true)
            } else {
                setLatLon(mutableClat, mutableClon)
            }
        }
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0 && event.action == MotionEvent.ACTION_SCROLL && event.getAxisValue(
                MotionEvent.AXIS_VSCROLL
            ) != 0f
        ) {
            currentRotatedTileBox?.let { tb ->
                val lat = tb.getLatFromPixel(event.x, event.y)
                val lon = tb.getLonFromPixel(event.x, event.y)
                val zoomDir = if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0) -1 else 1
                animatedDraggingThread?.startMoving(lat, lon, zoom + zoomDir, true)
                return true
            }
        }
        return false
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (mapRenderer != null) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                mapRenderer?.suspendSymbolsUpdate()
            } else if (event.action == MotionEvent.ACTION_UP
                || event.action == MotionEvent.ACTION_CANCEL
            ) {
                mapRenderer?.resumeSymbolsUpdate()
            }
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            animatedDraggingThread?.stopAnimating()
        }

        val isMultiTouch = multiTouchSupport != null && multiTouchSupport?.onTouchEvent(event).toNotNull()
        val isDoubleTap = doubleTapScaleDetector?.onTouchEvent(event).toNotNull()

        if (isMultiTouch || isDoubleTap || event.action == MotionEvent.ACTION_MOVE) {
            val isLinkedToLocation = application?.mapViewTrackingUtilities?.isMapLinkedToLocation ?: false
            val viewport = currentViewport
            if (viewport != null && (isLinkedToLocation.not() || isDoubleTap)) {
                mapGestureListener?.onMapGestureDetected(viewport.centerLatLon, viewport.zoom, true)
            }
        }

        currentRotatedTileBox?.let {
            if (!isMultiTouch && doubleTapScaleDetector != null && doubleTapScaleDetector?.isInZoomMode?.not().toNotNull()) {
                for (i in layers.indices.reversed()) {
                    layers[i].onTouchEvent(event, it)
                }
            }
        }

        if (doubleTapScaleDetector != null && doubleTapScaleDetector?.isInZoomMode?.not().toNotNull()
            && doubleTapScaleDetector?.isDoubleTapping?.not().toNotNull() && gestureDetector != null
        ) {
            gestureDetector?.onTouchEvent(event)
        }

        return true
    }

    fun hasMapRenderer(): Boolean {
        return mapRenderer != null
    }

    fun setOnLongClickListener(l: OnLongClickListener?) {
        onLongClickListener = l
    }

    fun setOnClickListener(l: OnClickListener?) {
        onClickListener = l
    }

    fun showMessage(msg: String?) {
        handler?.post {
            Toast.makeText(
                application,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private inner class MapTileViewMultiTouchZoomListener : MultiTouchZoomListener, DoubleTapZoomListener {

        private var initialMultiTouchCenterPoint: PointF? = null
        private var initialViewport: RotatedTileBox? = null
        private var x1 = 0f
        private var y1 = 0f
        private var x2 = 0f
        private var y2 = 0f
        private var initialCenterLatLon: LatLon? = null

        override fun onZoomOrRotationEnded(relativeToStart: Double) {
            // 1.5 works better even on dm.density=1 devices
            val deltaZoom = calculateDeltaZoom(relativeToStart).toFloat()
            initialViewport?.let {
                setIntZoom(deltaZoom.roundToInt() + it.zoom)
                rotateToAnimate(it.rotate)
            }
        }

        override fun onZoomEnded(relativeToStart: Double) {
            // 1.5 works better even on dm.density=1 devices
            val dz = ((relativeToStart - 1) * DoubleTapScaleDetector.SCALE_PER_SCREEN).toFloat()
            safeLet(initialViewport, application) { it, application ->
                setIntZoom(dz.roundToInt() + it.zoom)
            }
        }

        override fun onGestureInit(x1: Float, y1: Float, x2: Float, y2: Float) {
            this.x1 = x1
            this.y1 = y1
            this.x2 = x2
            this.y2 = y2
            if (x1 != x2 || y1 != y2) {
                val mapRenderer = mapRenderer
                if (mapRenderer != null) {
                    val animator = mapRenderer.mapAnimator
                    if (animator != null) {
                        animator.pause()
                        animator.cancelAllAnimations()
                    }
                }
                currentViewport?.let {
                    firstTouchPointLatLon = NativeUtilities.getLatLonFromPixel(mapRenderer, it, x1, y1)
                    secondTouchPointLatLon = NativeUtilities.getLatLonFromPixel(mapRenderer, it, x2, y2)
                }
                isMultiTouch = true
                isWasZoomInMultiTouch = false
                multiTouchStartTime = System.currentTimeMillis()
            }
        }

        override fun onActionPointerUp() {
            isMultiTouch = false
            if (isZooming) {
                isWasZoomInMultiTouch = true
            } else {
                multiTouchEndTime = System.currentTimeMillis()
                isWasZoomInMultiTouch = false
            }
        }

        override fun onActionCancel() {
            isMultiTouch = false
        }

        override fun onZoomStarted(centerPoint: PointF) {
            initialMultiTouchCenterPoint = centerPoint
            initialViewport = currentRotatedTileBox?.copy()?.also {
                initialMultiTouchCenterPoint?.let { point ->
                    initialCenterLatLon = NativeUtilities.getLatLonFromPixel(
                        mapRenderer, it, point.x, point.y
                    )
                }
            }
        }

        override fun onZoomingOrRotating(relativeToStart: Double) {
            var deltaZoom = calculateDeltaZoom(relativeToStart)
            if (abs(deltaZoom) <= 0.1) {
                deltaZoom = 0.0 // keep only rotating
            }
            if (deltaZoom != 0.0) {
                changeZoomPosition(deltaZoom.toFloat())
            }
        }

        override fun onZooming(relativeToStart: Double) {
            val dz = (relativeToStart - 1) * DoubleTapScaleDetector.SCALE_PER_SCREEN
            changeZoomPosition(dz.toFloat())
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            LOG.debug("onDoubleTap getZoom()")
            return if (doubleTapScaleDetector != null && doubleTapScaleDetector?.isInZoomMode?.not().toNotNull()) {
                if (isZoomingAllowed(zoom, 1.1f)) {
                    currentRotatedTileBox?.let {
                        val latlon = NativeUtilities.getLatLonFromPixel(mapRenderer, it, e.x, e.y)
                        animatedDraggingThread?.startMoving(
                            latlon.latitude, latlon.longitude, zoom + 1, true
                        )
                    }
                }
                afterDoubleTap = true
                true
            } else {
                false
            }
        }

        private fun calculateDeltaZoom(relativeToStart: Double): Double {
            val deltaZoom = Math.log(relativeToStart) / Math.log(2.0) * 1.5
            if (deltaZoom > 0.0 && deltaZoom > Companion.MAX_DELTA_ZOOM) {
                return Companion.MAX_DELTA_ZOOM.toDouble()
            } else if (deltaZoom < 0.0 && deltaZoom < -Companion.MAX_DELTA_ZOOM) {
                return (-Companion.MAX_DELTA_ZOOM).toDouble()
            }
            return deltaZoom
        }

        private fun changeZoomPosition(deltaZoom: Float) {
            safeLet(multiTouchSupport, initialViewport?.copy()) { multiTouchSupport, calc ->
                val cp = calc.centerPixelPoint
                var multiTouchCenterX: Int = 0
                var multiTouchCenterY: Int = 0
                if (multiTouchSupport.isInZoomMode) {
                    multiTouchCenterX = multiTouchSupport.centerPoint.x.toInt()
                    multiTouchCenterY = multiTouchSupport.centerPoint.y.toInt()
                } else {
                    initialMultiTouchCenterPoint?.let {
                        multiTouchCenterX = it.x.toInt()
                        multiTouchCenterY = it.y.toInt()
                    }
                }
                initialCenterLatLon?.let {
                    calc.setLatLonCenter(it.latitude, it.longitude)
                }
                val calcRotate = calc.rotate
                calc.setZoomAndAnimation(
                    calc.zoom,
                    deltaZoom.toDouble(),
                    calc.zoomFloatPart
                )
                if (isMultiTouch) {
                    isWasZoomInMultiTouch = true
                }
                // Keep zoom center fixed or flexible
                if (mapRenderer != null) {
                    zoomToAnimate(calc, deltaZoom, multiTouchCenterX, multiTouchCenterY)
                    rotateToAnimate(calcRotate, multiTouchCenterX, multiTouchCenterY)
                } else {
                    val r = calc.getLatLonFromPixel(
                        cp.x + cp.x - multiTouchCenterX,
                        cp.y + cp.y - multiTouchCenterY
                    )
                    setLatLon(r.latitude, r.longitude)
                    zoomToAnimate(calc, deltaZoom, multiTouchCenterX, multiTouchCenterY)
                    rotateToAnimate(calcRotate)
                }
            }
        }
    }

    private fun setElevationAngleValue(angle: Float) {
        var mutableAngle = angle
        mutableAngle = normalizeElevationAngle(mutableAngle)
        elevationAngle = mutableAngle
        application?.osmandMap?.setMapElevation(mutableAngle)
    }

    private fun isZoomingAllowed(baseZoom: Int, dz: Float): Boolean {
        if (baseZoom > maxZoom) {
            return false
        }
        if (baseZoom > maxZoom - 1 && dz > 1) {
            return false
        }
        return if (baseZoom < minZoom) {
            false
        } else baseZoom >= minZoom + 1 || !(dz < -1)
    }

    private fun notifyLocationListeners(lat: Double, lon: Double) {
        for (listener in locationListeners) {
            listener.locationChanged(lat, lon, this)
        }
    }

    private inner class MapTileViewOnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            val mapRenderer = mapRenderer
            val animator = mapRenderer?.mapAnimator
            if (animator != null) {
                animator.pause()
                animator.cancelAllAnimations()
            }
            // Facilitates better map re-linking for two finger tap zoom out
            wasMapLinkedBeforeGesture =
                application?.mapViewTrackingUtilities?.isMapLinkedToLocation ?: false
            return false
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            animatedDraggingThread?.startDragging(
                velocityX / 3, velocityY / 3,
                e1?.x ?: 0F, e1?.y ?: 0F, e2.x, e2.y, true
            )
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (multiTouchSupport?.isInZoomMode.toNotNull() || doubleTapScaleDetector?.isInZoomMode.toNotNull() || doubleTapScaleDetector?.isDoubleTapping.toNotNull()) {
                return
            }
            if (LOG.isDebugEnabled) {
                LOG.debug("On long click event " + e.x + " " + e.y) //$NON-NLS-2$
            }
            val point = PointF(e.x, e.y)
            for (i in layers.indices.reversed()) {
                currentRotatedTileBox?.let {
                    if (layers[i].onLongPressEvent(point, it)) {
                        return
                    }
                }
            }
            if (onLongClickListener?.onLongPressEvent(point).toNotNull()) {
                return
            }
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (multiTouchSupport?.isInTiltMode?.not().toNotNull()) {
                dragToAnimate(e2.x + distanceX, e2.y + distanceY, e2.x, e2.y, true)
            }
            return true
        }

        override fun onShowPress(e: MotionEvent) {}
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (doubleTapScaleDetector?.isDoubleTapping.toNotNull() || afterDoubleTap) {
                // Needed to suppress false single tap detection if we mask MotionEvents for gestures on isDoubleTapping()
                afterDoubleTap = false
                return true
            }
            val point = PointF(e.x, e.y)
            if (LOG.isDebugEnabled) {
                LOG.debug("On click event " + point.x + " " + point.y) //$NON-NLS-2$
            }
            for (i in layers.indices.reversed()) {
                currentRotatedTileBox?.let {
                    if (layers[i].onSingleTap(point, it)) {
                        return true
                    }
                }
            }
            return onClickListener?.onPressEvent(point).toNotNull()
        }
    }

    val resources: Resources?
        get() = application?.resources
    val isLayoutRtl: Boolean
        get() = AndroidUtils.isLayoutRtl(application)

    fun getLatLonFromPixel(x: Float, y: Float): LatLon? {
        val tileBox = currentRotatedTileBox
        return mapRenderer?.let {
            NativeUtilities.getLatLonFromPixel(
                it,
                tileBox,
                PointI(x.toInt(), y.toInt())
            )
        }
    }

    companion object {
        const val DEFAULT_ELEVATION_ANGLE = 90f
        const val MAP_DEFAULT_COLOR = -0x14181c
        private const val SHOW_POSITION_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 1
        private const val MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 4
        private const val MAP_FORCE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 5
        private const val BASE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 3
        private const val ANGLE_THRESHOLD = 30f
        private const val MAX_DELTA_ZOOM = 4f
        private const val PLAN_ROUTE_MIN_ZOOM = 5
        private const val PLAN_ROUTE_MAX_ZOOM = 17
        protected val LOG = PlatformUtil.getLog(OsmandMapTileView::class.java)
    }
}