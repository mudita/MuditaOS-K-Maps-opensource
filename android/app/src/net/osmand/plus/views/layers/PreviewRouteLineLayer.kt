package net.osmand.plus.views.layers

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import java.util.Collections
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.sqrt
import net.osmand.PlatformUtil
import net.osmand.data.RotatedTileBox
import net.osmand.plus.routing.ColoringType
import net.osmand.plus.routing.PreviewRouteLineInfo
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.layers.base.BaseRouteLayer
import net.osmand.plus.views.layers.geometry.GeometryWayStyle
import net.osmand.plus.views.layers.geometry.MultiColoringGeometryWay.GeometryGradientWayStyle
import net.osmand.plus.views.layers.geometry.RouteGeometryWay
import net.osmand.plus.views.layers.geometry.RouteGeometryWayContext
import net.osmand.render.RenderingRule
import net.osmand.render.RenderingRuleSearchRequest
import net.osmand.render.RenderingRuleStorageProperties
import net.osmand.render.RenderingRulesStorage
import net.osmand.router.RouteColorize
import net.osmand.router.RouteStatisticsHelper
import net.osmand.util.Algorithms

class PreviewRouteLineLayer(ctx: Context) : BaseRouteLayer(ctx) {
    private var previewIcon: LayerDrawable? = null
    private var previewWayContext: RouteGeometryWayContext? = null
    private var previewLineGeometry: RouteGeometryWay? = null

    override fun initGeometries(density: Float) {
        previewWayContext = RouteGeometryWayContext(context, density)
        previewWayContext?.disableMapRenderer()
        previewWayContext?.updatePaints(nightMode, attrs)
        previewLineGeometry = RouteGeometryWay(previewWayContext)
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) {
        if (previewRouteLineInfo != null) {
            updateRouteColoringType()
            updateAttrs(settings, tileBox)
            val angle = tileBox.rotate
            val c = tileBox.centerPixelPoint
            canvas.rotate(-angle, c.x, c.y)
            drawRouteLinePreview(canvas, tileBox, previewRouteLineInfo)
            canvas.rotate(angle, c.x, c.y)
        }
    }

    override fun updateAttrs(settings: DrawSettings?, tileBox: RotatedTileBox) {
        val updatePaints = attrs.updatePaints(view.application, settings, tileBox)
        attrs.isPaint2 = false
        attrs.isPaint3 = false
        nightMode = settings != null && settings.isNightMode
        if (updatePaints) {
            previewWayContext?.updatePaints(nightMode, attrs)
        }
    }

    override fun setPreviewRouteLineInfo(previewRouteLineInfo: PreviewRouteLineInfo?) {
        super.setPreviewRouteLineInfo(previewRouteLineInfo)
        if (previewRouteLineInfo == null) {
            previewIcon = null
        }
    }

    private fun drawRouteLinePreview(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        previewInfo: PreviewRouteLineInfo
    ) {
        val previewBounds = previewInfo.lineBounds ?: return
        val startX = previewBounds.left.toFloat()
        val startY = previewBounds.bottom.toFloat()
        val endX = previewBounds.right.toFloat()
        val endY = previewBounds.top.toFloat()
        val centerX = previewInfo.centerX.toFloat()
        val centerY = previewInfo.centerY.toFloat()
        val tx: MutableList<Float> = ArrayList()
        val ty: MutableList<Float> = ArrayList()
        val tx31: List<Int> = ArrayList()
        val ty31: List<Int> = ArrayList()
        tx.add(startX)
        tx.add(centerX)
        tx.add(centerX)
        tx.add(endX)
        ty.add(startY)
        ty.add(startY)
        ty.add(endY)
        ty.add(endY)
        val angles: MutableList<Double> = ArrayList()
        val distances: MutableList<Double> = ArrayList()
        val styles: MutableList<GeometryWayStyle<*>> = ArrayList()
        previewLineGeometry!!.setRouteStyleParams(
            getRouteLineColor(), routeLineWidth,
            directionArrowsColor, routeColoringType, routeInfoAttribute
        )
        fillPreviewLineArrays(tx, ty, angles, distances, styles)
        canvas.rotate(
            +tileBox.rotate,
            tileBox.centerPixelX.toFloat(),
            tileBox.centerPixelY.toFloat()
        )
        previewLineGeometry?.drawRouteSegment(
            tileBox,
            canvas,
            null,
            tx,
            ty,
            tx31,
            ty31,
            angles,
            distances,
            0.0,
            styles
        )
        canvas.rotate(
            -tileBox.rotate,
            tileBox.centerPixelX.toFloat(),
            tileBox.centerPixelY.toFloat()
        )
        if (previewRouteLineInfo.shouldShowTurnArrows()) {
            val path = Path()
            val matrix = Matrix()
            val lineLength = AndroidUtils.dpToPx(context, 24f)
            val offset = if (AndroidUtils.isLayoutRtl(context)) lineLength else -lineLength
            val attrsTurnArrowColor = attrs.paint3.color
            if (customTurnArrowColor != 0) {
                attrs.paint3.color = customTurnArrowColor
            }
            val routeWidth = previewLineGeometry?.defaultWayStyle?.getWidth(0) ?: 0f
            if (routeWidth != 0f) {
                attrs.paint3.strokeWidth = routeWidth / 2
                //attrs.paint3.setStrokeWidth(Math.min(previewLineGeometry.getContext().getAttrs().defaultWidth3, routeWidth / 2));
            }
            path.moveTo(centerX + offset, startY)
            path.lineTo(centerX, startY)
            path.lineTo(centerX, startY - lineLength)
            drawTurnArrow(canvas, matrix, centerX, startY - lineLength, centerX, startY)
            canvas.drawPath(path, attrs.paint3)
            path.reset()
            path.moveTo(centerX, endY + lineLength)
            path.lineTo(centerX, endY)
            path.lineTo(centerX - offset, endY)
            canvas.drawPath(path, attrs.paint3)
            drawTurnArrow(canvas, matrix, centerX - offset, endY, centerX, endY)
            attrs.paint3.color = attrsTurnArrowColor
        }
        if (previewIcon == null) {
            previewIcon =
                AppCompatResources.getDrawable(context, previewInfo.iconId) as LayerDrawable?
            if (previewIcon != null) {
                DrawableCompat.setTint(previewIcon!!.getDrawable(1), previewInfo.iconColor)
            }
        }
        canvas.rotate(-90f, centerX, centerY)
        drawIcon(canvas, previewIcon, centerX.toInt(), centerY.toInt())
        canvas.rotate(90f, centerX, centerY)
    }

    private fun fillPreviewLineArrays(
        tx: MutableList<Float>, ty: MutableList<Float>, angles: MutableList<Double>,
        distances: MutableList<Double>, styles: MutableList<GeometryWayStyle<*>>
    ) {
        fillDistancesAngles(tx, ty, angles, distances)
        if (routeColoringType.isSolidSingleColor) {
            for (i in tx.indices) {
                previewLineGeometry?.defaultWayStyle?.let {
                    styles.add(it)
                }
            }
        } else if (routeColoringType == ColoringType.ALTITUDE) {
            fillAltitudeGradientArrays(distances, styles)
        } else if (routeColoringType == ColoringType.SLOPE) {
            fillSlopeGradientArrays(tx, ty, angles, distances, styles)
        } else if (routeColoringType.isRouteInfoAttribute) {
            val success = fillRouteInfoAttributeArrays(tx, ty, angles, distances, styles)
            if (!success) {
                fillSolidSingeColorArrays(tx, styles)
            }
        }
    }

    private fun fillSolidSingeColorArrays(
        tx: List<Float>,
        styles: MutableList<GeometryWayStyle<*>>
    ) {
        for (i in tx.indices) {
            previewLineGeometry?.defaultWayStyle?.let {
                styles.add(it)
            }
        }
    }

    private fun fillAltitudeGradientArrays(
        distances: List<Double>,
        styles: MutableList<GeometryWayStyle<*>>
    ) {
        val colors = RouteColorize.COLORS
        for (i in 1 until distances.size) {
            previewLineGeometry?.gradientWayStyle?.let { style ->
                styles.add(style)
                val prevDist = distances[i - 1]
                val currDist = distances[i]
                val nextDist: Double = if (i + 1 == distances.size) 0.0 else distances[i + 1]
                style.currColor =
                    getPreviewColor(colors, i - 1, (prevDist + currDist / 2) / (prevDist + currDist))
                style.nextColor =
                    getPreviewColor(colors, i, (currDist + nextDist / 2) / (currDist + nextDist))
            }
        }
        styles.add(styles[styles.size - 1])
    }

    private fun fillSlopeGradientArrays(
        tx: MutableList<Float>, ty: MutableList<Float>, angles: MutableList<Double>,
        distances: MutableList<Double>, styles: MutableList<GeometryWayStyle<*>>
    ) {
        val palette: MutableList<Int> = ArrayList()
        for (color in RouteColorize.SLOPE_COLORS) {
            palette.add(color)
        }
        val gradientLengthsRatio = listOf(0.145833, 0.130209, 0.291031)
        val colors: MutableList<Int> = ArrayList()
        fillMultiColorLineArrays(palette, gradientLengthsRatio, tx, ty, angles, distances, colors)
        for (i in 1 until tx.size) {
            previewLineGeometry?.gradientWayStyle?.let { style ->
                styles.add(style)
                val currDist = distances[i]
                val nextDist: Double = if (i + 1 == distances.size) 0.0 else distances[i + 1]
                style.currColor =
                    if (i == 1) colors[0] else (styles[i - 2] as GeometryGradientWayStyle<*>).nextColor
                if (colors[i] != 0) {
                    style.nextColor = colors[i]
                } else {
                    val coeff = currDist / (currDist + nextDist)
                    style.nextColor =
                        RouteColorize.getIntermediateColor(colors[i - 1], colors[i + 1], coeff)
                }
            }
        }
        styles.add(styles[styles.size - 1])
    }

    private fun fillRouteInfoAttributeArrays(
        tx: MutableList<Float>, ty: MutableList<Float>, angles: MutableList<Double>,
        distances: MutableList<Double>, styles: MutableList<GeometryWayStyle<*>>
    ): Boolean {
        val palette = fetchColorsOfRouteInfoAttribute()
        if (Algorithms.isEmpty(palette)) {
            return false
        }
        val ratiosAmount = palette.size - 1
        val lengthRatio = 1.0 / palette.size
        val attributesLengthsRatio: List<Double> =
            ArrayList(Collections.nCopies(ratiosAmount, lengthRatio))
        val colors: MutableList<Int> = ArrayList()
        fillMultiColorLineArrays(palette, attributesLengthsRatio, tx, ty, angles, distances, colors)
        for (i in 0 until tx.size - 1) {
            previewLineGeometry?.getSolidWayStyle(colors[i])?.let { style ->
                styles.add(style)
            }
        }
        styles.add(styles[styles.size - 1])
        return true
    }

    private fun fillMultiColorLineArrays(
        palette: List<Int>, lengthRatios: List<Double>,
        tx: MutableList<Float>, ty: MutableList<Float>, angles: MutableList<Double>,
        distances: MutableList<Double>, colors: MutableList<Int>
    ) {
        var totalDist = 0.0
        for (d in distances) {
            totalDist += d
        }
        val rtl = AndroidUtils.isLayoutRtl(context)
        val srcTx: List<Float> = ArrayList(tx)
        val srcTy: List<Float> = ArrayList(ty)
        val colorsArray = IntArray(tx.size + lengthRatios.size)
        colorsArray[0] = palette[0]
        colorsArray[colorsArray.size - 1] = palette[palette.size - 1]
        var passedDist = 0.0
        for (i in lengthRatios.indices) {
            val ratio = lengthRatios[i]
            var length = passedDist + totalDist * ratio
            passedDist += totalDist * ratio
            var insertIdx = 1
            while (insertIdx < distances.size && length - distances[insertIdx] > 0) {
                length -= distances[insertIdx]
                insertIdx++
            }
            val px = srcTx[insertIdx - 1]
            val py = srcTy[insertIdx - 1]
            val nx = srcTx[insertIdx]
            val ny = srcTy[insertIdx]
            val r = (length / distances[insertIdx]).toFloat()
            val x =
                ceil((if (rtl) px - (px - nx) * r else px + (nx - px) * r.toDouble()) as Double).toFloat()
            val y = ceil((py + (ny - py) * r).toDouble()).toFloat()
            val idx = findNextPrevPointIdx(x, y, tx, ty)
            tx.add(idx, x)
            ty.add(idx, y)
            val color = palette[i + 1]
            colorsArray[idx] = color
        }
        for (colorIdx in 1 until colorsArray.size) {
            if (colorsArray[colorIdx] == 0) {
                colorsArray[colorIdx] = colorsArray[colorIdx - 1]
            }
        }
        distances.clear()
        angles.clear()
        fillDistancesAngles(tx, ty, angles, distances)
        for (color in colorsArray) {
            colors.add(color)
        }
    }

    private fun fetchColorsOfRouteInfoAttribute(): List<Int> {
        validRenderer?.let { renderer ->
            val request = RenderingRuleSearchRequest(renderer)
            applyProfileFiltersToRequest(request, renderer)
            return if (request.searchRenderingAttribute(routeInfoAttribute)) {
                fetchColorsInternal(renderer, request)
            } else emptyList()
        } ?: return emptyList()
    }

    private val validRenderer: RenderingRulesStorage?
        get() {
            val app = view.application
            val currentRenderer = app?.rendererRegistry?.currentSelectedRenderer
            val defaultRenderer = app?.rendererRegistry?.defaultRender()
            if (currentRenderer == null) {
                return defaultRenderer
            }
            for (attributeName in currentRenderer.renderingAttributeNames) {
                if (attributeName.startsWith(RouteStatisticsHelper.ROUTE_INFO_PREFIX)) {
                    return currentRenderer
                }
            }
            return defaultRenderer
        }

    private fun applyProfileFiltersToRequest(
        request: RenderingRuleSearchRequest,
        renderer: RenderingRulesStorage
    ) {
        request.setBooleanFilter(renderer.PROPS.R_NIGHT_MODE, nightMode)
        val preferences = view.settings?.getProfilePreferences(appMode) as? SharedPreferences
        for (property in renderer.PROPS.customRules) {
            val preferenceKey = "nrenderer_" + property.attrName
            if (property.isString) {
                request.setStringFilter(property, preferences?.getString(preferenceKey, null))
            } else if (property.isBoolean) {
                request.setBooleanFilter(property, preferences?.getBoolean(preferenceKey, false) ?: false)
            } else if (property.isInt || property.isColor) {
                request.setIntFilter(property, preferences?.getInt(preferenceKey, 0) ?: 0)
            } else if (property.isFloat) {
                request.setFloatFilter(property, preferences?.getFloat(preferenceKey, 0f) ?: 0f)
            }
        }
    }

    private fun fetchColorsInternal(
        renderer: RenderingRulesStorage,
        request: RenderingRuleSearchRequest
    ): List<Int> {
        val renderingRules = renderer.getRenderingAttributeRule(routeInfoAttribute)
            .ifElseChildren
        if (Algorithms.isEmpty(renderingRules)) {
            return emptyList()
        }
        val colors: MutableList<Int> = ArrayList()
        for (rule in renderingRules) {
            setTagValueAdditional(renderer, request, rule)
            if (request.searchRenderingAttribute(routeInfoAttribute)) {
                val stringColor =
                    request.getColorStringPropertyValue(renderer.PROPS.R_ATTR_COLOR_VALUE)
                var color = 0
                try {
                    color = Algorithms.parseColor(stringColor)
                } catch (e: IllegalArgumentException) {
                    log.error(e)
                }
                if (color != 0 && !colors.contains(color)) {
                    colors.add(color)
                }
            }
        }
        return colors
    }

    private fun setTagValueAdditional(
        renderer: RenderingRulesStorage, request: RenderingRuleSearchRequest,
        rule: RenderingRule?
    ) {
        val properties = rule?.properties ?: return
        for (property in properties) {
            val attribute = property.attrName
            if (RenderingRuleStorageProperties.TAG == attribute) {
                request.setStringFilter(
                    renderer.PROPS.R_TAG,
                    rule.getStringPropertyValue(property.attrName)
                )
            } else if (RenderingRuleStorageProperties.VALUE == attribute) {
                request.setStringFilter(
                    renderer.PROPS.R_VALUE,
                    rule.getStringPropertyValue(property.attrName)
                )
            } else if (RenderingRuleStorageProperties.ADDITIONAL == attribute) {
                request.setStringFilter(
                    renderer.PROPS.R_ADDITIONAL,
                    rule.getStringPropertyValue(property.attrName)
                )
            }
        }
    }

    private fun fillDistancesAngles(
        tx: List<Float>, ty: List<Float>, angles: MutableList<Double>,
        distances: MutableList<Double>
    ) {
        angles.add(0.0)
        distances.add(0.0)
        for (i in 1 until tx.size) {
            val x = tx[i]
            val y = ty[i]
            val px = tx[i - 1]
            val py = ty[i - 1]
            val angleRad = atan2((y - py).toDouble(), (x - px).toDouble())
            val angle = angleRad * 180 / Math.PI + 90f
            angles.add(angle)
            val dist = sqrt(((y - py) * (y - py) + (x - px) * (x - px)).toDouble())
            distances.add(dist)
        }
    }

    private fun findNextPrevPointIdx(
        x: Float,
        y: Float,
        tx: List<Float>,
        ty: List<Float>,
    ): Int {
        for (i in tx.indices) {
            if (tx[i] >= x ) {
                if (ty[i] == y) {
                    return i
                } else if (ty[i] <= y) {
                    return i
                }
            }
        }
        return tx.size - 1
    }

    private fun getPreviewColor(colors: IntArray, index: Int, coeff: Double): Int {
        if (index == 0) {
            return colors[0]
        } else if (index > 0 && index < colors.size) {
            return RouteColorize.getIntermediateColor(colors[index - 1], colors[index], coeff)
        } else if (index == colors.size) {
            return colors[index - 1]
        }
        return 0
    }

    override fun drawInScreenPixels(): Boolean {
        return false
    }

    companion object {
        private val log = PlatformUtil.getLog(
            PreviewRouteLineLayer::class.java
        )
    }
}