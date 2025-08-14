package com.mudita.download.repository

import com.mudita.download.domain.DownloadingStatePublisher
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadQueueModel
import com.mudita.download.repository.models.LocalIndex
import com.mudita.download.ui.DownloadViewModel
import com.mudita.map.common.utils.memory.MemoryManager
import com.mudita.map.common.utils.network.NetworkManager
import com.mudita.maps.data.api.DownloadApiService
import com.mudita.maps.data.db.dao.DownloadsDao
import com.mudita.maps.data.db.entity.DownloadEntity
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class DownloadRepositoryImplTest {

    @MockK
    private lateinit var downloadApiService: DownloadApiService

    @MockK
    private lateinit var downloadsDao: DownloadsDao

    @MockK
    private lateinit var networkManager: NetworkManager

    @MockK
    private lateinit var memoryManager: MemoryManager

    @MockK
    private lateinit var downloadingStatePublisher: DownloadingStatePublisher

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var downloadRepository: DownloadRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        downloadRepository = DownloadRepositoryImpl(
            downloadApiService = downloadApiService,
            downloadsDao = downloadsDao,
            networkManager = networkManager,
            memoryManager = memoryManager,
            downloadingStatePublisher = downloadingStatePublisher,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given there is a download queue, when getDownloadsQueue is called, then should call dao method and return success result`() = runTest {
        // Given
        coEvery { downloadsDao.getDownloadsQueue() } returns listOf(DOWNLOAD_ENTITY)

        // When
        val result = downloadRepository.getDownloadsQueue()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(DOWNLOAD_QUEUE_MODEL), result.getOrNull())
        coVerify { downloadsDao.getDownloadsQueue() }
    }

    @Test
    fun `Given error getting download queue, when getDownloadsQueue is called, then should return failed result`() = runTest {
        // Given
        coEvery { downloadsDao.getDownloadsQueue() } throws IOException()

        // When
        val result = downloadRepository.getDownloadsQueue()

        // Then
        assertTrue(result.isFailure)
        coVerify { downloadsDao.getDownloadsQueue() }
    }

    @Test
    fun `Given file name, when deleteFromDownloadsDb is called, then should call dao method and return success result`() = runTest {
        // Given
        coJustRun { downloadsDao.deleteFromDownloads(any()) }

        // When
        val result = downloadRepository.deleteFromDownloadsDb(FILE_NAME)

        // Then
        assertTrue(result.isSuccess)
        coVerify { downloadsDao.deleteFromDownloads(FILE_NAME) }
    }

    @Test
    fun `Given file name and dao error, when deleteFromDownloadsDb is called, then should return failed result`() = runTest {
        // Given
        coEvery { downloadsDao.deleteFromDownloads(any()) } throws IOException()

        // When
        val result = downloadRepository.deleteFromDownloadsDb(FILE_NAME)

        // Then
        assertTrue(result.isFailure)
        coVerify { downloadsDao.deleteFromDownloads(FILE_NAME) }
    }

    @Test
    fun `Given download queue model, when addToDownloadsDb is called, then should call dao method and return success result`() = runTest {
        // Given
        coJustRun { downloadsDao.addDownload(any()) }

        // When
        val result = downloadRepository.addToDownloadsDb(DOWNLOAD_QUEUE_MODEL)

        // Then
        assertTrue(result.isSuccess)
        coVerify { downloadsDao.addDownload(DOWNLOAD_ENTITY) }
    }

    @Test
    fun `Given download queue model and dao error, when addToDownloadsDb is called, then should return failed result`() = runTest {
        // Given
        coEvery { downloadsDao.addDownload(any()) } throws IOException()

        // When
        val result = downloadRepository.addToDownloadsDb(DOWNLOAD_QUEUE_MODEL)

        // Then
        assertTrue(result.isFailure)
        coVerify { downloadsDao.addDownload(DOWNLOAD_ENTITY) }
    }

    @Test
    fun `When clearDownloadsDb is called, then should call dao method and return success result`() = runTest {
        // Given
        coJustRun { downloadsDao.clearDownloadsQueue() }

        // When
        val result = downloadRepository.clearDownloadsDb()

        // Then
        assertTrue(result.isSuccess)
        coVerify { downloadsDao.clearDownloadsQueue() }
    }

    @Test
    fun `Given dao error, when clearDownloadsDb is called, then should return failed result`() = runTest {
        // Given
        coEvery { downloadsDao.clearDownloadsQueue() } throws IOException()

        // When
        val result = downloadRepository.clearDownloadsDb()

        // Then
        assertTrue(result.isFailure)
        coVerify { downloadsDao.clearDownloadsQueue() }
    }

    @Test
    fun `Given download item, when deleteMap is called, then should call dao method`() = runTest {
        // Given
        coJustRun { downloadsDao.deleteFromDownloads(any()) }

        // When
        downloadRepository.deleteMap(DOWNLOAD_ITEM)

        // Then
        coVerify { downloadsDao.deleteFromDownloads(DOWNLOAD_ITEM.fileName) }
    }

    @Test
    fun `Given local index, when deleteMap is called, then should remove file`() = runTest {
        // Given
        coJustRun { downloadsDao.deleteFromDownloads(any()) }
        val file: File = mockk() {
            every { length() } returns 12345L
            every { name } returns FILE_NAME
            every { isDirectory } returns false
            every { delete() } returns true
        }
        val localIndex = LocalIndex(
            type = DownloadViewModel.LocalIndexType.MAP_DATA,
            file = file,
            parentName = ""
        )

        // When
        downloadRepository.deleteMap(localIndex)

        // Then
        verify { file.delete() }
        coVerify(exactly = 0) { downloadsDao.deleteFromDownloads(FILE_NAME) }
    }

    companion object {
        private const val FILE_NAME = "Poland_wielkopolskie_europe.zip"

        private val DOWNLOAD_ENTITY = DownloadEntity(
            filename = "Poland_wielkopolskie_europe.zip",
            name = "Wielkopolskie",
            description = "163 MB",
            size = "163 MB",
            targetSize = "273 MB",
            parentName = "Poland",
        )

        private val DOWNLOAD_QUEUE_MODEL = DownloadQueueModel(
            name = "Wielkopolskie",
            description = "163 MB",
            filename = "Poland_wielkopolskie_europe.zip",
            size = "163 MB",
            targetSize = "273 MB",
            parentName = "Poland",
        )

        private val DOWNLOAD_ITEM = DownloadItem(
            name = "Wielkopolskie",
            description = "163 MB",
            fileName = "Poland_wielkopolskie_europe.zip",
            size = "163 MB",
            targetSize = "273 MB",
            downloaded = false
        )
    }
}