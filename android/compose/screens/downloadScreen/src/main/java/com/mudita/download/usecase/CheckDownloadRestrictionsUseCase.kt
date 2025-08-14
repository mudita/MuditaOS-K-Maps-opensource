package com.mudita.download.usecase

import com.mudita.download.repository.models.Downloadable
import com.mudita.download.repository.models.getFullDownloadSize
import com.mudita.download.ui.ErrorType
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkType
import javax.inject.Inject

class CheckDownloadRestrictionsUseCase @Inject constructor(
    private val networkManager: NetworkManager,
    private val memoryManager: MemoryManager,
) {
    operator fun invoke(downloadable: Downloadable): ErrorType? = when {
        !networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) -> ErrorType.WifiNetwork
        !networkManager.isNetworkReachable(NetworkType.ALL) -> ErrorType.Network
        !memoryManager.hasEnoughSpace(downloadable.getFullDownloadSize()) -> ErrorType.Memory
        else -> null
    }
}
