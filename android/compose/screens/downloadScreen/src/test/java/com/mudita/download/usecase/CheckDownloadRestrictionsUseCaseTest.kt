package com.mudita.download.usecase

import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.ui.ErrorType
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.map.common.utils.network.NetworkType
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CheckDownloadRestrictionsUseCaseTest {

    @MockK
    private lateinit var networkManager: NetworkManager

    @MockK
    private lateinit var memoryManager: MemoryManager

    private lateinit var checkDownloadRestrictionsUseCase: CheckDownloadRestrictionsUseCase

    @BeforeEach
    fun setup() {
        checkDownloadRestrictionsUseCase = CheckDownloadRestrictionsUseCase(
            networkManager = networkManager,
            memoryManager = memoryManager,
        )
    }

    @Test
    fun `Given wifi only option and no wifi available, when checkDownloadRestrictionsUseCase invoked, should return WifiNetwork error`() {
        // Given
        every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns false

        // When
        val result = checkDownloadRestrictionsUseCase(DUMMY_DOWNLOAD_ITEM)

        // Then
        assertEquals(ErrorType.WifiNetwork, result)
    }

    @Test
    fun `Given no internet available, when checkDownloadRestrictionsUseCase invoked, should return Network error`() {
        // Given
        every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns true
        every { networkManager.isNetworkReachable(NetworkType.ALL) } returns false

        // When
        val result = checkDownloadRestrictionsUseCase(DUMMY_DOWNLOAD_ITEM)

        // Then
        assertEquals(ErrorType.Network, result)
    }

    @Test
    fun `Given no memory available, when checkDownloadRestrictionsUseCase invoked, should return Memory error`() {
        // Given
        every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns true
        every { networkManager.isNetworkReachable(NetworkType.ALL) } returns true
        every { memoryManager.hasEnoughSpace(any()) } returns false

        // When
        val result = checkDownloadRestrictionsUseCase(DUMMY_DOWNLOAD_ITEM)

        // Then
        assertEquals(ErrorType.Memory, result)
    }

    @Test
    fun `Given no network or memory error, when checkDownloadRestrictionsUseCase invoked, should return null`() {
        // Given
        every { networkManager.isNetworkReachable(NetworkType.WI_FI_ONLY) } returns true
        every { networkManager.isNetworkReachable(NetworkType.ALL) } returns true
        every { memoryManager.hasEnoughSpace(any()) } returns true

        // When
        val result = checkDownloadRestrictionsUseCase(DUMMY_DOWNLOAD_ITEM)

        // Then
        assertNull(result)
    }

    companion object {
        private val DUMMY_DOWNLOAD_ITEM = DownloadItem(
            name = "Albania",
            description = "48 MB",
            fileName = "Albania_europe.zip",
            size = "48 MB",
            targetSize = "73 MB",
            downloaded = false
        )
    }
}