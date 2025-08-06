package net.osmand.plus.views

import android.content.DialogInterface
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import com.mudita.maps.R
import net.osmand.CallbackWithObject
import net.osmand.GPXUtilities
import net.osmand.IndexConstants
import net.osmand.ResultMatcher
import net.osmand.StateChangedListener
import net.osmand.map.TileSourceManager
import net.osmand.plus.DialogListItemAdapter
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.GpxUiHelper
import net.osmand.plus.measurementtool.MeasurementToolLayer
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.resources.SQLiteTileSource
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.layers.ContextMenuLayer
import net.osmand.plus.views.layers.DownloadedRegionsLayer
import net.osmand.plus.views.layers.GPXLayer
import net.osmand.plus.views.layers.MapMarkersLayer
import net.osmand.plus.views.layers.MapTextLayer
import net.osmand.plus.views.layers.MapTileLayer
import net.osmand.plus.views.layers.MapVectorLayer
import net.osmand.plus.views.layers.POIMapLayer
import net.osmand.plus.views.layers.PointLocationLayer
import net.osmand.plus.views.layers.PointNavigationLayer
import net.osmand.plus.views.layers.PreviewRouteLineLayer
import net.osmand.plus.views.layers.RouteLayer
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.ViewCreator
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem

/**
 * Object is responsible to maintain layers using by map activity
 */
class MapLayers(private val app: OsmandApplication) {
    // the order of layer should be preserved ! when you are inserting new layer
    val mapTileLayer: MapTileLayer by lazy {
        MapTileLayer(app, true)
    }
    val mapVectorLayer: MapVectorLayer by lazy {
        MapVectorLayer(app)
    }
    val gpxLayer: GPXLayer by lazy {
        GPXLayer(app)
    }
    val routeLayer: RouteLayer by lazy {
        RouteLayer(app)
    }
    val previewRouteLineLayer: PreviewRouteLineLayer by lazy {
        PreviewRouteLineLayer(app)
    }
    val poiMapLayer: POIMapLayer? by lazy {
        // POIMapLayer(app) // NOT USED AT THE MOMENT
        null
    }
    val locationLayer: PointLocationLayer by lazy {
        PointLocationLayer(app)
    }
    val navigationLayer: PointNavigationLayer by lazy {
        PointNavigationLayer(app)
    }
    val mapMarkersLayer: MapMarkersLayer by lazy {
        MapMarkersLayer(app)
    }
    private val mapTextLayer: MapTextLayer by lazy {
        MapTextLayer(app)
    }
    val contextMenuLayer: ContextMenuLayer by lazy {
        ContextMenuLayer(app)
    }
    val downloadedRegionsLayer: DownloadedRegionsLayer by lazy {
        DownloadedRegionsLayer(app)
    }
    val measurementToolLayer: MeasurementToolLayer by lazy {
        MeasurementToolLayer(app)
    }

    fun createLayers(mapView: OsmandMapTileView) {
        // first create to make accessible
        // 5.95 all labels
        mapView.addLayer(mapTextLayer, 5.95f)
        // 8. context menu layer
        mapView.addLayer(contextMenuLayer, 8f)
        // mapView.addLayer(underlayLayer, -0.5f);
        mapView.addLayer(mapTileLayer, 0.05f)
        mapView.setMainLayer(mapTileLayer)
        // 1-st in the order
        mapView.addLayer(downloadedRegionsLayer, 0.5f, -11.0f)
        // icons are 2-d in the order (icons +1 000 000 or -10.f by zOrder in core)
        // text and shields are 5-th in the order
        mapView.addLayer(mapVectorLayer, 0.0f)
        // route layer, 4-th in the order
        mapView.addLayer(routeLayer, 1.0f, -2.0f)
        // 1.5 preview route line layer
        mapView.addLayer(previewRouteLineLayer, 1.5f)
        // 2. osm bugs layer
        // 3. poi layer
        // mapView.addLayer(poiMapLayer, 3f) // NOT USED AT THE MOMENT
        // 5.95 all text labels
        // 6. point location layer
        mapView.addLayer(locationLayer, 6f)
        // 7. point navigation layer
        mapView.addLayer(navigationLayer, 7f)
        // 7.3 map markers layer
        mapView.addLayer(mapMarkersLayer, 7.3f)
        val transparencyListener =
            StateChangedListener { change: Int ->
                app.runInUIThread {
                    mapTileLayer.alpha = change
                    mapVectorLayer.alpha = change
                    mapView.refreshMap()
                }
            }
        val overlayTransparencyListener =
            StateChangedListener { change: Int ->
                app.runInUIThread {
                    mapView.mapRenderer?.let {
                        mapTileLayer.alpha = 255 - change
                        mapVectorLayer.alpha = 255 - change
                        it.requestRender()
                    }
                }
            }
        app.settings?.apply {
            MAP_TRANSPARENCY.addListener(transparencyListener)
            MAP_OVERLAY_TRANSPARENCY.addListener(overlayTransparencyListener)
        }
        createAdditionalLayers(null)
    }

    fun createAdditionalLayers(mapActivity: MapActivity?) {
        PluginsHelper.createLayers(app, mapActivity)
        app.appCustomization.createLayers(app, mapActivity)
        app.aidlApi.registerMapLayers(app)
    }

    fun setMapActivity(mapActivity: MapActivity?) {
        app.osmandMap?.let {
            val mapView = it.mapView
            for (layer in mapView.getLayers()) {
                val layerMapActivity = layer.mapActivity
                if (mapActivity != null && layerMapActivity != null) {
                    layer.mapActivity = null
                }
                layer.mapActivity = mapActivity
            }
            val mapRenderer = mapView.mapRenderer
            mapRenderer?.removeAllSymbolsProviders()
        }
    }

    fun hasMapActivity(): Boolean {
        app.osmandMap?.let {
            val mapView = it.mapView
            for (layer in mapView.getLayers()) {
                if (layer.mapActivity != null) {
                    return true
                }
            }
        }
        return false
    }

    fun updateLayers(mapActivity: MapActivity?) {
        val settings = app.settings
        val mapView = app.osmandMap?.mapView
        if (mapView != null && settings != null) {
            updateMapSource(mapView, settings.MAP_TILE_SOURCES)
            PluginsHelper.refreshLayers(app, mapActivity)
        }
    }

    fun updateMapSource(
        mapView: OsmandMapTileView,
        settingsToWarnAboutMap: CommonPreference<String?>?
    ) {
        val settings = app.settings
        val useOpenGLRender = app.useOpenGlRenderer()

        // update transparency
        settings?.let {
            var mapTransparency = 255
            if (it.MAP_UNDERLAY.get() != null) {
                mapTransparency = it.MAP_TRANSPARENCY.get()
            } else if (useOpenGLRender && it.MAP_OVERLAY.get() != null) {
                mapTransparency = 255 - it.MAP_OVERLAY_TRANSPARENCY.get()
            }
            mapTileLayer.alpha = mapTransparency
            mapVectorLayer.alpha = mapTransparency
            val newSource =
                it.getMapTileSource(it.MAP_TILE_SOURCES === settingsToWarnAboutMap)
            val oldMap = mapTileLayer.map
            if (newSource !== oldMap) {
                if (oldMap is SQLiteTileSource) {
                    oldMap.closeDB()
                }
                mapTileLayer.map = newSource
            }
            val vectorData = !it.MAP_ONLINE_DATA.get()
            mapTileLayer.isVisible = !vectorData
            mapVectorLayer.setVisible(vectorData)
            if (vectorData) {
                mapView.setMainLayer(mapVectorLayer)
            } else {
                mapView.setMainLayer(mapTileLayer)
            }
        }
    }

    fun showGPXFileLayer(files: List<String?>, mapActivity: MapActivity): AlertDialog {
        val settings = app.settings
        val mapView = mapActivity.mapView
        val dashboard = mapActivity.dashboard
        val callbackWithObject =
            CallbackWithObject { result: Array<GPXUtilities.GPXFile> ->
                var locToShow: GPXUtilities.WptPt? = null
                for (g in result) {
                    locToShow = if (g.showCurrentTrack) {
                        settings?.let {
                            if (!it.SAVE_TRACK_TO_GPX.get() && !it.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
                                app.showToastMessage(R.string.gpx_monitoring_disabled_warn)
                            }
                        }
                        break
                    } else {
                        g.findPointToShow()
                    }
                }
                app.selectedGpxHelper?.setGpxFileToDisplay(*result)
                mapView?.let { mView ->
                    locToShow?.let {
                        mView.animatedDraggingThread?.startMoving(
                            it.lat, it.lon,
                            mView.zoom, true
                        )
                    }
                    mView.refreshMap()
                }
                dashboard.refreshContent(false)
                true
            }
        return GpxUiHelper.selectGPXFiles(
            files, mapActivity, callbackWithObject,
            themeRes,
            isNightMode
        )
    }

    fun showMultiChoicePoiFilterDialog(mapActivity: MapActivity, listener: DismissListener) {
        val poiFilters = app.poiFilters
        val adapter = ContextMenuAdapter(app)
        val list = mutableListOf<PoiUIFilter?>()
        poiFilters?.let {
            for (f in poiFilters.getSortedPoiFilters(true)) {
                if (!f.isTopWikiFilter
                    && !f.isRoutesFilter
                    && !f.isRouteArticleFilter
                    && !f.isRouteArticlePointFilter
                    && !f.isCustomPoiFilter
                ) {
                    addFilterToList(adapter, list, f, true)
                }
            }
        }
        val appMode = app.settings?.applicationMode
        val viewCreator = ViewCreator(mapActivity, isNightMode)
        viewCreator.setCustomControlsColor(appMode?.getProfileColor(isNightMode))
        val listAdapter = adapter.toListAdapter(mapActivity, viewCreator)
        val themedContext = UiUtilities.getThemedContext(
            mapActivity,
            isNightMode
        )
        val builder = AlertDialog.Builder(themedContext)
        val listView = ListView(themedContext)
        listView.divider = null
        listView.isClickable = true
        listView.adapter = listAdapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, view: View?, position: Int, _ ->
                val item = listAdapter.getItem(position)
                item?.let {
                    it.selected = !it.selected
                    val clickListener = it.itemClickListener
                    clickListener?.onContextMenuClick(listAdapter, view, it, it.selected)
                    listAdapter.notifyDataSetChanged()
                }
            }
        builder.setView(listView)
            .setTitle(R.string.show_poi_over_map)
            .setPositiveButton(R.string.shared_string_ok) { _, _ ->
                for (i in 0 until listAdapter.count) {
                    val item = listAdapter.getItem(i)
                    val filter = list[i]
                    if (item != null && item.selected) {
                        filter?.let {
                            if (filter.isStandardFilter) {
                                filter.removeUnsavedFilterByName()
                            }
                        }
                        poiFilters?.addSelectedPoiFilter(filter)
                    } else {
                        poiFilters?.removeSelectedPoiFilter(filter)
                    }
                }
                mapActivity.mapView?.refreshMap()
            }
            .setNegativeButton(R.string.shared_string_cancel, null)
            .setNeutralButton(
                " "
            ) { _, _ ->
                showSingleChoicePoiFilterDialog(
                    mapActivity,
                    listener
                )
            }
        val alertDialog = builder.create()
        alertDialog.setOnShowListener {
            val neutralButton =
                alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            val drawable =
                app.uIUtilities.getThemedIcon(R.drawable.ic_action_singleselect)
            neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            neutralButton.contentDescription = app.getString(R.string.shared_string_filters)
        }
        alertDialog.setOnDismissListener { listener.dismiss() }
        alertDialog.show()
    }

    fun showSingleChoicePoiFilterDialog(mapActivity: MapActivity, listener: DismissListener) {
        val poiFilters = app.poiFilters
        val adapter = ContextMenuAdapter(app)
        adapter.addItem(
            ContextMenuItem(null)
                .setTitleId(R.string.shared_string_search, app)
                .setIcon(R.drawable.ic_action_search_dark)
        )
        val list = mutableListOf<PoiUIFilter?>(null)
        poiFilters?.let {
            for (f in poiFilters.getSortedPoiFilters(true)) {
                if (!f.isTopWikiFilter
                    && !f.isRoutesFilter
                    && !f.isRouteArticleFilter
                    && !f.isRouteArticlePointFilter
                    && !f.isCustomPoiFilter
                ) {
                    addFilterToList(adapter, list, f, false)
                }
            }
        }
        val appMode = app.settings?.applicationMode
        val viewCreator = ViewCreator(mapActivity, isNightMode)
        viewCreator.setCustomControlsColor(appMode?.getProfileColor(isNightMode))
        val listAdapter = adapter.toListAdapter(mapActivity, viewCreator)
        val themedContext = UiUtilities.getThemedContext(
            mapActivity,
            isNightMode
        )
        val builder = AlertDialog.Builder(themedContext)
        builder.setAdapter(listAdapter) { _, which: Int ->
            val filter = list[which]
            if (filter == null) {
                if (mapActivity.dashboard.isVisible) {
                    mapActivity.dashboard.hideDashboard()
                }
            } else {
                if (filter.isStandardFilter) {
                    filter.removeUnsavedFilterByName()
                }
                poiFilters?.let {
                    val wiki = poiFilters.topWikiPoiFilter
                    poiFilters.clearSelectedPoiFilters(wiki)
                    poiFilters.addSelectedPoiFilter(filter)
                    updateRoutingPoiFiltersIfNeeded()
                    mapActivity.mapView?.refreshMap()
                }
            }
        }
        builder.setTitle(R.string.show_poi_over_map)
        builder.setNegativeButton(R.string.shared_string_dismiss, null)
        builder.setNeutralButton(
            " "
        ) { _, _ ->
            showMultiChoicePoiFilterDialog(
                mapActivity,
                listener
            )
        }
        val alertDialog = builder.create()
        alertDialog.setOnShowListener {
            val neutralButton =
                alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            val drawable =
                app.uIUtilities.getThemedIcon(R.drawable.ic_action_multiselect)
            neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            neutralButton.contentDescription = app.getString(R.string.apply_filters)
        }
        alertDialog.setOnDismissListener { listener.dismiss() }
        alertDialog.show()
    }

    private fun addFilterToList(
        adapter: ContextMenuAdapter,
        list: MutableList<PoiUIFilter?>,
        f: PoiUIFilter,
        multiChoice: Boolean
    ) {
        list.add(f)
        val item = ContextMenuItem(null)
        if (multiChoice) {
            app.poiFilters?.let {
                item.selected = it.isPoiFilterSelected(f)
            }
            item.setListener { _, _, it: ContextMenuItem, isChecked: Boolean ->
                it.selected = isChecked
                false
            }
        }
        item.title = f.name
        if (RenderingIcons.containsBigIcon(f.iconId)) {
            item.icon = RenderingIcons.getBigIconResourceId(f.iconId)
        } else {
            item.icon = com.mudita.map.common.R.drawable.mx_mudita_default
        }
        item.setColor(app, ContextMenuItem.INVALID_ID)
        item.setUseNaturalIconColor(true)
        adapter.addItem(item)
    }

    fun selectMapLayer(
        mapActivity: MapActivity,
        item: ContextMenuItem,
        uiAdapter: OnDataChangeUiAdapter
    ) {
        selectMapLayer(mapActivity, true) { mapSourceName: String? ->
            item.description = mapSourceName
            uiAdapter.onDataSetChanged()
            true
        }
    }

    fun selectMapLayer(
        mapActivity: MapActivity,
        includeOfflineMaps: Boolean,
        callback: CallbackWithObject<String?>?
    ) {
        if (!PluginsHelper.isActive(OsmandRasterMapsPlugin::class.java)) {
            app.showToastMessage(R.string.map_online_plugin_is_not_installed)
            return
        }
        val settings = app.settings
        settings?.let {
            val entriesMap: MutableMap<String, String> = linkedMapOf()
            val layerOsmVector = "LAYER_OSM_VECTOR"
            val layerInstallMore = "LAYER_INSTALL_MORE"
            val layerAdd = "LAYER_ADD"
            if (includeOfflineMaps) {
                entriesMap[layerOsmVector] = getString(R.string.vector_data)
            }
            entriesMap.putAll(settings.tileSourceEntries)
            entriesMap[layerInstallMore] = getString(R.string.install_more)
            entriesMap[layerAdd] = getString(R.string.shared_string_add)
            val entriesMapList: MutableList<Map.Entry<String, String>> = ArrayList(entriesMap.entries)
            val selectedTileSourceKey = settings.MAP_TILE_SOURCES.get()
            var selectedItem = -1
            if (!settings.MAP_ONLINE_DATA.get()) {
                selectedItem = 0
            } else {
                var selectedEntry: Map.Entry<String, String>? = null
                for (entry in entriesMap.entries) {
                    if (entry.key == selectedTileSourceKey) {
                        selectedEntry = entry
                        break
                    }
                }
                if (selectedEntry != null) {
                    selectedItem = 0
                    entriesMapList.remove(selectedEntry)
                    entriesMapList.add(0, selectedEntry)
                }
            }
            val items = arrayOfNulls<String>(entriesMapList.size)
            var i = 0
            for (entry: Map.Entry<String, String> in entriesMapList) {
                items[i++] = entry.value
            }
            val nightMode = isNightMode
            val themeRes = themeRes
            val selectedModeColor = settings.applicationMode.getProfileColor(nightMode)
            val dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
                items, nightMode, selectedItem, app, selectedModeColor, themeRes
            ) { v: View ->
                val which = v.tag as Int
                when (val layerKey = entriesMapList[which].key) {
                    layerOsmVector -> {
                        settings.MAP_ONLINE_DATA.set(false)
                        mapActivity.mapView?.let {
                            updateMapSource(it, null)
                        }
                        callback?.processResult(null)
                    }
                    layerInstallMore -> OsmandRasterMapsPlugin.installMapLayers(
                        mapActivity,
                        object :
                            ResultMatcher<TileSourceManager.TileSourceTemplate?> {
                            var template: TileSourceManager.TileSourceTemplate? =
                                null
                            var count = 0
                            override fun publish(obj: TileSourceManager.TileSourceTemplate?): Boolean {
                                if (obj == null) {
                                    if (count == 1) {
                                        template?.let { sourceTemplate ->
                                            settings.MAP_TILE_SOURCES.set(sourceTemplate.name)
                                            settings.MAP_ONLINE_DATA.set(true)
                                            mapActivity.mapView?.let {
                                                updateMapSource(
                                                    it,
                                                    settings.MAP_TILE_SOURCES
                                                )
                                            }
                                            callback?.processResult(sourceTemplate.name)
                                        }
                                    } else {
                                        selectMapLayer(mapActivity, includeOfflineMaps, callback)
                                    }
                                } else {
                                    count++
                                    template = obj
                                }
                                return false
                            }

                            override fun isCancelled(): Boolean {
                                return false
                            }
                        })
                    else -> {
                        settings.MAP_TILE_SOURCES.set(layerKey)
                        settings.MAP_ONLINE_DATA.set(true)
                        mapActivity.mapView?.let {
                            updateMapSource(it, settings.MAP_TILE_SOURCES)
                        }
                        callback?.processResult(
                            layerKey.replace(
                                IndexConstants.SQLITE_EXT,
                                ""
                            )
                        )
                    }
                }
            }
            val themedContext = UiUtilities.getThemedContext(
                mapActivity,
                isNightMode
            )
            val builder = AlertDialog.Builder(themedContext)
            builder.setAdapter(dialogAdapter, null)
            builder.setNegativeButton(R.string.shared_string_dismiss, null)
            dialogAdapter.setDialog(builder.show())
        }
    }

    private fun updateRoutingPoiFiltersIfNeeded() {
        val settings = app.settings
        settings?.let {
            val routingHelper = app.routingHelper
            routingHelper?.let {
                val usingRouting = (routingHelper.isFollowingMode || routingHelper.isRoutePlanningMode
                            || routingHelper.isRouteBeingCalculated || routingHelper.isRouteCalculated)
                val routingMode = routingHelper.appMode
                if (usingRouting && routingMode !== settings.applicationMode) {
                    settings.setSelectedPoiFilters(routingMode, settings.selectedPoiFilters)
                }
            }
        }
    }

    private val isNightMode: Boolean = false

    @get:StyleRes
    private val themeRes: Int
        get() = if (isNightMode) R.style.OsmandDarkTheme else R.style.OsmandLightTheme

    private fun getString(resId: Int): String {
        return app.getString(resId)
    }

    interface DismissListener {
        fun dismiss()
    }
}