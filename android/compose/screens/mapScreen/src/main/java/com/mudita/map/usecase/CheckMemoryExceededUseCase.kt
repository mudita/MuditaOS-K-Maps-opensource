package com.mudita.map.usecase

import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class CheckMemoryExceededUseCase @Inject constructor() {
    suspend operator fun invoke(): Boolean {
        val runtime = Runtime.getRuntime()
        while (currentCoroutineContext().isActive) {
            if (runtime.maxMemory() == runtime.totalMemory() &&
                runtime.freeMemory() / BYTES_IN_MB < FREE_MEMORY_LIMIT_MB
            ) {
                return true
            }
            delay(MEMORY_CHECK_FREQUENCY)
        }
        return false
    }

    companion object {
        private const val MEMORY_CHECK_FREQUENCY = 1_000L
        private const val BYTES_IN_MB = 1024 * 1024.0
        private const val FREE_MEMORY_LIMIT_MB = 10
    }
}