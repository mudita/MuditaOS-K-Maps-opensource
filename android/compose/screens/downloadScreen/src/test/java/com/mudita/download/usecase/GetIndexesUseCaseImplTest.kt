package com.mudita.download.usecase

import app.cash.turbine.test
import com.mudita.download.domain.GetIndexesUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.ResourceGroup
import com.mudita.map.common.region.GetRegionsIndexedEvents
import com.mudita.maps.data.api.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class GetIndexesUseCaseImplTest {

    @MockK
    private lateinit var downloadRepository: DownloadRepository

    @MockK
    private lateinit var getRegionsIndexedEvents: GetRegionsIndexedEvents

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var getIndexesUseCase: GetIndexesUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        getIndexesUseCase = GetIndexesUseCaseImpl(
            downloadRepository = downloadRepository,
            getRegionsIndexedEvents = getRegionsIndexedEvents,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given indexes list successfully fetched from repository, when getIndexesUseCase is invoked, then should return this list`() = runTest {
        // Given
        val successResult = Resource.Success(poland)
        every { downloadRepository.getIndexesList() } returns flowOf(successResult)
        every { getRegionsIndexedEvents() } returns flowOf(Unit)

        // When
        getIndexesUseCase().test {
            // Then
            assertEquals(successResult, awaitItem())
            awaitComplete()
        }
        coVerify(exactly = 1) { downloadRepository.getIndexesList() }
    }

    @Test
    fun `Given indexes list fetch from repository failed, when getIndexesUseCase is invoked, then should return failed result`() = runTest {
        // Given
        val errorResult = Resource.Error<ResourceGroup>(UnknownHostException())
        coEvery { downloadRepository.getIndexesList() } returns flowOf(errorResult)
        every { getRegionsIndexedEvents() } returns flowOf(Unit)

        // When
        getIndexesUseCase().test {
            // Then
            assertEquals(errorResult, awaitItem())
            awaitComplete()
        }
        coVerify(exactly = 1) { downloadRepository.getIndexesList() }
    }

    companion object {
        private val masovian = DownloadItem(
            name = "Mazowieckie",
            description = "297 MB",
            fileName = "Poland_mazowieckie_europe.zip",
            size = "297 MB",
            targetSize = "479 MB",
            downloaded = false
        )

        private val greaterPoland = DownloadItem(
            name = "Wielkopolskie",
            description = "163 MB",
            fileName = "Poland_wielkopolskie_europe.zip",
            size = "163 MB",
            targetSize = "273 MB",
            downloaded = true
        )

        private val poland = ResourceGroup(
            name = "Poland",
            parents = listOf("europe"),
            individualDownloadItems = mutableListOf(
                masovian,
                greaterPoland
            )
        )
    }
}