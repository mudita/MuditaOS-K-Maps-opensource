package com.mudita.map.common.utils.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface NetworkManager {
    val networkStatus: Flow<NetworkStatus>
    fun isNetworkReachable(networkType: NetworkType = NetworkType.ALL): Boolean
    suspend fun awaitNetworkAvailable() = networkStatus.first {
        it == NetworkStatus.Available && isNetworkReachable(NetworkType.WI_FI_ONLY) &&
                isNetworkReachable(NetworkType.ALL)
    }
}