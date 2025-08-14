package net.osmand.plus.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

fun Context.openLocationSettings() {
    val locationSettingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    this.startActivity(locationSettingsIntent)
}
