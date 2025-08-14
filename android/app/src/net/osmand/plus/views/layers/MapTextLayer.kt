package net.osmand.plus.views.layers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import net.osmand.core.jni.TextRasterizer
import net.osmand.data.LatLon
import net.osmand.data.QuadRect
import net.osmand.data.QuadTree
import net.osmand.data.RotatedTileBox
import com.mudita.maps.R
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.util.Algorithms
import java.util.*

class MapTextLayer(ctx: Context) : OsmandMapLayer(ctx) {

    private var textObjects: MutableMap<OsmandMapLayer, Collection<Any?>> = linkedMapOf()
    private var paintTextIcon = Paint()

    interface MapTextProvider<T : Any?> {
        fun getTextLocation(o: T): LatLon?
        fun getTextShift(o: T, rb: RotatedTileBox): Int
        fun getText(o: T): String?
        fun isTextVisible(): Boolean
        fun isFakeBoldText(): Boolean
    }

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        updateTextSize()
        paintTextIcon.textAlign = Paint.Align.CENTER
        paintTextIcon.isAntiAlias = true
        val textObjectsLoc: MutableMap<OsmandMapLayer, Collection<*>> =
            TreeMap { lhs: OsmandMapLayer, rhs: OsmandMapLayer ->
                val v = this.view
                if (v != null) {
                    val z1 = v.getZorder(lhs)
                    val z2 = v.getZorder(rhs)
                    return@TreeMap z1.compareTo(z2)
                }
                0
            }
        textObjectsLoc.putAll(textObjects)
        textObjects = textObjectsLoc
    }

    fun putData(ml: OsmandMapLayer, objects: Collection<*>) {
        if (Algorithms.isEmpty(objects)) {
            textObjects.remove(ml)
        } else {
            if (ml is MapTextProvider<*>) {
                textObjects[ml] = objects
            } else {
                throw IllegalArgumentException()
            }
        }
    }

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        val intersections = initBoundIntersections(tileBox)
        updateTextSize()
        for ((l, value) in textObjects) {
            val provider = l as MapTextProvider<Any?>
            if (!view.isLayerExists(l) || !provider.isTextVisible()) {
                continue
            }
            paintTextIcon.isFakeBoldText = provider.isFakeBoldText()
            for (o in value) {
                val loc = provider.getTextLocation(o)
                val name = provider.getText(o)
                if (loc == null || TextUtils.isEmpty(name)) {
                    continue
                }
                val r = provider.getTextShift(o, tileBox)
                val x = tileBox.getPixXFromLatLon(loc.latitude, loc.longitude)
                val y = tileBox.getPixYFromLatLon(loc.latitude, loc.longitude)
                if (name != null) {
                    drawWrappedText(
                        canvas,
                        name,
                        intersections,
                        x,
                        y + r + 2 + paintTextIcon.textSize / 2,
                        settings.isNightMode
                    )
                }
            }
        }
    }

    private fun drawWrappedText(
        canvas: Canvas, text: String,
        intersections: QuadTree<QuadRect>, x: Float, y: Float,
        nightMode: Boolean
    ) {
        val textSize = paintTextIcon.textSize
        if (text.length > TEXT_WRAP) {
            var start = 0
            val end = text.length
            var lastSpace: Int
            var line = 0
            var pos = 0
            var limit = 0
            while (pos < end && line < TEXT_LINES) {
                lastSpace = -1
                limit += TEXT_WRAP
                while (pos < limit && pos < end) {
                    if (Character.isWhitespace(text[pos])) {
                        lastSpace = pos
                    }
                    pos++
                }
                if (lastSpace == -1 || pos == end) {
                    val subtext = text.substring(start, pos)
                    if (!drawShadowTextLine(
                            canvas,
                            subtext,
                            intersections,
                            x,
                            y,
                            line,
                            nightMode
                        )
                    ) {
                        break
                    }
                    start = pos
                } else {
                    var subtext = text.substring(start, lastSpace)
                    if (line + 1 == TEXT_LINES) {
                        subtext += ".."
                    }
                    if (!drawShadowTextLine(
                            canvas,
                            subtext,
                            intersections,
                            x,
                            y,
                            line,
                            nightMode
                        )
                    ) {
                        break
                    }
                    start = lastSpace + 1
                    limit += start - pos - 1
                }
                line++
            }
        } else if (!intersects(intersections, x, y, paintTextIcon.measureText(text), textSize)) {
            drawShadowText(canvas, text, x, y, nightMode)
        }
    }

    private fun drawShadowTextLine(
        canvas: Canvas, text: String,
        intersections: QuadTree<QuadRect>, x: Float, y: Float,
        line: Int, nightMode: Boolean
    ): Boolean {
        val textSize = paintTextIcon.textSize
        val centerY = y + line * (textSize + 2)
        if (!intersects(intersections, x, centerY, paintTextIcon.measureText(text), textSize)) {
            drawShadowText(canvas, text, x, centerY, nightMode)
            return true
        }
        return false
    }

    private fun drawShadowText(
        cv: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        nightMode: Boolean
    ) {
        paintTextIcon.style = Paint.Style.STROKE
        val ctx = context
        paintTextIcon.color = if (nightMode) ContextCompat.getColor(
            ctx,
            R.color.widgettext_shadow_night
        ) else ContextCompat.getColor(
            ctx,
            R.color.widgettext_shadow_day
        )
        paintTextIcon.strokeWidth = 2f
        cv.drawText(text, centerX, centerY, paintTextIcon)
        // reset
        paintTextIcon.strokeWidth = 2f
        paintTextIcon.style = Paint.Style.FILL
        paintTextIcon.color = if (nightMode) ContextCompat.getColor(
            ctx,
            R.color.widgettext_night
        ) else ContextCompat.getColor(ctx, R.color.widgettext_day)
        cv.drawText(text, centerX, centerY, paintTextIcon)
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) {}

    override fun drawInScreenPixels(): Boolean = true

    private fun updateTextSize() {
        val scale = textScale
        view.density?.let { density ->
            val textSize = scale * TEXT_SIZE * density
            if (paintTextIcon.textSize != textSize) {
                paintTextIcon.textSize = textSize
            }
        }
    }

    companion object {
        private const val TEXT_WRAP = 15
        private const val TEXT_LINES = 3
        private const val TEXT_SIZE = 13

        @JvmStatic
        fun getTextStyle(
            ctx: Context, nightMode: Boolean,
            textScale: Float, density: Float
        ): TextRasterizer.Style {
            val textStyle = TextRasterizer.Style()
            textStyle.wrapWidth = TEXT_WRAP.toLong()
            textStyle.maxLines = TEXT_LINES.toLong()
            textStyle.bold = false
            textStyle.italic = false
            textStyle.color = NativeUtilities.createColorARGB(
                ContextCompat.getColor(
                    ctx,
                    if (nightMode) R.color.widgettext_night else R.color.widgettext_day
                )
            )
            textStyle.size = textScale * TEXT_SIZE * density
            textStyle.haloColor = NativeUtilities.createColorARGB(
                ContextCompat.getColor(
                    ctx,
                    if (nightMode) R.color.widgettext_shadow_night else R.color.widgettext_shadow_day
                )
            )
            textStyle.haloRadius = 5
            return textStyle
        }
    }
}