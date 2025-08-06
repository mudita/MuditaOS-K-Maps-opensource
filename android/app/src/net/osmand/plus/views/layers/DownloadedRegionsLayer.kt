package net.osmand.plus.views.layers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.text.TextPaint
import android.util.DisplayMetrics
import android.view.WindowManager
import com.mudita.maps.R
import java.io.IOException
import java.util.LinkedList
import java.util.TreeSet
import kotlin.math.abs
import net.osmand.binary.BinaryMapDataObject
import net.osmand.core.jni.PointI
import net.osmand.core.jni.PolygonBuilder
import net.osmand.core.jni.PolygonsCollection
import net.osmand.core.jni.QVectorPointI
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox
import net.osmand.map.OsmandRegions
import net.osmand.map.WorldRegion
import net.osmand.plus.AppInitializer
import net.osmand.plus.AppInitializer.AppInitializeListener
import net.osmand.plus.AppInitializer.InitEvents
import net.osmand.plus.OsmandApplication
import net.osmand.plus.download.DownloadActivityType
import net.osmand.plus.download.IndexItem
import net.osmand.plus.download.LocalIndexHelper
import net.osmand.plus.download.LocalIndexInfo
import net.osmand.plus.resources.ResourceManager
import net.osmand.plus.resources.ResourceManager.ResourceListener
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.util.MapUtils

class DownloadedRegionsLayer(context: Context) : OsmandMapLayer(context), IContextMenuProvider,
    IContextMenuProviderSelection, ResourceListener {

    private var app: OsmandApplication? = null
    private val paintDownloaded: Paint by lazy {
        getPaint(getColor(R.color.region_uptodate))
    }
    private val paintSelected: Paint by lazy {
        getPaint(getColor(R.color.region_selected))
    }
    private val paintBackuped: Paint by lazy {
        getPaint(getColor(R.color.region_backuped))
    }
    private val osmandRegions: OsmandRegions by lazy {
        rm.osmandRegions
    }
    private val helper: LocalIndexHelper by lazy {
        LocalIndexHelper(app)
    }
    private val pathDownloaded = Path()
    private val pathSelected = Path()
    private val pathBackuped = Path()
    private var textPaint = TextPaint()
    private val rm: ResourceManager by lazy {
        app?.resourceManager!!
    }
    private val data: MapLayerData<List<BinaryMapDataObject>> = object : MapLayerData<List<BinaryMapDataObject>>() {
        override fun layerOnPostExecute() {
            view.refreshMap()
        }

        override fun queriedBoxContains(
            queriedData: RotatedTileBox?,
            newBox: RotatedTileBox
        ): Boolean {
            if (newBox.zoom < ZOOM_TO_SHOW_SELECTION) {
                return if (queriedData != null && queriedData.zoom < ZOOM_TO_SHOW_SELECTION) {
                    queriedData.containsTileBox(newBox)
                } else {
                    false
                }
            }
            val queriedResults = getResults()
            return if (queriedData != null && queriedData.containsTileBox(newBox) && queriedData.zoom >= ZOOM_TO_SHOW_MAP_NAMES) {
                queriedResults != null && (queriedResults.isEmpty() || abs(queriedData.zoom - newBox.zoom) <= 1)
            } else false
        }

        override fun calculateResult(
            latLonBounds: QuadRect,
            zoom: Int
        ): List<BinaryMapDataObject>? {
            return queryData(latLonBounds, zoom)
        }
    }
    private var selectedObjects: List<BinaryMapDataObject> = LinkedList()
    private var lastCheckMapCx = 0
    private var lastCheckMapCy = 0
    private var lastCheckMapZoom = 0

    //OpenGL
    private var polygonsCollection: PolygonsCollection? = null
    private var downloadedSize = 0
    private var selectedSize = 0
    private var backupedSize = 0
    private var polygonId = 1
    private var needRedrawOpenGL = false
    private var indexRegionBoundaries = false
    private var onMapsChanged = false
    private var cachedShowDownloadedMaps = false
    private var downloadedRegions: List<WorldRegion>? = null
    private var backedUpRegions: List<WorldRegion>? = null

    class DownloadMapObject(
        val dataObject: BinaryMapDataObject, val worldRegion: WorldRegion,
        val indexItem: IndexItem?, val localIndexInfo: LocalIndexInfo?
    )

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        app = view.application
        rm.addResourceListener(this)
        cachedShowDownloadedMaps = isShowDownloadedMaps
        val wmgr = view.application?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wmgr.defaultDisplay.getMetrics(dm)
        textPaint.strokeWidth = 21 * dm.scaledDensity
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        addMapsInitializedListener()
    }

    private fun getPaint(color: Int): Paint {
        val paint = Paint()
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f
        paint.color = color
        paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        return paint
    }

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        val zoom = tileBox.zoom
        if (zoom < ZOOM_TO_SHOW_SELECTION_ST || !indexRegionBoundaries) {
            return
        }
        // make sure no maps are loaded for the location
        checkMapToDownload(tileBox)
        // draw objects
        if (osmandRegions.isInitialized && zoom < ZOOM_TO_SHOW_SELECTION) {
            val mapRenderer = mapRenderer
            if (mapRenderer != null) {
                drawMapPolygons(zoom)
                return
            }
            val currentObjects: MutableList<BinaryMapDataObject> = LinkedList()
            if (data.results != null) {
                currentObjects.addAll(data.results)
            }
            val selectedObjects: List<BinaryMapDataObject> = LinkedList(
                selectedObjects
            )
            if (selectedObjects.isNotEmpty()) {
                removeObjectsFromList(currentObjects, selectedObjects)
                drawBorders(canvas, tileBox, selectedObjects, pathSelected, paintSelected)
            }
            val isZoomToShowBorders = zoom in (ZOOM_TO_SHOW_BORDERS_ST until ZOOM_TO_SHOW_BORDERS)
            if (isShowDownloadedMaps && isZoomToShowBorders) {
                if (currentObjects.size > 0) {
                    val downloadedObjects: MutableList<BinaryMapDataObject> = ArrayList()
                    val backupedObjects: MutableList<BinaryMapDataObject> = ArrayList()
                    for (o in currentObjects) {
                        val downloaded = rm.checkIfObjectDownloaded(
                            osmandRegions.getDownloadName(o)
                        )
                        val backuped =
                            rm.checkIfObjectBackuped(osmandRegions.getDownloadName(o))
                        if (downloaded) {
                            downloadedObjects.add(o)
                        } else if (backuped) {
                            backupedObjects.add(o)
                        }
                    }
                    if (backupedObjects.size > 0) {
                        drawBorders(canvas, tileBox, backupedObjects, pathBackuped, paintBackuped)
                    }
                    if (downloadedObjects.size > 0) {
                        drawBorders(
                            canvas,
                            tileBox,
                            downloadedObjects,
                            pathDownloaded,
                            paintDownloaded
                        )
                    }
                }
            }
        } else {
            clearPolygonsCollections()
        }
    }

    private fun checkMapToDownload(tileBox: RotatedTileBox) {
        val zoom = tileBox.zoom
        val cx = tileBox.center31X
        val cy = tileBox.center31Y
        if (lastCheckMapCx == cx && lastCheckMapCy == cy && lastCheckMapZoom == zoom) {
            return
        }
        lastCheckMapCx = cx
        lastCheckMapCy = cy
        lastCheckMapZoom = zoom
        if (zoom >= ZOOM_MIN_TO_SHOW_DOWNLOAD_DIALOG && view?.isAnimatingMapMove == false) {
            checkMissingRegion(tileBox.centerLatLon)
        } else {
            checkMissingRegion(null)
        }
    }

    private fun checkMissingRegion(latLon: LatLon?) {
        mapActivity?.checkMissingRegion(latLon)
    }

    private fun removeObjectsFromList(
        list: MutableList<BinaryMapDataObject>,
        objects: List<BinaryMapDataObject>
    ) {
        val it = list.iterator()
        while (it.hasNext()) {
            val o = it.next()
            for (obj in objects) {
                if (o.id == obj.id) {
                    it.remove()
                    break
                }
            }
        }
    }

    private fun drawBorders(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        objects: List<BinaryMapDataObject>,
        path: Path,
        paint: Paint
    ) {
        path.reset()
        for (o in objects) {
            var lat = MapUtils.get31LatitudeY(o.getPoint31YTile(0))
            var lon = MapUtils.get31LongitudeX(o.getPoint31XTile(0))
            path.moveTo(
                tileBox.getPixXFromLonNoRot(lon).toFloat(),
                tileBox.getPixYFromLatNoRot(lat).toFloat()
            )
            for (j in 1 until o.pointsLength) {
                lat = MapUtils.get31LatitudeY(o.getPoint31YTile(j))
                lon = MapUtils.get31LongitudeX(o.getPoint31XTile(j))
                path.lineTo(
                    tileBox.getPixXFromLonNoRot(lon).toFloat(),
                    tileBox.getPixYFromLatNoRot(lat).toFloat()
                )
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun queryData(latLonBounds: QuadRect, zoom: Int): List<BinaryMapDataObject>? {
        if (zoom >= ZOOM_AFTER_BASEMAP) {
            if (!checkIfMapEmpty(zoom)) {
                return emptyList()
            }
        }
        val result: MutableList<BinaryMapDataObject>
        val left = MapUtils.get31TileNumberX(latLonBounds.left)
        val right = MapUtils.get31TileNumberX(latLonBounds.right)
        val top = MapUtils.get31TileNumberY(latLonBounds.top)
        val bottom = MapUtils.get31TileNumberY(latLonBounds.bottom)
        result = try {
            osmandRegions.query(left, right, top, bottom, false)
        } catch (e: IOException) {
            return null
        }
        val it = result.iterator()
        while (it.hasNext()) {
            val o = it.next()
            if (zoom >= ZOOM_TO_SHOW_SELECTION) {
                if (!OsmandRegions.contain(o, left / 2 + right / 2, top / 2 + bottom / 2)) {
                    it.remove()
                }
            }
        }
        return result
    }

    private fun checkIfMapEmpty(zoom: Int): Boolean {
        val cState = rm.renderer.checkedRenderedState
        val empty: Boolean = if (zoom < ZOOM_AFTER_BASEMAP) {
            cState == 0
        } else {
            cState <= 1
        }
        return empty
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, nightMode: DrawSettings) {
        if (view.getMainLayer() is MapTileLayer) {
            return
        }
        // query from UI thread because of Android AsyncTask bug (Handler init)
        data.queryNewData(tileBox)
    }

    fun getFilter(btnName: StringBuilder): String? {
        val filter = StringBuilder()
        val zoom = view.zoom
        val queriedBox = data.getQueriedBox()
        val currentObjects = data.results
        if (osmandRegions.isInitialized && queriedBox != null) {
            if (zoom >= ZOOM_TO_SHOW_MAP_NAMES && abs(queriedBox.zoom - zoom) <= ZOOM_THRESHOLD && currentObjects != null) {
                btnName.setLength(0)
                btnName.append(view.resources?.getString(R.string.shared_string_download))
                filter.setLength(0)
                val set: MutableSet<String> = TreeSet()
                view.currentRotatedTileBox?.let {
                    val cx = it.center31X
                    val cy = it.center31Y
                    if (currentObjects.isNotEmpty()) {
                        for (i in currentObjects.indices) {
                            val o = currentObjects[i]
                            if (!OsmandRegions.contain(o, cx, cy)) {
                                continue
                            }
                            val fullName = osmandRegions.getFullName(o)
                            val rd = osmandRegions.getRegionData(fullName)
                            if (rd != null && rd.isRegionMapDownload && rd.regionDownloadName != null) {
                                val name = rd.localeName
                                if (rm.checkIfObjectDownloaded(rd.regionDownloadName)) {
                                    return null
                                }
                                if (!set.add(name)) {
                                    continue
                                }
                                if (set.size > 1) {
                                    btnName.append(" ")
                                        .append(view.resources?.getString(R.string.shared_string_or))
                                        .append(" ")
                                    filter.append(", ")
                                } else {
                                    btnName.append(" ")
                                }
                                filter.append(name)
                                btnName.append(name)
                            }
                        }
                    }
                }
            }
        }
        return if (filter.isEmpty()) {
            null
        } else filter.toString()
    }

    override fun drawInScreenPixels(): Boolean = false

    override fun onLongPressEvent(point: PointF, tileBox: RotatedTileBox): Boolean = false

    override fun destroyLayer() {
        super.destroyLayer()
        rm.removeResourceListener(this)
        clearPolygonsCollections()
    }

    // IContextMenuProvider
    override fun collectObjectsFromPoint(
        point: PointF,
        tileBox: RotatedTileBox,
        o: MutableList<Any>,
        unknownLocation: Boolean
    ) {
        var isMenuVisible = false
        val mapActivity = view.mapActivity
        if (!isMenuVisible) {
            getWorldRegionFromPoint(tileBox, point, o)
        }
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        if (o is DownloadMapObject) {
            return o.worldRegion.regionCenter
        }
        return null
    }

    override fun getObjectName(o: Any?): PointDescription {
        if (o is DownloadMapObject) {
            return PointDescription(
                PointDescription.POINT_TYPE_WORLD_REGION,
                context.getString(R.string.shared_string_map), o.worldRegion.localeName
            )
        }
        return PointDescription(
            PointDescription.POINT_TYPE_WORLD_REGION,
            context.getString(R.string.shared_string_map), ""
        )
    }

    override fun disableSingleTap(): Boolean = false

    override fun disableLongPressOnMap(point: PointF, tileBox: RotatedTileBox): Boolean = false

    override fun isObjectClickable(o: Any): Boolean = false

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean = false

    private fun getWorldRegionFromPoint(
        tb: RotatedTileBox?,
        point: PointF?,
        dataObjects: MutableList<Any>
    ) {
        tb?.zoom?.let { zoom ->
            if (zoom in ZOOM_TO_SHOW_SELECTION_ST until ZOOM_TO_SHOW_SELECTION && data.results != null && osmandRegions.isInitialized && point != null) {
                val pointLatLon =
                    NativeUtilities.getLatLonFromPixel(mapRenderer, tb, point.x, point.y)
                val point31x = MapUtils.get31TileNumberX(pointLatLon.longitude)
                val point31y = MapUtils.get31TileNumberY(pointLatLon.latitude)
                val result: MutableList<BinaryMapDataObject> = LinkedList(
                    data.results
                )
                val it = result.iterator()
                while (it.hasNext()) {
                    val o = it.next()
                    var isRegion = true
                    for (i in o.types.indices) {
                        val tp = o.mapIndex.decodeType(o.types[i])
                        if ("boundary" == tp.value) {
                            isRegion = false
                            break
                        }
                    }
                    if (!isRegion || !OsmandRegions.contain(o, point31x, point31y)) {
                        it.remove()
                    }
                }
                val osmandRegions = app?.regions
                for (o in result) {
                    val fullName = osmandRegions?.getFullName(o)
                    val region = osmandRegions?.getRegionData(fullName)
                    if (region != null && region.isRegionMapDownload) {
                        val indexItems = app?.downloadThread?.indexes?.getIndexItems(region)
                        val dataItems: MutableList<IndexItem> = LinkedList()
                        var regularMapItem: IndexItem? = null
                        if (indexItems != null) {
                            for (item in indexItems) {
                                if (item.isDownloaded || app?.downloadThread?.isDownloading(item) == true) {
                                    dataItems.add(item)
                                    if (item.type === DownloadActivityType.NORMAL_FILE) {
                                        regularMapItem = item
                                    }
                                }
                            }
                        }
                        if (dataItems.isEmpty() && regularMapItem != null) {
                            dataItems.add(regularMapItem)
                        }
                        if (dataItems.isNotEmpty()) {
                            for (item in dataItems) {
                                dataObjects.add(DownloadMapObject(o, region, item, null))
                            }
                        } else {
                            val downloadName = osmandRegions.getDownloadName(o)
                            val infos = helper.getLocalIndexInfos(downloadName)
                            if (infos.size == 0) {
                                dataObjects.add(DownloadMapObject(o, region, null, null))
                            } else {
                                for (info in infos) {
                                    dataObjects.add(DownloadMapObject(o, region, null, info))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getOrder(o: Any?): Int {
        var order = 0
        if (o is DownloadMapObject) {
            order = o.worldRegion.level * 1000 - 100000
            if (o.indexItem != null) {
                order += o.indexItem.type.orderIndex
            } else if (o.localIndexInfo != null) {
                order += o.localIndexInfo.type.getOrderIndex(o.localIndexInfo)
            }
        }
        return order
    }

    override fun setSelectedObject(o: Any?) {
        if (o is DownloadMapObject) {
            val list: MutableList<BinaryMapDataObject> = LinkedList()
            list.add(o.dataObject)
            selectedObjects = list
        }
    }

    override fun clearSelectedObject() {
        selectedObjects = LinkedList()
    }

    /**OpenGL */
    private fun drawMapPolygons(zoom: Int) {
        val mapRenderer = mapRenderer ?: return
        val showDownloadedMaps = isShowDownloadedMaps
        val showDownloadedMapsChanged = cachedShowDownloadedMaps != showDownloadedMaps
        cachedShowDownloadedMaps = showDownloadedMaps
        if (onMapsChanged || showDownloadedMapsChanged) {
            clearPolygonsCollections()
            onMapsChanged = false
            downloadedRegions = null
            backedUpRegions = null
        }
        if (polygonsCollection != null && selectedSize == selectedObjects.size && !showDownloadedMapsChanged && !mapActivityInvalidated) {
            return
        }
        var downloadedRegions: MutableList<WorldRegion> = ArrayList()
        var backupedRegions: MutableList<WorldRegion> = ArrayList()
        if (showDownloadedMaps && zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS) {
            if (this.downloadedRegions == null || this.backedUpRegions == null) {
                val worldRegions = osmandRegions.allRegionData
                for (wr in worldRegions) {
                    val n = wr.regionDownloadName
                    if (rm.checkIfObjectDownloaded(n)) {
                        downloadedRegions.add(wr)
                    } else if (rm.checkIfObjectBackuped(n)) {
                        backupedRegions.add(wr)
                    }
                }
                this.downloadedRegions = ArrayList(downloadedRegions)
                this.backedUpRegions = ArrayList(backupedRegions)
            } else {
                downloadedRegions = ArrayList(this.downloadedRegions ?: emptyList())
                backupedRegions = ArrayList(this.backedUpRegions ?: emptyList())
            }
        }
        val selectedRegions: MutableList<WorldRegion> = ArrayList()
        if (zoom in ZOOM_TO_SHOW_SELECTION_ST until ZOOM_TO_SHOW_SELECTION) {
            if (selectedObjects.isNotEmpty()) {
                for (o in selectedObjects) {
                    val fullName = osmandRegions.getFullName(o)
                    val wr = osmandRegions.getRegionData(fullName)
                    if (wr != null) {
                        selectedRegions.add(wr)
                        downloadedRegions.remove(wr)
                        backupedRegions.remove(wr)
                    }
                }
            }
        }
        if (backupedSize != backupedRegions.size || downloadedSize != downloadedRegions.size || selectedSize != selectedRegions.size) {
            clearPolygonsCollections()
            backupedSize = backupedRegions.size
            downloadedSize = downloadedRegions.size
            selectedSize = selectedRegions.size
        }
        var baseOrder = baseOrder
        if (zoom in ZOOM_TO_SHOW_BORDERS_ST until ZOOM_TO_SHOW_BORDERS) {
            baseOrder = addToPolygonsCollection(downloadedRegions, paintDownloaded, baseOrder)
            baseOrder = addToPolygonsCollection(backupedRegions, paintBackuped, baseOrder)
        }
        if (zoom in ZOOM_TO_SHOW_SELECTION_ST until ZOOM_TO_SHOW_SELECTION) {
            addToPolygonsCollection(selectedRegions, paintSelected, baseOrder)
        }
        if ((needRedrawOpenGL || mapActivityInvalidated) && polygonsCollection != null) {
            mapRenderer.addSymbolsProvider(polygonsCollection)
            needRedrawOpenGL = false
        }
        mapActivityInvalidated = false
    }

    /**OpenGL */
    private fun addToPolygonsCollection(
        regionList: List<WorldRegion>,
        paint: Paint,
        baseOrder: Int
    ): Int {
        var order = baseOrder
        val mapRenderer = mapView.mapRenderer
        if (mapRenderer == null || regionList.isEmpty() || !needRedrawOpenGL) {
            return order
        }
        if (polygonsCollection == null) {
            polygonsCollection = PolygonsCollection()
        }
        for (region in regionList) {
            for (polygon in region.polygons) {
                val points = QVectorPointI()
                for (latLon in polygon) {
                    val x = MapUtils.get31TileNumberX(latLon.longitude)
                    val y = MapUtils.get31TileNumberY(latLon.latitude)
                    points.add(PointI(x, y))
                }
                val colorARGB = NativeUtilities.createFColorARGB(paint.color)
                val polygonBuilder = PolygonBuilder()
                polygonBuilder.setBaseOrder(order--)
                    .setIsHidden(points.size() < 3)
                    .setPolygonId(++polygonId)
                    .setPoints(points)
                    .setFillColor(colorARGB)
                    .buildAndAddToCollection(polygonsCollection)
            }
        }
        return order
    }

    /**OpenGL */
    private fun clearPolygonsCollections() {
        val mapRenderer = mapRenderer ?: return
        if (polygonsCollection != null) {
            mapRenderer.removeSymbolsProvider(polygonsCollection)
            polygonsCollection = null
        }
        needRedrawOpenGL = true
        selectedSize = 0
        polygonId = 1
    }

    private fun addMapsInitializedListener() {
        val app = application
        if (app.isApplicationInitializing) {
            app.appInitializer.addListener(object : AppInitializeListener {
                override fun onProgress(init: AppInitializer, event: InitEvents) {
                    if (event == InitEvents.INDEX_REGION_BOUNDARIES) {
                        indexRegionBoundaries = true
                    }
                }
            })
        } else {
            indexRegionBoundaries = true
        }
    }

    private val isShowDownloadedMaps: Boolean
        get() = app?.settings?.SHOW_BORDERS_OF_DOWNLOADED_MAPS?.get() == true

    override fun onMapsIndexed() {
        onMapsChanged = true
    }

    override fun onMapClosed(fileName: String) {
        onMapsChanged = true
    }

    companion object {
        private const val ZOOM_THRESHOLD = 2
        private const val ZOOM_TO_SHOW_MAP_NAMES = 6
        private const val ZOOM_AFTER_BASEMAP = 12
        private const val ZOOM_TO_SHOW_BORDERS_ST = 4
        private const val ZOOM_TO_SHOW_BORDERS = 8
        private const val ZOOM_TO_SHOW_SELECTION_ST = 3
        private const val ZOOM_TO_SHOW_SELECTION = 8
        private const val ZOOM_MIN_TO_SHOW_DOWNLOAD_DIALOG = 9
    }
}