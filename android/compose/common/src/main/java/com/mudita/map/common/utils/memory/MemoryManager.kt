package com.mudita.map.common.utils.memory

interface MemoryManager {
    fun hasEnoughSpace(fileSizeInMegabytes : Double): Boolean
}