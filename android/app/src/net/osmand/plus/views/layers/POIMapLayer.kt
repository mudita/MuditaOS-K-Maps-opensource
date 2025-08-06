package net.osmand.plus.views.layers

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.text.util.Linkify
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.mudita.maps.R
import java.util.TreeSet
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import net.osmand.PlatformUtil
import net.osmand.ResultMatcher
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox
import net.osmand.data.ValueHolder
import net.osmand.osm.MapPoiTypes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.helpers.ColorDialogs
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.render.TravelRendererHelper
import net.osmand.plus.render.TravelRendererHelper.OnFileVisibilityChangeListener
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.routing.RoutingHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.PointImageDrawable
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.core.POITileProvider
import net.osmand.plus.widgets.WebViewEx
import net.osmand.util.Algorithms
import org.apache.commons.logging.Log

class POIMapLayer(context: Context) : OsmandMapLayer(context), IContextMenuProvider,
    MapTextProvider<Amenity?>, IRouteInformationListener, OnFileVisibilityChangeListener {
    private val app: OsmandApplication = context.applicationContext as OsmandApplication
    private val routingHelper: RoutingHelper? = app.routingHelper
    private var filters: Set<PoiUIFilter> = TreeSet()
    private var mapTextLayer: MapTextLayer? = null
    private var poiTileProvider: POITileProvider? = null
    private val travelRendererHelper: TravelRendererHelper? = app.travelRendererHelper
    private var showTravel: Boolean?
    private var routeArticleFilterEnabled: Boolean?
    private var routeArticlePointsFilterEnabled: Boolean?
    private var routeTrackFilterEnabled = false
    private var routeTrackAsPoiFilterEnabled = false
    private var routeArticleFilter: PoiUIFilter?
    private var routeArticlePointsFilter: PoiUIFilter?
    private var routeTrackFilter: PoiUIFilter?
    private var routeArticlePointsFilterByName: String?
    private var fileVisibilityChanged = false

    /// cache for displayed POI
    // Work with cache (for map copied from AmenityIndexRepositoryOdb)
    private val data: MapLayerData<List<Amenity>>

    init {
        showTravel = app.settings?.SHOW_TRAVEL?.get()
        routeArticleFilterEnabled = travelRendererHelper?.routeArticlesProperty?.get()
        routeArticlePointsFilterEnabled = travelRendererHelper?.routeArticlePointsProperty?.get()
        routeArticleFilter = travelRendererHelper?.routeArticleFilter
        routeArticlePointsFilter = travelRendererHelper?.routeArticlePointsFilter
        routeTrackFilter = travelRendererHelper?.routeTrackFilter
        routeArticlePointsFilterByName =
            if (routeArticlePointsFilter != null) routeArticlePointsFilter?.filterByName else null
        routingHelper?.addListener(this)
        travelRendererHelper?.addFileVisibilityListener(this)
        data = object : MapLayerData<List<Amenity>>() {
            var calculatedFilters: Set<PoiUIFilter>? = null

            init {
                ZOOM_THRESHOLD = 0
            }

            override fun layerOnPreExecute() {
                calculatedFilters = collectFilters()
            }

            override fun layerOnPostExecute() {
                app.osmandMap?.refreshMap()
            }

            override fun calculateResult(latLonBounds: QuadRect, zoom: Int): List<Amenity> {
                if (calculatedFilters.isNullOrEmpty()) {
                    return ArrayList()
                }
                val z = floor(zoom + ln(mapDensity.toDouble()) / ln(2.0)).toInt()
                val res: MutableList<Amenity> = ArrayList()
                PoiUIFilter.combineStandardPoiFilters(calculatedFilters, app)
                calculatedFilters?.let {
                    for (filter in it) {
                        val amenities = filter.searchAmenities(latLonBounds.top,
                            latLonBounds.left,
                            latLonBounds.bottom,
                            latLonBounds.right,
                            z,
                            object : ResultMatcher<Amenity> {
                                override fun publish(`object`: Amenity): Boolean {
                                    return true
                                }

                                override fun isCancelled(): Boolean {
                                    return isInterrupted
                                }
                            })
                        if (filter.isRouteTrackFilter) {
                            for (amenity in amenities) {
                                var hasRoute = false
                                val routeId = amenity.routeId
                                if (!Algorithms.isEmpty(routeId)) {
                                    for (a in res) {
                                        if (routeId == a.routeId) {
                                            hasRoute = true
                                            break
                                        }
                                    }
                                }
                                if (!hasRoute) {
                                    res.add(amenity)
                                }
                            }
                        } else {
                            res.addAll(amenities)
                        }
                    }
                }
                res.sortWith { lhs: Amenity, rhs: Amenity -> if (lhs.id < rhs.id) -1 else if (lhs.id.toLong() == rhs.id.toLong()) 0 else 1 }
                return res
            }
        }
    }

    private fun collectFilters(): Set<PoiUIFilter> {
        val calculatedFilters: MutableSet<PoiUIFilter> = TreeSet(filters)
        if (showTravel == true) {
            val routeArticleFilterEnabled = routeArticleFilterEnabled
            val routeArticleFilter = routeArticleFilter
            if (routeArticleFilterEnabled == true && routeArticleFilter != null) {
                calculatedFilters.add(routeArticleFilter)
            }
            val routeArticlePointsFilterEnabled = routeArticlePointsFilterEnabled
            val routeArticlePointsFilter = routeArticlePointsFilter
            if (routeArticlePointsFilterEnabled == true && routeArticlePointsFilter != null && !Algorithms.isEmpty(
                    routeArticlePointsFilter.filterByName
                )
            ) {
                calculatedFilters.add(routeArticlePointsFilter)
            }
            val routeTrackAsPoiFilterEnabled = routeTrackAsPoiFilterEnabled
            val routeTrackFilter = routeTrackFilter
            if (routeTrackAsPoiFilterEnabled && routeTrackFilter != null) {
                calculatedFilters.add(routeTrackFilter)
            }
        }
        return calculatedFilters
    }

    private fun getAmenityFromPoint(
        tb: RotatedTileBox,
        point: PointF,
        am: MutableList<in Amenity>
    ) {
        val objects = data.results
        view.application?.let {
            if (objects != null) {
                val ex = point.x.toInt()
                val ey = point.y.toInt()
                var compare = getScaledTouchRadius(it, getRadiusPoi(tb))
                val radius = compare * 3 / 2
                try {
                    for (i in objects.indices) {
                        val n = objects[i]
                        val pixel = NativeUtilities.getPixelFromLatLon(
                            mapRenderer, tb, n.location.latitude, n.location.longitude
                        )
                        if (abs(pixel.x - ex) <= compare && abs(pixel.y - ey) <= compare) {
                            compare = radius
                            am.add(n)
                        }
                    }
                } catch (e: IndexOutOfBoundsException) {
                    // that's really rare case, but is much efficient than introduce synchronized block
                }
            }
        }
    }

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        mapTextLayer = view.getLayerByClass(MapTextLayer::class.java)
    }

    private fun getRadiusPoi(tb: RotatedTileBox): Int {
        val zoom = tb.zoom.toDouble()
        val r = if (zoom < START_ZOOM) {
            0
        } else if (zoom <= 15) {
            10
        } else if (zoom <= 16) {
            14
        } else if (zoom <= 17) {
            16
        } else {
            18
        }
        return (r * view.scaleCoefficient).toInt()
    }

    private fun getColor(amenity: Amenity): Int {
        var color = 0
        if (MapPoiTypes.ROUTE_ARTICLE_POINT == amenity.subType) {
            val colorStr = amenity.color
            if (colorStr != null) {
                color = ColorDialogs.getColorByTag(colorStr)
            }
        }
        return if (color != 0) color else ContextCompat.getColor(app, R.color.osmand_orange)
    }

    private fun shouldDraw(zoom: Int): Boolean {
        if (filters.isNotEmpty() && zoom >= START_ZOOM) {
            return true
        } else if (filters.isEmpty()) {
            if ((travelRendererHelper?.routeArticlesProperty?.get() == true && routeArticleFilter != null || travelRendererHelper?.routeArticlePointsProperty?.get() == true && routeArticlePointsFilter != null) && zoom >= START_ZOOM) {
                return true
            }
            if (travelRendererHelper?.routeTracksAsPoiProperty?.get() == true && routeTrackFilter != null) {
                return if (travelRendererHelper.routeTracksProperty?.get() == true) zoom >= START_ZOOM else zoom >= START_ZOOM_ROUTE_TRACK
            }
        }
        return false
    }

    private fun shouldDraw(tileBox: RotatedTileBox, amenity: Amenity): Boolean {
        val routeArticle =
            MapPoiTypes.ROUTE_ARTICLE_POINT == amenity.subType || MapPoiTypes.ROUTE_ARTICLE == amenity.subType
        val routeTrack = MapPoiTypes.ROUTE_TRACK == amenity.subType
        return if (routeArticle) {
            tileBox.zoom >= START_ZOOM
        } else if (routeTrack) {
            if (travelRendererHelper?.routeTracksProperty?.get() == true) {
                tileBox.zoom in START_ZOOM..END_ZOOM_ROUTE_TRACK
            } else {
                tileBox.zoom >= START_ZOOM_ROUTE_TRACK
            }
        } else {
            tileBox.zoom >= START_ZOOM
        }
    }

    override fun fileVisibilityChanged() {
        fileVisibilityChanged = true
    }

    override fun onPrepareBufferImage(
        canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings?
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        val selectedPoiFilters = app.poiFilters?.getSelectedPoiFilters()
        val showTravel = app.settings?.SHOW_TRAVEL?.get()
        val routeArticleFilterEnabled = travelRendererHelper?.routeArticlesProperty?.get()
        val routeArticlePointsFilterEnabled = travelRendererHelper?.routeArticlePointsProperty?.get()
        val routeTrackFilterEnabled = travelRendererHelper?.routeTracksProperty?.get()
        val routeTrackAsPoiFilterEnabled = travelRendererHelper?.routeTracksAsPoiProperty?.get()
        val routeArticleFilter = travelRendererHelper?.routeArticleFilter
        val routeArticlePointsFilter = travelRendererHelper?.routeArticlePointsFilter
        val routeTrackFilter = travelRendererHelper?.routeTrackFilter
        val routeArticlePointsFilterByName = routeArticlePointsFilter?.filterByName
        if (filters !== selectedPoiFilters || this.showTravel != showTravel || this.routeArticleFilterEnabled != routeArticleFilterEnabled || this.routeArticlePointsFilterEnabled != routeArticlePointsFilterEnabled || this.routeTrackFilterEnabled != routeTrackFilterEnabled || this.routeTrackAsPoiFilterEnabled != routeTrackAsPoiFilterEnabled || this.routeArticleFilter !== routeArticleFilter || this.routeArticlePointsFilter !== routeArticlePointsFilter || this.routeTrackFilter !== routeTrackFilter || fileVisibilityChanged || !Algorithms.stringsEqual(
                this.routeArticlePointsFilterByName, routeArticlePointsFilterByName
            )
        ) {
            if (selectedPoiFilters != null) {
                filters = selectedPoiFilters
            }
            this.showTravel = showTravel
            this.routeArticleFilterEnabled = routeArticleFilterEnabled
            this.routeArticlePointsFilterEnabled = routeArticlePointsFilterEnabled
            if (routeTrackFilterEnabled != null) {
                this.routeTrackFilterEnabled = routeTrackFilterEnabled
            }
            if (routeTrackAsPoiFilterEnabled != null) {
                this.routeTrackAsPoiFilterEnabled = routeTrackAsPoiFilterEnabled
            }
            this.routeArticleFilter = routeArticleFilter
            this.routeArticlePointsFilter = routeArticlePointsFilter
            this.routeTrackFilter = routeTrackFilter
            this.routeArticlePointsFilterByName = routeArticlePointsFilterByName
            fileVisibilityChanged = false
            data.clearCache()
        }
        val zoom = tileBox.zoom
        val fullObjects: MutableList<Amenity> = ArrayList()
        val fullObjectsLatLon: MutableList<LatLon> = ArrayList()
        val smallObjectsLatLon: MutableList<LatLon> = ArrayList()
        if (shouldDraw(zoom)) {
            data.queryNewData(tileBox)
            val objects = data.results
            if (objects != null) {
                val textScale = textScale
                val iconSize = getIconSize(app)
                val boundIntersections = initBoundIntersections(tileBox)
                for (o in objects) {
                    if (shouldDraw(tileBox, o)) {
                        val latLon = o.location
                        val x = tileBox.getPixXFromLatLon(latLon.latitude, latLon.longitude)
                        val y = tileBox.getPixYFromLatLon(latLon.latitude, latLon.longitude)
                        if (tileBox.containsPoint(x, y, iconSize)) {
                            val intersects = intersects(boundIntersections, x, y, iconSize, iconSize)
                            if (!intersects) {
                                val iconResId = (o.gpxIcon ?: RenderingIcons.getIconNameForAmenity(o))?.let(RenderingIcons::getResId)
                                if (iconResId != null) {
                                    val pointImageDrawable = PointImageDrawable.getOrCreate(context, getColor(o), true, iconResId)
                                    pointImageDrawable.drawPoint(canvas, x, y, textScale, false)
                                }
                                fullObjects.add(o)
                                fullObjectsLatLon.add(latLon)
                            }
                        }
                    }
                }
                this.fullObjectsLatLon = fullObjectsLatLon
                this.smallObjectsLatLon = smallObjectsLatLon
            }
        }
        mapTextLayer?.putData(this, fullObjects)
        mapActivityInvalidated = false
    }

    private fun clearPoiTileProvider() {
        val mapRenderer = mapRenderer
        if (mapRenderer != null && poiTileProvider != null) {
            poiTileProvider?.clearSymbols(mapRenderer)
            poiTileProvider = null
        }
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) {}

    override fun destroyLayer() {
        super.destroyLayer()
        clearPoiTileProvider()
        routingHelper?.removeListener(this)
        travelRendererHelper?.removeFileVisibilityListener(this)
    }

    override fun drawInScreenPixels(): Boolean {
        return true
    }

    override fun getObjectName(o: Any?): PointDescription? {
        return if (o is Amenity) {
            PointDescription(
                PointDescription.POINT_TYPE_POI, getAmenityName(o)
            )
        } else null
    }

    override fun disableSingleTap(): Boolean {
        return false
    }

    override fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean {
        return false
    }

    override fun collectObjectsFromPoint(
        point: PointF, tileBox: RotatedTileBox, o: MutableList<Any>, unknownLocation: Boolean
    ) {
        if (tileBox.zoom >= START_ZOOM) {
            getAmenityFromPoint(tileBox, point, o)
        }
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        return if (o is Amenity) {
            o.location
        } else null
    }

    override fun isObjectClickable(o: Any): Boolean {
        return o is Amenity
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        return false
    }

    override fun getTextLocation(o: Amenity?): LatLon? {
        return o?.location
    }

    override fun getTextShift(o: Amenity?, rb: RotatedTileBox): Int {
        return (16 * rb.density * textScale).toInt()
    }

    override fun getText(o: Amenity?): String? {
        return o?.let { getAmenityName(it) }
    }

    private fun getAmenityName(amenity: Amenity): String? {
        var locale = app.settings?.MAP_PREFERRED_LOCALE?.get()
        if (amenity.type.isWiki) {
            if (Algorithms.isEmpty(locale)) {
                locale = app.language
            }
            locale = PluginsHelper.onGetMapObjectsLocale(amenity, locale)
        }
        return app.settings?.MAP_TRANSLITERATE_NAMES?.get()?.let { amenity.getName(locale, it) }
    }

    override fun isTextVisible(): Boolean = false

    override fun isFakeBoldText(): Boolean = false

    override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean>) {}
    override fun routeWasCancelled() {}
    override fun routeWasFinished() {}

    companion object {
        private const val START_ZOOM = 15
        private const val START_ZOOM_ROUTE_TRACK = 15
        private const val END_ZOOM_ROUTE_TRACK = 17
        val log: Log = PlatformUtil.getLog(POIMapLayer::class.java)

        @JvmStatic
        fun showPlainDescriptionDialog(
            ctx: Context, app: OsmandApplication, text: String?, title: String
        ) {
            val textView = TextView(ctx)
            val llTextParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val textMargin = AndroidUtils.dpToPx(app, 10f)
            val light = app.settings?.isLightContent
            textView.layoutParams = llTextParams
            textView.setPadding(textMargin, textMargin, textMargin, textMargin)
            textView.textSize = 16f
            textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, light == false))
            textView.autoLinkMask = Linkify.ALL
            textView.linksClickable = true
            textView.text = text
            showText(ctx, app, textView, title)
        }

        @JvmStatic
        fun showHtmlDescriptionDialog(
            ctx: Context, app: OsmandApplication, html: String, title: String
        ) {
            var mutableHtml = html
            val webView = WebViewEx(ctx)
            val llTextParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            webView.layoutParams = llTextParams
            val margin = AndroidUtils.dpToPx(app, 10f)
            webView.setPadding(margin, margin, margin, margin)
            webView.isScrollbarFadingEnabled = true
            webView.isVerticalScrollBarEnabled = false
            webView.setBackgroundColor(Color.TRANSPARENT)
            webView.settings.textZoom = (app.resources.configuration.fontScale * 100f).toInt()
            val light = app.settings?.isLightContent
            val textColor = ColorUtilities.getPrimaryTextColor(app, light == false)
            val rgbHex = Algorithms.colorToString(textColor)
            mutableHtml = "<body style=\"color:$rgbHex;\">$mutableHtml</body>"
            val encoded = Base64.encodeToString(mutableHtml.toByteArray(), Base64.NO_PADDING)
            webView.loadData(encoded, "text/html", "base64")
            showText(ctx, app, webView, title)
        }

        private fun getResIdFromAttribute(ctx: Context, attr: Int): Int {
            if (attr == 0) {
                return 0
            }
            val typedvalueattr = TypedValue()
            ctx.theme.resolveAttribute(attr, typedvalueattr, true)
            return typedvalueattr.resourceId
        }

        private fun showText(ctx: Context, app: OsmandApplication, view: View, title: String) {
            val dialog = Dialog(
                ctx,
                if (app.settings?.isLightContent == true) R.style.OsmandLightTheme else R.style.OsmandDarkTheme
            )
            val ll = LinearLayout(ctx)
            ll.orientation = LinearLayout.VERTICAL
            val topBar = Toolbar(ctx)
            topBar.isClickable = true
            val icBack = app.uIUtilities.getIcon(AndroidUtils.getNavigationIconResId(ctx))
            topBar.navigationIcon = icBack
            topBar.setNavigationContentDescription(R.string.access_shared_string_navigate_up)
            topBar.title = title
            topBar.setBackgroundColor(
                ContextCompat.getColor(
                    ctx, getResIdFromAttribute(ctx, R.attr.pstsTabBackground)
                )
            )
            topBar.setTitleTextColor(
                ContextCompat.getColor(
                    ctx, getResIdFromAttribute(ctx, R.attr.pstsTextColor)
                )
            )
            topBar.setNavigationOnClickListener { dialog.dismiss() }
            val scrollView = ScrollView(ctx)
            ll.addView(topBar)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0)
            lp.weight = 1f
            ll.addView(scrollView, lp)
            scrollView.addView(view)
            dialog.setContentView(ll)
            dialog.setCancelable(true)
            dialog.show()
        }
    }
}