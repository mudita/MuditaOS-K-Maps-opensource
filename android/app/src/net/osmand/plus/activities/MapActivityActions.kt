package net.osmand.plus.activities

import android.content.DialogInterface
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.mudita.maps.R
import net.osmand.GPXUtilities
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet
import net.osmand.plus.profiles.data.RoutingDataUtils
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BooleanUtils.toNotNull
import net.osmand.plus.views.MapActions

class MapActivityActions(mapActivity: MapActivity) : MapActions(mapActivity.myApplication) {
    private val app: OsmandApplication
    private val settings: OsmandSettings?
    private val mapActivity: MapActivity
    private val routingDataUtils: RoutingDataUtils
    private val drawerLogoHeader: ImageView

    init {
        this.app = mapActivity.myApplication
        this.settings = app.settings
        this.mapActivity = mapActivity
        routingDataUtils = RoutingDataUtils(app)
        drawerLogoHeader = ImageView(mapActivity)
        drawerLogoHeader.setPadding(
            -AndroidUtils.dpToPx(mapActivity, 8f),
            AndroidUtils.dpToPx(mapActivity, 16f), 0, 0
        )
    }

    fun addMapMarker(
        latitude: Double,
        longitude: Double,
        pd: PointDescription?,
        mapObjectName: String?
    ) {
        val markersHelper = app.mapMarkersHelper
        markersHelper?.addMapMarker(LatLon(latitude, longitude), pd, mapObjectName)
    }

    protected fun getString(res: Int): String {
        return mapActivity.getString(res)
    }

    override fun hasUiContext(): Boolean {
        return true
    }

    override fun enterRoutePlanningModeGivenGpx(
        gpxFile: GPXUtilities.GPXFile?, appMode: ApplicationMode?, from: LatLon?,
        fromName: PointDescription?, useIntermediatePointsByDefault: Boolean,
        showMenu: Boolean, menuState: Int
    ) {
        super.enterRoutePlanningModeGivenGpx(
            gpxFile, appMode, from, fromName, useIntermediatePointsByDefault,
            showMenu, menuState
        )
        if (settings?.SPEED_CAMERAS_ALERT_SHOWED?.get()?.not().toNotNull()) {
            SpeedCamerasBottomSheet.showInstance(mapActivity.supportFragmentManager, null)
        }
    }

    override fun initVoiceCommandPlayer(mode: ApplicationMode, showMenu: Boolean) {
        app.initVoiceCommandPlayer(mapActivity, mode, null, true, false, false, showMenu)
    }

    @JvmOverloads
    fun stopNavigationActionConfirm(
        listener: DialogInterface.OnDismissListener?,
        onStopAction: Runnable? = null
    ) {}

    fun whereAmIDialog() {
        val items: MutableList<String> = ArrayList()
        items.add(getString(R.string.show_location))
        items.add(getString(R.string.shared_string_show_details))
        val menu = AlertDialog.Builder(mapActivity)
        menu.setItems(
            items.toTypedArray()
        ) { dialog, item ->
            dialog.dismiss()
            when (item) {
                0 -> mapActivity.mapViewTrackingUtilities?.backToLocationImpl()
                1 -> {
                    val locationProvider = app.locationProvider
                    locationProvider?.showNavigationInfo(mapActivity.pointToNavigate, mapActivity)
                }
                else -> {}
            }
        }
        menu.show()
    }

    companion object {
        private val LOG = PlatformUtil.getLog(MapActivityActions::class.java)
        const val KEY_LONGITUDE = "longitude"
        const val KEY_LATITUDE = "latitude"
        const val KEY_NAME = "name"
        const val REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION = 203

        // Constants for determining the order of items in the additional actions context menu
        const val DIRECTIONS_FROM_ITEM_ORDER = 1000
        const val SEARCH_NEAR_ITEM_ORDER = 2000
        const val CHANGE_POSITION_ITEM_ORDER = 3000
        const val EDIT_GPX_WAYPOINT_ITEM_ORDER = 9000
        const val ADD_GPX_WAYPOINT_ITEM_ORDER = 9000
        const val MEASURE_DISTANCE_ITEM_ORDER = 13000
        const val AVOID_ROAD_ITEM_ORDER = 14000
        private const val DRAWER_MODE_NORMAL = 0
        private const val DRAWER_MODE_SWITCH_PROFILE = 1
    }
}