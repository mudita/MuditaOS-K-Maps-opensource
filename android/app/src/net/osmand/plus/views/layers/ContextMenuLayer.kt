package net.osmand.plus.views.layers

import com.mudita.map.common.R as commonR
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.mudita.maps.R
import gnu.trove.list.array.TIntArrayList
import net.osmand.CallbackWithObject
import net.osmand.NativeLibrary.RenderedObject
import net.osmand.core.jni.MapMarker
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.QVectorPointI
import net.osmand.core.jni.VectorLineBuilder
import net.osmand.core.jni.VectorLinesCollection
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer

open class ContextMenuLayer(context: Context) : OsmandMapLayer(context) {
    private var selectOnMap: CallbackWithObject<LatLon>? = null
    private lateinit var contextMarker: ImageView
    private lateinit var paint: Paint
    private lateinit var outlinePaint: Paint
    private var movementListener: GestureDetector? = null

    var onLongPressCallback: (LatLon) -> Boolean = { true }
    var onSinglePressCallback: (LatLon, Amenity?) -> Unit = { _, _ -> }
    var shouldSearchAmenities: (() -> Boolean)? = null

    var cancelApplyingNewMarkerPosition = false
        private set
    private var applyingMarkerLatLon: LatLon? = null
    var isInGpxDetailsMode = false
        private set
    var isInAddGpxPointMode = false
        private set

    // OpenGl
    private var outlineCollection: VectorLinesCollection? = null
    private var contextMarkerCollection: MapMarkersCollection? = null
    private var contextCoreMarker: MapMarker? = null
    private var contextMarkerImage: Bitmap? = null
    var selectedObject: Any? = null
    private var selectedObjectCached: Any? = null

    override fun setMapActivity(mapActivity: MapActivity?) {
        super.setMapActivity(mapActivity)
        if (mapActivity != null) {
            movementListener = GestureDetector(mapActivity, MenuLayerOnGestureListener())
        } else {
            movementListener = null
        }
    }

    override fun destroyLayer() {
        super.destroyLayer()
        clearContextMarkerCollection()
        clearOutlineCollection()
    }

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        val context = context
        contextMarker = ImageView(context)
        contextMarker.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        val markerDrawable =
            AppCompatResources.getDrawable(context, commonR.drawable.ic_location_white)
        contextMarker.setImageDrawable(markerDrawable)
        contextMarker.isClickable = true
        paint = Paint()
        paint.color = 0x7f000000
        outlinePaint = Paint()
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
        outlinePaint.strokeWidth = AndroidUtils.dpToPx(getContext(), 2f).toFloat()
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.color = getColor(R.color.osmand_orange)
    }

    override fun onDraw(canvas: Canvas, box: RotatedTileBox, nightMode: DrawSettings) {
        val mapActivity = mapActivity ?: return

        val mapRenderer = mapRenderer
        val hasMapRenderer = mapRenderer != null
        if (contextMarkerCollection == null || mapActivityInvalidated) {
            recreateContextMarkerCollection()
        }
        var clearSelectedObject = true
        if (selectedObject != null) {
            var x: TIntArrayList? = null
            var y: TIntArrayList? = null
            when (selectedObject) {
                is Amenity -> {
                    val a = selectedObject as Amenity
                    x = a.x
                    y = a.y
                }
                is RenderedObject -> {
                    val r = selectedObject as RenderedObject
                    x = r.x
                    y = r.y
                }
            }
            if (x != null && y != null && x.size() > 2) {
                if (hasMapRenderer) {
                    clearSelectedObject = false
                    if (selectedObject !== selectedObjectCached) {
                        clearOutlineCollection()
                        val outlineCollection = VectorLinesCollection()
                        val points = QVectorPointI()
                        for (i in 0 until x.size()) {
                            points.add(PointI(x[i], y[i]))
                        }
                        val builder = VectorLineBuilder()
                        builder.setPoints(points)
                            .setIsHidden(false)
                            .setLineId(1)
                            .setLineWidth((outlinePaint.strokeWidth * GeometryWayDrawer.VECTOR_LINE_SCALE_COEF).toDouble())
                            .setFillColor(NativeUtilities.createFColorARGB(outlinePaint.color))
                            .setApproximationEnabled(false).baseOrder = baseOrder
                        builder.buildAndAddToCollection(outlineCollection)
                        this.outlineCollection = outlineCollection
                        mapRenderer?.addSymbolsProvider(outlineCollection)
                    }
                } else {
                    var px: Float
                    var py: Float
                    var prevX: Float
                    var prevY: Float
                    prevX = box.getPixXFrom31(x[0], y[0])
                    prevY = box.getPixYFrom31(x[0], y[0])
                    for (i in 1 until x.size()) {
                        px = box.getPixXFrom31(x[i], y[i])
                        py = box.getPixYFrom31(x[i], y[i])
                        canvas.drawLine(prevX, prevY, px, py, outlinePaint)
                        prevX = px
                        prevY = py
                    }
                }
            }
        }
        selectedObjectCached = selectedObject
        if (clearSelectedObject && hasMapRenderer) {
            clearOutlineCollection()
        }
        if (isInAddGpxPointMode) {
            canvas.translate(
                box.pixWidth / 2f - contextMarker.width / 2f,
                box.pixHeight / 2f - contextMarker.height
            )
            contextMarker.draw(canvas)
        }
        if (hasMapRenderer) {
            contextCoreMarker?.setIsHidden(true)
        }
        mapActivityInvalidated = false
    }

    fun setSelectOnMap(selectOnMap: CallbackWithObject<LatLon>?) {
        this.selectOnMap = selectOnMap
    }

    private fun recreateContextMarkerCollection() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            clearContextMarkerCollection()
            if (contextMarkerImage == null) {
                return
            }
            contextMarkerCollection = MapMarkersCollection()
            val builder = MapMarkerBuilder()
            builder.baseOrder = getPointsOrder() - 100
            builder.setIsAccuracyCircleSupported(false)
            builder.setIsHidden(true)
            contextMarkerImage?.let {
                builder.pinIcon = NativeUtilities.createSkImageFromBitmap(it)
            }
            builder.pinIconVerticalAlignment = MapMarker.PinIconVerticalAlignment.Top
            contextCoreMarker = builder.buildAndAddToCollection(contextMarkerCollection)
            mapRenderer.addSymbolsProvider(contextMarkerCollection)
        }
    }

    private fun clearContextMarkerCollection() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && contextMarkerCollection != null) {
            mapRenderer.removeSymbolsProvider(contextMarkerCollection)
            contextMarkerCollection = null
        }
    }

    private fun clearOutlineCollection() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && outlineCollection != null) {
            mapRenderer.removeSymbolsProvider(outlineCollection)
            outlineCollection = null
        }
    }

    override fun onLongPressEvent(point: PointF, tileBox: RotatedTileBox): Boolean {
        if (disableLongPressOnMap(point, tileBox)) {
            return false
        }

        val pointLatLon = NativeUtilities.getLatLonFromPixel(mapRenderer, tileBox, point.x, point.y)
        onLongPressCallback(pointLatLon)
        view.refreshMap()
        return true
    }

    fun getMovableCenterPoint(tb: RotatedTileBox): PointF {
        return applyingMarkerLatLon?.let {
            return@let NativeUtilities.getPixelFromLatLon(
                mapRenderer, tb,
                it.latitude, it.longitude
            )
        } ?: kotlin.run {
            PointF(tb.pixWidth / 2f, tb.pixHeight / 2f)
        }
    }

    val moveableObject: Any?
        get() = null

    fun applyNewMarkerPosition() {
        mapView.currentRotatedTileBox?.let { tileBox ->
            val newMarkerPosition = getMovableCenterPoint(tileBox)
            val ll = NativeUtilities.getLatLonFromPixel(
                mapRenderer, tileBox,
                newMarkerPosition.x, newMarkerPosition.y
            )
            applyingMarkerLatLon = ll
            val obj = moveableObject
            cancelApplyingNewMarkerPosition = false
        }

    }

    fun showContextMenu(latLon: LatLon): Boolean {
        if (isInAddGpxPointMode) {
            view.animatedDraggingThread?.startMoving(
                latLon.latitude,
                latLon.longitude,
                view.zoom,
                true
            )
        }
        return true
    }

    fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean {
        val mapActivity = mapActivity

        if (isInGpxDetailsMode || isInAddGpxPointMode || mapActivity == null) {
            return true
        }
        var res = false
        for (lt in view.getLayers()) {
            if (lt is IContextMenuProvider) {
                if ((lt as IContextMenuProvider).disableLongPressOnMap(point, tileBox)) {
                    res = true
                    break
                }
            }
        }
        return res
    }

    override fun drawInScreenPixels(): Boolean = true

    override fun onSingleTap(point: PointF, tileBox: RotatedTileBox): Boolean {
        val mapActivity = mapActivity
        val latlon = NativeUtilities.getLatLonFromPixel(mapRenderer, tileBox, point.x, point.y)
        if (mapActivity == null || isInGpxDetailsMode) {
            return true
        }
        if (selectOnMap != null) {
            val cb: CallbackWithObject<LatLon> = selectOnMap as CallbackWithObject<LatLon>
            cb.processResult(latlon)
            selectOnMap = null
            return true
        }
        val amenity = if (shouldSearchAmenities?.invoke() == true) {
            MapSelectionHelper.findClosestAmenity(
                checkNotNull(application.resourceManager),
                latlon,
                tileBox,
            )
        } else {
            null
        }
        onSinglePressCallback(latlon, amenity)
        return false
    }

    interface IContextMenuProvider {
        fun collectObjectsFromPoint(
            point: PointF,
            tileBox: RotatedTileBox,
            o: MutableList<Any>,
            unknownLocation: Boolean
        )

        fun getObjectLocation(o: Any?): LatLon?
        fun getObjectName(o: Any?): PointDescription?
        fun disableSingleTap(): Boolean
        fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean
        fun isObjectClickable(o: Any): Boolean
        fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean
    }

    interface IMoveObjectProvider {
        fun isObjectMovable(o: Any?): Boolean
        fun applyNewObjectPosition(
            o: Any?,
            position: LatLon,
            callback: ApplyMovedObjectCallback?
        )
    }

    interface ApplyMovedObjectCallback {
        fun onApplyMovedObject(success: Boolean, newObject: Any?)
        val isCancelled: Boolean
    }

    interface IContextMenuProviderSelection {
        fun getOrder(o: Any?): Int
        fun setSelectedObject(o: Any?)
        fun clearSelectedObject()
    }

    private class MenuLayerOnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean = true

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean = true

    }
}