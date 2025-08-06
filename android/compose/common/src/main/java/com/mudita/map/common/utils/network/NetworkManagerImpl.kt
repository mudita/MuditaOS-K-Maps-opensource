package com.mudita.map.common.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.mudita.map.common.sharedPrefs.SettingsPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class NetworkManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsPreference: SettingsPreference
) : NetworkManager {

    private val connectivityManager: ConnectivityManager get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isNetworkReachable(networkType: NetworkType): Boolean = when (networkType) {
            NetworkType.WI_FI_ONLY -> isWifiOnlyNetworkReachable()
            NetworkType.ALL -> isNetworkConnected()
        }

    private fun isWifiOnlyNetworkReachable(): Boolean = when {
            isWifiConnected() -> true
            settingsPreference.getWifiOnlyEnabled() && isWifiConnected() -> true
            else -> (settingsPreference.getWifiOnlyEnabled() && !isWifiConnected()).not()
        }

    private fun isWifiConnected(): Boolean = with(connectivityManager) {
        getNetworkCapabilities(activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    }

    private fun isNetworkConnected(): Boolean = with(connectivityManager) {
        getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
                || getNetworkCapabilities(activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
                || isWifiConnected()
    }

    override val networkStatus = callbackFlow {
        if (connectivityManager.activeNetwork == null) {
            trySend(NetworkStatus.Unavailable)
        }
        val networkStatusCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                if (!isNetworkConnected()) {
                    trySend(NetworkStatus.Unavailable)
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkStatusCallback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkStatusCallback)
        }
    }
}

sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Unavailable : NetworkStatus()
}

enum class NetworkType {
    WI_FI_ONLY, ALL
}
