package com.mudita.map.common.utils.memory

import android.os.StatFs
import com.mudita.map.common.model.StorageType
import com.mudita.map.common.utils.KEY_CURRENT_STORAGE_DIRECTORY
import com.mudita.map.common.utils.KEY_CURRENT_STORAGE_TYPE
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

class MemoryManagerImpl @Inject constructor(
    @Named(KEY_CURRENT_STORAGE_DIRECTORY) private val storageFolder: Provider<String>,
    @Named(KEY_CURRENT_STORAGE_TYPE) private val storageType: Provider<StorageType>,
) : MemoryManager {

    override fun hasEnoughSpace(fileSizeInMegabytes: Double): Boolean =
        (getAvailableMemorySize() - getStorageSafetyBuffer()) > fileSizeInMegabytes

    private fun getAvailableMemorySize(): Double {
        val path: File = getDataPath()
        val stat = StatFs(path.path)
        val availableMemory: Double = stat.availableBytes / SIZE_OF_MB
        return availableMemory
    }

    private fun getStorageSafetyBuffer(): Double {
        val path: File = getDataPath()
        val stat = StatFs(path.path)

        return if (storageType.get() == StorageType.PHONE) {
            (stat.totalBytes / SIZE_OF_MB) * INTERNAL_STORAGE_SAFETY_BUFFER_PERCENT
        } else {
            0.0
        }
    }

    private fun getDataPath(): File = File(storageFolder.get())

    companion object {
        private const val SIZE_OF_MB = 1024 * 1024.0
        private const val INTERNAL_STORAGE_SAFETY_BUFFER_PERCENT: Double = 0.1
    }
}