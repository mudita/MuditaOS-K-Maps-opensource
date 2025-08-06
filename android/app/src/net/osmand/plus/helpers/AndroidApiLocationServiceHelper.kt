package net.osmand.plus.helpers

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import java.util.LinkedList
import net.osmand.PlatformUtil
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication

class AndroidApiLocationServiceHelper(
    private val app: OsmandApplication
) : LocationServiceHelper(), LocationListener {
    private var locationCallback: LocationCallback? = null
    private var networkLocationCallback: LocationCallback? = null
    private val networkListeners = LinkedList<LocationListener>()

    // Working with location checkListeners
    private inner class NetworkListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            networkLocationCallback?.let { locationCallback ->
                val l = convertLocation(location)
                locationCallback.onLocationResult(l?.let { listOf(it) } ?: emptyList())
            }
        }

        override fun onProviderDisabled(provider: String) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        @Deprecated("Deprecated in Java", ReplaceWith("Unit"))
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) = Unit
    }

    override fun requestLocationUpdates(locationCallback: LocationCallback) {
        this.locationCallback = locationCallback
        // request location updates
        val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        } catch (e: SecurityException) {
            LOG.debug("Location service permission not granted")
            throw e
        } catch (e: IllegalArgumentException) {
            LOG.debug("GPS location provider not available")
            throw e
        }
    }

    override fun isNetworkLocationUpdatesSupported(): Boolean = true

    override fun requestNetworkLocationUpdates(locationCallback: LocationCallback) {
        networkLocationCallback = locationCallback
        // request location updates
        val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            if (provider == null || provider == LocationManager.GPS_PROVIDER) {
                continue
            }
            try {
                val networkListener = NetworkListener()
                locationManager.requestLocationUpdates(provider, 0, 0f, networkListener)
                networkListeners.add(networkListener)
            } catch (e: SecurityException) {
                LOG.debug("$provider location service permission not granted")
            } catch (e: IllegalArgumentException) {
                LOG.debug("$provider location provider not available")
            }
        }
    }

    override fun removeLocationUpdates() {
        // remove location updates
        val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            LOG.debug("Location service permission not granted", e)
            throw e
        } finally {
            while (!networkListeners.isEmpty()) {
                val listener = networkListeners.poll()
                if (listener != null) {
                    locationManager.removeUpdates(listener)
                }
            }
        }
    }

    override fun getFirstTimeRunDefaultLocation(locationCallback: LocationCallback?): net.osmand.Location? {
        val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers: MutableList<String> = ArrayList(locationManager.getProviders(true))
        // note, passive provider is from API_LEVEL 8 but it is a constant, we can check for it.
        // constant should not be changed in future
        val passiveFirst = providers.indexOf(LocationManager.PASSIVE_PROVIDER)
        // put passive provider to first place
        if (passiveFirst > -1) {
            providers.add(0, providers.removeAt(passiveFirst))
        }
        // find location
        providers.forEach { provider ->
            try {
                val location = convertLocation(
                    locationManager.getLastKnownLocation(
                        provider
                    )
                )
                if (location != null) {
                    return location
                }
            } catch (e: SecurityException) {
                // location service permission not granted
            } catch (e: IllegalArgumentException) {
                // location provider not available
            }
        }
        return null
    }

    private fun convertLocation(location: Location?): net.osmand.Location? {
        return if (location == null) null else OsmAndLocationProvider.convertLocation(location, app)
    }

    override fun onLocationChanged(location: Location) {
        val locationCallback = locationCallback
        if (locationCallback != null) {
            val l = convertLocation(location)
            locationCallback.onLocationResult(l?.let { listOf(it) } ?: emptyList())
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("Unit"))
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) = Unit
    override fun onProviderEnabled(provider: String) {
        val locationCallback = locationCallback
        locationCallback?.onLocationAvailability(true)
    }

    override fun onProviderDisabled(provider: String) {
        val locationCallback = locationCallback
        locationCallback?.onLocationAvailability(false)
    }

    companion object {
        private val LOG = PlatformUtil.getLog(AndroidApiLocationServiceHelper::class.java)
    }
}