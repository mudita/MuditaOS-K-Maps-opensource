package net.osmand.plus.views.layers

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import net.osmand.Location
import net.osmand.StateChangedListener
import net.osmand.data.LatLon
import net.osmand.data.RotatedTileBox
import net.osmand.plus.OsmandApplication
import com.mudita.maps.R
import com.mudita.map.common.enums.DistanceByTapTextSize
import net.osmand.plus.utils.BooleanUtils.toNotNull
import net.osmand.plus.utils.NativeUtilities
import com.mudita.map.common.utils.OsmAndFormatter
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.geometry.GeometryWay
import net.osmand.util.MapUtils
import kotlin.math.pow
import kotlin.math.sqrt

class DistanceRulerControlLayer(ctx: Context) : OsmandMapLayer(ctx) {
    private val app: OsmandApplication by lazy {
        application
    }
    private val acceptableTouchRadius by lazy {
        app.resources.getDimensionPixelSize(R.dimen.acceptable_touch_radius)
    }
    private val centerIconDay: Bitmap by lazy {
        BitmapFactory.decodeResource(view.resources, R.drawable.map_ruler_center_day)
    }
    private val centerIconNight: Bitmap by lazy {
        BitmapFactory.decodeResource(view.resources, R.drawable.map_ruler_center_night)
    }
    private val lineAttrs: RenderingLineAttributes by lazy {
        RenderingLineAttributes("rulerLine")
    }
    private val lineFontAttrs: RenderingLineAttributes by lazy {
        RenderingLineAttributes("rulerLineFont")
    }
    private var bitmapPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
    }
    private var touchPoint = PointF()
    private var showTwoFingersDistance = false
    private var showDistBetweenFingerAndLocation = false
    private var touchOutside = false
    private var touched = false
    private var wasZoom = false
    private var cacheMultiTouchEndTime: Long = 0
    private var touchStartTime: Long = 0
    private var touchEndTime: Long = 0
    private val tx = mutableListOf<Float>()
    private val ty = mutableListOf<Float>()
    private val linePath = Path()
    private var touchPointLatLon: LatLon? = null
    private var handler: Handler? = null
    private var textSizeListener: StateChangedListener<DistanceByTapTextSize>? = null


    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                view.refreshMap()
            }
        }
        addTextSizeListener()
        updateTextSize()
    }

    override fun isMapGestureAllowed(type: MapGestureType): Boolean {
        return !rulerModeOn() || type != MapGestureType.TWO_POINTERS_ZOOM_OUT
    }

    override fun onTouchEvent(event: MotionEvent, tileBox: RotatedTileBox): Boolean {
        if (rulerModeOn() && !showTwoFingersDistance) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                touched = true
                touchOutside = false
                touchPoint[event.x] = event.y
                touchPointLatLon = NativeUtilities.getLatLonFromPixel(
                    mapRenderer, tileBox,
                    event.x, event.y
                )
                touchStartTime = System.currentTimeMillis()
                wasZoom = false
            } else if (event.action == MotionEvent.ACTION_MOVE && !touchOutside &&
                !(touched && showDistBetweenFingerAndLocation)
            ) {
                val d = sqrt(
                    (event.x - touchPoint.x).toDouble().pow(2.0) + (event.y - touchPoint.y).toDouble()
                        .pow(2.0)
                )
                if (d > acceptableTouchRadius) {
                    touchOutside = true
                }
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                touched = false
                touchEndTime = System.currentTimeMillis()
                refreshMapDelayed()
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas, tb: RotatedTileBox, settings: DrawSettings) {
        if (rulerModeOn()) {
            val app = view.application
            lineAttrs.updatePaints(app, settings, tb)
            lineFontAttrs.updatePaints(app, settings, tb)
            lineFontAttrs.paint.style = Paint.Style.FILL
            val currentTime = System.currentTimeMillis()
            if (cacheMultiTouchEndTime != view.multiTouchEndTime) {
                cacheMultiTouchEndTime = view.multiTouchEndTime
                refreshMapDelayed()
            }
            if (touched && view.isMultiTouch) {
                touched = false
                touchEndTime = currentTime
            }
            if (tb.isZoomAnimated) {
                wasZoom = true
            }
            showTwoFingersDistance = !tb.isZoomAnimated &&
                    !view.isWasZoomInMultiTouch && currentTime - view.multiTouchStartTime > DELAY_BEFORE_DRAW &&
                    (view.isMultiTouch || currentTime - cacheMultiTouchEndTime < DRAW_TIME)
            showDistBetweenFingerAndLocation = !wasZoom &&
                    !showTwoFingersDistance &&
                    !view.isMultiTouch &&
                    !touchOutside && touchStartTime - view.multiTouchStartTime > DELAY_BEFORE_DRAW && currentTime - touchStartTime > DELAY_BEFORE_DRAW &&
                    (touched || currentTime - touchEndTime < DRAW_TIME)
            val currentLoc = app?.locationProvider?.lastKnownLocation
            if (currentLoc != null && showDistBetweenFingerAndLocation) {
                drawDistBetweenFingerAndLocation(canvas, tb, currentLoc, settings.isNightMode)
            } else if (showTwoFingersDistance) {
                drawTwoFingersDistance(
                    canvas, tb, view.getFirstTouchPointLatLon(),
                    view.getSecondTouchPointLatLon(), settings.isNightMode
                )
            }
        }
    }

    fun rulerModeOn(): Boolean {
        return app.settings?.SHOW_DISTANCE_RULER?.get().toNotNull()
    }

    private fun refreshMapDelayed() {
        handler?.sendEmptyMessageDelayed(0, DRAW_TIME + 50)
    }

    private fun drawTwoFingersDistance(
        canvas: Canvas, tb: RotatedTileBox, firstTouch: LatLon?,
        secondTouch: LatLon?, nightMode: Boolean
    ) {
        if (firstTouch != null && secondTouch != null) {
            val firstScreenPoint = NativeUtilities.getPixelFromLatLon(
                mapRenderer, tb, firstTouch.latitude, firstTouch.longitude
            )
            val secondScreenPoint = NativeUtilities.getPixelFromLatLon(
                mapRenderer, tb, secondTouch.latitude, secondTouch.longitude
            )
            val x1 = firstScreenPoint.x
            val y1 = firstScreenPoint.y
            val x2 = secondScreenPoint.x
            val y2 = secondScreenPoint.y
            val path = Path()
            path.moveTo(x1, y1)
            path.lineTo(x2, y2)
            canvas.rotate(-tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
            canvas.drawPath(path, lineAttrs.paint)
            drawFingerTouchIcon(canvas, x1, y1, nightMode)
            drawFingerTouchIcon(canvas, x2, y2, nightMode)
            canvas.rotate(tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
        }
    }

    private fun drawTextOnCenterOfPath(
        canvas: Canvas,
        x1: Float,
        x2: Float,
        path: Path,
        text: String
    ) {
        val pm = PathMeasure(path, false)
        val bounds = Rect()
        lineFontAttrs.paint.getTextBounds(text, 0, text.length, bounds)
        val hOffset = pm.length / 2 - bounds.width() / 2f
        if (x1 >= x2) {
            val pos = FloatArray(2)
            pm.getPosTan(pm.length / 2, pos, null)
            canvas.rotate(180f, pos[0], pos[1])
            canvas.drawTextOnPath(
                text,
                path,
                hOffset,
                (bounds.height() + VERTICAL_OFFSET).toFloat(),
                lineFontAttrs.paint2
            )
            canvas.drawTextOnPath(
                text,
                path,
                hOffset,
                (bounds.height() + VERTICAL_OFFSET).toFloat(),
                lineFontAttrs.paint
            )
            canvas.rotate(-180f, pos[0], pos[1])
        } else {
            canvas.drawTextOnPath(
                text,
                path,
                hOffset,
                -VERTICAL_OFFSET.toFloat(),
                lineFontAttrs.paint2
            )
            canvas.drawTextOnPath(
                text,
                path,
                hOffset,
                -VERTICAL_OFFSET.toFloat(),
                lineFontAttrs.paint
            )
        }
    }

    private fun drawFingerTouchIcon(canvas: Canvas, x: Float, y: Float, nightMode: Boolean) {
        if (nightMode) {
            canvas.drawBitmap(
                centerIconNight, x - centerIconNight.width / 2f,
                y - centerIconNight.height / 2f, bitmapPaint
            )
        } else {
            canvas.drawBitmap(
                centerIconDay, x - centerIconDay.width / 2f,
                y - centerIconDay.height / 2f, bitmapPaint
            )
        }
    }

    private fun drawDistBetweenFingerAndLocation(
        canvas: Canvas,
        tb: RotatedTileBox,
        currLoc: Location,
        night: Boolean
    ) {
        touchPointLatLon?.let {
            val firstScreenPoint = NativeUtilities.getPixelFromLatLon(
                mapRenderer, tb, it.latitude, it.longitude
            )
            val secondScreenPoint = NativeUtilities.getPixelFromLatLon(
                mapRenderer, tb, currLoc.latitude, currLoc.longitude
            )
            val x = firstScreenPoint.x
            val y = firstScreenPoint.y
            val currX = secondScreenPoint.x
            val currY = secondScreenPoint.y
            linePath.reset()
            tx.clear()
            ty.clear()
            tx.add(x)
            ty.add(y)
            tx.add(currX)
            ty.add(currY)
            GeometryWay.calculatePath(tb, tx, ty, linePath)
            val dist =
                MapUtils.getDistance(it, currLoc.latitude, currLoc.longitude).toFloat()
            canvas.rotate(-tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
            canvas.drawPath(linePath, lineAttrs.paint)
            drawFingerTouchIcon(canvas, x, y, night)
            canvas.rotate(tb.rotate, tb.centerPixelX.toFloat(), tb.centerPixelY.toFloat())
        }
    }

    override fun drawInScreenPixels(): Boolean {
        return false
    }

    private fun addTextSizeListener() {
        textSizeListener =
            StateChangedListener { updateTextSize() }
        app.settings?.DISTANCE_BY_TAP_TEXT_SIZE?.addListener(textSizeListener)
    }

    private fun updateTextSize() {

    }

    companion object {
        private const val VERTICAL_OFFSET = 15
        private const val DRAW_TIME: Long = 4000
        private const val DELAY_BEFORE_DRAW: Long = 200
        private const val DISTANCE_TEXT_SIZE = 16
    }
}