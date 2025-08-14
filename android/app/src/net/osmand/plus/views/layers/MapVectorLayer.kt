package net.osmand.plus.views.layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import net.osmand.core.android.MapRendererContext
import net.osmand.core.jni.MapLayerConfiguration
import net.osmand.core.jni.PointI
import net.osmand.core.jni.SymbolSubsectionConfiguration
import net.osmand.data.RotatedTileBox
import net.osmand.plus.resources.AsyncLoadingThread
import net.osmand.plus.resources.ResourceManager
import net.osmand.plus.utils.BooleanUtils.toNotNull
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.corenative.NativeCoreContext
import net.osmand.plus.views.layers.base.BaseMapLayer

class MapVectorLayer(context: Context) : BaseMapLayer(context) {
    private val resourceManager: ResourceManager? = application.resourceManager
    private var paintImg: Paint? = null
    private val destImage = RectF()
    private var visible = false
    private var cachedVisible = true
    private var cachedAlpha = -1
    private var cachedLabelsVisible = false

    override fun destroyLayer() {
        super.destroyLayer()
        resetLayerProvider()
    }

    override fun drawInScreenPixels(): Boolean {
        return false
    }

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        paintImg = Paint()
        paintImg!!.isFilterBitmap = true
        paintImg!!.alpha = alpha
        cachedLabelsVisible = view.settings?.KEEP_MAP_LABELS_VISIBLE?.get() ?: false
    }

    val isVectorDataVisible: Boolean
        get() = visible && view != null && view.zoom >= (view.settings?.LEVEL_TO_SWITCH_VECTOR_RASTER?.get() ?: 1)

    fun isVisible(): Boolean {
        return visible
    }

    fun setVisible(visible: Boolean) {
        this.visible = visible
        if (!visible) {
            resourceManager?.renderer?.clearCache()
        }
    }

    override fun onDraw(canvas: Canvas, tilesRect: RotatedTileBox, drawSettings: DrawSettings) {}
    private fun recreateLayerProvider() {
        val mapContext = NativeCoreContext.getMapRendererContext()
        mapContext?.recreateRasterAndSymbolsProvider()
    }

    private fun resetLayerProvider() {
        val mapContext = NativeCoreContext.getMapRendererContext()
        mapContext?.resetRasterAndSymbolsProvider()
    }

    private fun updateLayerProviderAlpha(alpha: Int) {
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            val mapLayerConfiguration = MapLayerConfiguration()
            mapLayerConfiguration.opacityFactor = alpha.toFloat() / 255.0f
            mapRenderer.setMapLayerConfiguration(
                MapRendererContext.OBF_RASTER_LAYER,
                mapLayerConfiguration
            )
            var keepLabels = false
            if (view != null) keepLabels = view.settings?.KEEP_MAP_LABELS_VISIBLE?.get().toNotNull()
            val symbolSubsectionConfiguration = SymbolSubsectionConfiguration()
            symbolSubsectionConfiguration.opacityFactor =
                if (keepLabels) 1.0f else alpha.toFloat() / 255.0f
            mapRenderer.setSymbolSubsectionConfiguration(
                MapRendererContext.OBF_SYMBOL_SECTION,
                symbolSubsectionConfiguration
            )
        }
    }

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tilesRect: RotatedTileBox,
        drawSettings: DrawSettings
    ) {
        super.onPrepareBufferImage(canvas, tilesRect, drawSettings)
        if (view == null) {
            return
        }
        val visible = isVisible()
        val visibleChanged = cachedVisible != visible
        cachedVisible = visible
        val alpha = alpha
        val alphaChanged = cachedAlpha != alpha
        cachedAlpha = alpha
        val labelsVisible = view.settings?.KEEP_MAP_LABELS_VISIBLE?.get().toNotNull()
        val labelsVisibleChanged = cachedLabelsVisible != labelsVisible
        cachedLabelsVisible = labelsVisible
        val mapRenderer = mapRenderer
        if (mapRenderer != null) {
            // opengl renderer
            if (visibleChanged) {
                if (visible) {
                    recreateLayerProvider()
                } else {
                    resetLayerProvider()
                }
            }
            if (visible) {
                NativeCoreContext.getMapRendererContext()!!.setNightMode(drawSettings.isNightMode)
            }
            if ((alphaChanged || visibleChanged || labelsVisibleChanged) && visible) {
                updateLayerProviderAlpha(alpha)
            }
            if (mapActivityInvalidated) {
                mapRenderer.target = PointI(tilesRect.center31X, tilesRect.center31Y)
                mapRenderer.azimuth = -tilesRect.rotate
                mapRenderer.zoom = (tilesRect.zoom + tilesRect.zoomAnimation + tilesRect
                    .zoomFloatPart).toFloat()
                val zoomMagnifier = mapDensity
                mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f)
            }
            mapActivityInvalidated = false
        } else if (visible) {
            if (!view.isZooming) {
                if (resourceManager!!.updateRenderedMapNeeded(tilesRect, drawSettings)) {
                    val copy = tilesRect.copy()
                    copy.increasePixelDimensions(copy.pixWidth / 3, copy.pixHeight / 4)
                    resourceManager.updateRendererMap(
                        copy,
                        AsyncLoadingThread.OnMapLoadedListener { interrupted ->
                            if (!interrupted) view.refreshMap()
                        },
                        false,
                    )
                }
            }
            resourceManager?.renderer?.let { renderer ->
                drawRenderedMap(canvas, renderer.bitmap, renderer.bitmapLocation, tilesRect)
                drawRenderedMap(canvas, renderer.prevBitmap, renderer.prevBmpLocation, tilesRect)
            }
        }
    }

    private fun drawRenderedMap(
        canvas: Canvas,
        bmp: Bitmap?,
        bmpLoc: RotatedTileBox?,
        currentViewport: RotatedTileBox
    ): Boolean {
        var shown = false
        if (bmp != null && bmpLoc != null) {
            val rot = -bmpLoc.rotate
            canvas.rotate(
                rot,
                currentViewport.centerPixelX.toFloat(),
                currentViewport.centerPixelY.toFloat()
            )
            val calc = currentViewport.copy()
            calc.rotate = bmpLoc.rotate
            val lt = bmpLoc.getLeftTopTile(bmpLoc.zoom.toDouble())
            val rb = bmpLoc.getRightBottomTile(bmpLoc.zoom.toFloat())
            val x1 = calc.getPixXFromTile(lt.x, lt.y, bmpLoc.zoom.toFloat())
            val x2 = calc.getPixXFromTile(rb.x, rb.y, bmpLoc.zoom.toFloat())
            val y1 = calc.getPixYFromTile(lt.x, lt.y, bmpLoc.zoom.toFloat())
            val y2 = calc.getPixYFromTile(rb.x, rb.y, bmpLoc.zoom.toFloat())

            destImage[x1, y1, x2] = y2
            if (!bmp.isRecycled) {
                canvas.drawBitmap(bmp, null, destImage, paintImg)
                shown = true
            }
            canvas.rotate(
                -rot,
                currentViewport.centerPixelX.toFloat(),
                currentViewport.centerPixelY.toFloat()
            )
        }
        return shown
    }

    override fun setAlpha(alpha: Int) {
        super.setAlpha(alpha)
        paintImg?.alpha = alpha
    }

    override fun onLongPressEvent(point: PointF, tileBox: RotatedTileBox): Boolean {
        return false
    }

    override fun onSingleTap(point: PointF, tileBox: RotatedTileBox): Boolean {
        return false
    }
}