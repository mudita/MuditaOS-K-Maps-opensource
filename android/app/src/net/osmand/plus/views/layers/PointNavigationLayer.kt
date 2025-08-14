package net.osmand.plus.views.layers

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.mudita.maps.R
import kotlin.math.abs
import kotlin.math.sqrt
import net.osmand.core.jni.MapMarker
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.TextRasterizer
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.plus.helpers.TargetPointsHelper
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

private const val FLAG_MARGIN_WIDTH_FRACTION = -0.1f
private const val FLAG_MARGIN_HEIGHT_FRACTION = -0.925f
private const val START_POINT_MARGIN_WIDTH_FRACTION = 0.5f
private const val START_POINT_MARGIN_HEIGHT_FRACTION = 0.575f

class PointNavigationLayer(context: Context) : OsmandMapLayer(context),
    IContextMenuProvider, IMoveObjectProvider {

    private val targetPoints: TargetPointsHelper? by lazy {
        application.targetPointsHelper
    }
    private var mBitmapPaint = Paint().apply {
        isDither = true
        isAntiAlias = true
        isFilterBitmap = true
    }
    private var mTextPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private var carView = false
    private var _textScale = 1f
    private var pointSizePx = 0.0
    private var mStartPoint: Bitmap? = null
    private var mTargetPoint: Bitmap? = null
    private var mIntermediatePoint: Bitmap? = null
    private var contextMenuLayer: ContextMenuLayer? = null

    //OpenGL
    private var captionStyle: TextRasterizer.Style? = null
    private var renderedPoints: List<TargetPoint>? = null
    private var nightMode = false

    private fun initUI() {
        updateTextSize()
        updateBitmaps(true)
    }

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        initUI()
        contextMenuLayer = view.getLayerByClass(ContextMenuLayer::class.java)
    }

    override fun onDraw(canvas: Canvas, tb: RotatedTileBox, nightMode: DrawSettings) {
        if (tb.zoom < 3) {
            clearMapMarkersCollections()
            return
        }
        updateBitmaps(false)
        if (mapView.hasMapRenderer()) {
            val movableObject = contextMenuLayer?.moveableObject
            if (movableObject is TargetPoint) {
                targetPoints?.let {
                    //draw movable object on canvas
                    if (it.pointToStart === movableObject) {
                        drawStartPoint(canvas, tb, movableObject)
                    } else if (it.pointToNavigate === movableObject) {
                        drawPointToNavigate(canvas, tb, movableObject)
                    } else if (it.intermediatePoints.contains(movableObject)) {
                        drawIntermediatePoint(
                            canvas,
                            tb,
                            movableObject,
                            it.intermediatePoints.indexOf(movableObject) + 1
                        )
                    }
                    setMovableObject(movableObject.latitude, movableObject.longitude)
                }
            }
            contextMenuLayer?.let {
                if (this.movableObject != null) {
                    cancelMovableObject()
                }
            }
            return
        }
        targetPoints?.let {
            val pointToStart = it.pointToStart
            if (pointToStart != null) {
                if (isLocationVisible(tb, pointToStart)) {
                    drawStartPoint(canvas, tb, pointToStart)
                }
            }
            var index = 0
            for (ip in it.intermediatePoints) {
                index++
                if (isLocationVisible(tb, ip)) {
                    drawIntermediatePoint(canvas, tb, ip, index)
                }
            }
            val pointToNavigate = it.pointToNavigate
            if (isLocationVisible(tb, pointToNavigate)) {
                drawPointToNavigate(canvas, tb, pointToNavigate)
            }
        }
    }

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        val mapRenderer = mapView.mapRenderer
        if (mapRenderer != null) {
            //OpenGL
            if (nightMode != settings.isNightMode || mapActivityInvalidated) {
                //switch to day/night mode
                captionStyle = null
                clearMapMarkersCollections()
                nightMode = settings.isNightMode
            }
            targetPoints?.let {
                val pointToStart = it.pointToStart
                val pointToNavigate = it.pointToNavigate
                val intermediatePoints = it.intermediatePoints
                val allPoints = mutableListOf<TargetPoint>()
                if (pointToStart != null) {
                    allPoints.add(pointToStart)
                }
                if (!Algorithms.isEmpty(intermediatePoints)) {
                    allPoints.addAll(intermediatePoints)
                }
                if (pointToNavigate != null) {
                    allPoints.add(pointToNavigate)
                }
                val renderedPoints = renderedPoints
                if (renderedPoints != null) {
                    if (allPoints.isEmpty() || renderedPoints.size != allPoints.size) {
                        clearMapMarkersCollections()
                    } else {
                        for (i in allPoints.indices) {
                            val r = renderedPoints[i]
                            val a = allPoints[i]
                            if (a != r) {
                                clearMapMarkersCollections()
                                break
                            }
                        }
                    }
                }
                var markersCollection = mapMarkersCollection
                if (markersCollection == null && allPoints.isNotEmpty()) {
                    markersCollection = MapMarkersCollection()
                    if (pointToStart != null) {
                        val x = MapUtils.get31TileNumberX(pointToStart.longitude)
                        val y = MapUtils.get31TileNumberY(pointToStart.latitude)
                        mStartPoint?.let { startPoint ->
                            drawMarker(markersCollection, startPoint, PointI(x, y), null)
                        }
                    }
                    for (i in intermediatePoints.indices) {
                        val ip = intermediatePoints[i]
                        val x = MapUtils.get31TileNumberX(ip.longitude)
                        val y = MapUtils.get31TileNumberY(ip.latitude)
                        mIntermediatePoint?.let { intermediatePoint ->
                            drawMarker(
                                markersCollection,
                                intermediatePoint,
                                PointI(x, y),
                                (i + 1).toString()
                            )
                        }
                    }
                    if (pointToNavigate != null) {
                        val x = MapUtils.get31TileNumberX(pointToNavigate.longitude)
                        val y = MapUtils.get31TileNumberY(pointToNavigate.latitude)
                        mTargetPoint?.let { targetPoint ->
                            drawMarker(markersCollection, targetPoint, PointI(x, y), null)
                        }
                    }
                    mapRenderer.addSymbolsProvider(markersCollection)
                    mapMarkersCollection = markersCollection
                }
                this.renderedPoints = allPoints
            }
        }
        mapActivityInvalidated = false
    }

    private fun updateBitmaps(forceUpdate: Boolean) {
        val app = application
        val textScale = textScale
        app.osmandMap?.let {
            if (this._textScale != textScale || forceUpdate) {
                this._textScale = textScale
                recreateBitmaps()
                mTargetPoint?.let { targetPoint ->
                    pointSizePx = sqrt(
                        (targetPoint.width * targetPoint.width
                                + targetPoint.height * targetPoint.height).toDouble()
                    )
                }
            }
        }
    }

    private fun updateTextSize() {
        application.osmandMap?.let {
            mTextPaint.textSize = (18f * Resources.getSystem().displayMetrics.scaledDensity)
        }
    }

    private fun recreateBitmaps() {
        mStartPoint = getVectorDrawableBitmap(com.mudita.map.common.R.drawable.map_start_point)
        mTargetPoint = getVectorDrawableBitmap(com.mudita.map.common.R.drawable.map_target_point)
        mIntermediatePoint = getVectorDrawableBitmap(com.mudita.map.common.R.drawable.map_intermediate_point)
        clearMapMarkersCollections()
    }

    private fun getVectorDrawableBitmap(@DrawableRes drawableId: Int): Bitmap? =
        AppCompatResources.getDrawable(context, drawableId)?.toBitmap()

    override fun getScaledBitmap(drawableId: Int): Bitmap? {
        return getScaledBitmap(drawableId, _textScale)
    }

    private fun getPointX(tileBox: RotatedTileBox, point: TargetPoint): Float {
        return if (contextMenuLayer?.moveableObject != null
            && point === contextMenuLayer?.moveableObject
        ) {
            val layer = contextMenuLayer as ContextMenuLayer
            layer.getMovableCenterPoint(tileBox).x
        } else {
            tileBox.getPixXFromLonNoRot(point.longitude).toFloat()
        }
    }

    private fun getPointY(tileBox: RotatedTileBox, point: TargetPoint): Float {
        return if (contextMenuLayer?.moveableObject != null
            && point === contextMenuLayer?.moveableObject
        ) {
            val layer = contextMenuLayer as ContextMenuLayer
            layer.getMovableCenterPoint(tileBox).y
        } else {
            tileBox.getPixYFromLatNoRot(point.latitude).toFloat()
        }
    }

    fun isLocationVisible(tb: RotatedTileBox?, p: TargetPoint?): Boolean {
        if (contextMenuLayer?.moveableObject != null
            && p === contextMenuLayer?.moveableObject
        ) {
            return true
        } else if (p == null || tb == null) {
            return false
        }
        val tx = tb.getPixXFromLatLon(p.latitude, p.longitude).toDouble()
        val ty = tb.getPixYFromLatLon(p.latitude, p.longitude).toDouble()
        return tx >= -pointSizePx && tx <= tb.pixWidth + pointSizePx && ty >= -pointSizePx && ty <= tb.pixHeight + pointSizePx
    }

    override fun drawInScreenPixels(): Boolean {
        return false
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

    override fun collectObjectsFromPoint(
        point: PointF,
        tileBox: RotatedTileBox,
        o: MutableList<Any>,
        unknownLocation: Boolean
    ) {
        if (tileBox.zoom >= 3) {
            val tg = application.targetPointsHelper
            tg?.let {
                val intermediatePoints = tg.allPoints
                val r = tileBox.defaultRadiusPoi
                for (i in intermediatePoints.indices) {
                    val tp = intermediatePoints[i]
                    val latLon = tp.point
                    if (latLon != null) {
                        val ex = point.x.toInt()
                        val ey = point.y.toInt()
                        val pixel = NativeUtilities.getPixelFromLatLon(
                            mapRenderer, tileBox, latLon.latitude, latLon.longitude
                        )
                        if (calculateBelongs(ex, ey, pixel.x.toInt(), pixel.y.toInt(), r)) {
                            o.add(tp)
                        }
                    }
                }
            }
        }
    }

    private fun calculateBelongs(ex: Int, ey: Int, objx: Int, objy: Int, radius: Int): Boolean {
        return abs(objx - ex) <= radius && ey - objy <= radius && objy - ey <= 2.5 * radius
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        return if (o is TargetPoint) {
            o.point
        } else null
    }

    override fun getObjectName(o: Any?): PointDescription? {
        return if (o is TargetPoint) {
            o.getPointDescription(context)
        } else null
    }

    override fun isObjectMovable(o: Any?): Boolean {
        if (o is TargetPoint) {
            val targetPointsHelper = application.targetPointsHelper ?: return false
            return targetPointsHelper.allPoints.contains(o)
        }
        return false
    }

    override fun applyNewObjectPosition(
        o: Any?,
        position: LatLon,
        callback: ApplyMovedObjectCallback?
    ) {
        var result = false
        var newTargetPoint: TargetPoint? = null
        if (o is TargetPoint) {
            val targetPointsHelper = application.targetPointsHelper
            targetPointsHelper?.let {
                if (o.start) {
                    targetPointsHelper.setStartPoint(position, true, null)
                    newTargetPoint = targetPointsHelper.pointToStart
                } else if (o === targetPointsHelper.pointToNavigate) {
                    targetPointsHelper.navigateToPoint(position, true, -1, null)
                    newTargetPoint = targetPointsHelper.pointToNavigate
                } else if (o.intermediate) {
                    val points = targetPointsHelper.intermediatePointsWithTarget
                    val i = points.indexOf(o)
                    if (i != -1) {
                        newTargetPoint = TargetPoint(
                            position,
                            PointDescription(PointDescription.POINT_TYPE_LOCATION, "")
                        )
                        points[i] = newTargetPoint
                        targetPointsHelper.reorderAllTargetPoints(points, true)
                    }
                }
                result = true
            }
        }
        callback?.onApplyMovedObject(result, newTargetPoint ?: o)
        applyMovableObject(position)
    }

    /** OpenGL  */
    private fun drawMarker(
        markersCollection: MapMarkersCollection,
        bitmap: Bitmap, position: PointI, caption: String?
    ) {
        if (!mapView.hasMapRenderer()) {
            return
        }
        val mapMarkerBuilder = MapMarkerBuilder()
        mapMarkerBuilder
            .setPosition(position)
            .setIsHidden(false)
            .setBaseOrder(getPointsOrder())
            .setPinIcon(NativeUtilities.createSkImageFromBitmap(bitmap))
            .setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top).pinIconHorisontalAlignment =
            MapMarker.PinIconHorisontalAlignment.Right
        if (caption != null) {
            initCaptionStyle()
            mIntermediatePoint?.let { intermediatePoint ->
                captionStyle?.let {
                    mapMarkerBuilder
                        .setCaptionStyle(captionStyle)
                        .setCaptionTopSpace(-intermediatePoint.height * 0.7 - it.size / 2).caption =
                        caption
                }
            }
        }
        mapMarkerBuilder.buildAndAddToCollection(markersCollection)
    }

    /** OpenGL  */
    private fun initCaptionStyle() {
        if (!mapView.hasMapRenderer() || captionStyle != null) {
            return
        }
        val captionColor = context.resources.getColor(
            if (nightMode) R.color.widgettext_night else R.color.widgettext_day, null
        )
        captionStyle = TextRasterizer.Style()
        captionStyle?.apply {
            size = mTextPaint.textSize
            wrapWidth = 20
            maxLines = 3
            bold = false
            italic = false
            color = NativeUtilities.createColorARGB(captionColor)
        }
    }

    private fun drawStartPoint(canvas: Canvas, tb: RotatedTileBox, pointToStart: TargetPoint) {
        mStartPoint?.let {
            val marginX = -it.width * START_POINT_MARGIN_WIDTH_FRACTION
            val marginY = -it.height * START_POINT_MARGIN_HEIGHT_FRACTION
            val locationX = getPointX(tb, pointToStart)
            val locationY = getPointY(tb, pointToStart)
            canvas.rotate(-tb.rotate, locationX, locationY)
            canvas.drawBitmap(it, locationX + marginX, locationY + marginY, mBitmapPaint)
            canvas.rotate(tb.rotate, locationX, locationY)
        }
    }

    private fun drawIntermediatePoint(
        canvas: Canvas,
        tb: RotatedTileBox,
        ip: TargetPoint,
        index: Int
    ) {
        mIntermediatePoint?.let {
            var marginX = it.width * FLAG_MARGIN_WIDTH_FRACTION
            val marginY = it.height * FLAG_MARGIN_HEIGHT_FRACTION
            val locationX = getPointX(tb, ip)
            val locationY = getPointY(tb, ip)
            canvas.rotate(-tb.rotate, locationX, locationY)
            canvas.drawBitmap(
                it,
                locationX + marginX,
                locationY + marginY,
                mBitmapPaint
            )
            marginX = it.width / 3f
            canvas.drawText(
                index.toString() + "", locationX - marginX, locationY - 3 * -marginY / 5f,
                mTextPaint
            )
            canvas.rotate(tb.rotate, locationX, locationY)
        }
    }

    private fun drawPointToNavigate(
        canvas: Canvas,
        tb: RotatedTileBox,
        pointToNavigate: TargetPoint
    ) {
        mTargetPoint?.let {
            val marginX = it.width * FLAG_MARGIN_WIDTH_FRACTION
            val marginY = it.height * FLAG_MARGIN_HEIGHT_FRACTION
            val locationX = getPointX(tb, pointToNavigate)
            val locationY = getPointY(tb, pointToNavigate)
            canvas.rotate(-tb.rotate, locationX, locationY)
            canvas.drawBitmap(it, locationX + marginX, locationY + marginY, mBitmapPaint)
            canvas.rotate(tb.rotate, locationX, locationY)
        }
    }
}