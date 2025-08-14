package com.mudita.map.repository

import com.mudita.maps.data.db.dao.SearchHistoryDao
import com.mudita.maps.data.db.entity.SearchHistoryEntity
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class HistoryRepositoryImplTest {

    @MockK
    private lateinit var searchHistoryDao: SearchHistoryDao

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private lateinit var historyRepository: HistoryRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        historyRepository = HistoryRepositoryImpl(
            searchHistoryDao = searchHistoryDao
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given history item, when adding history item fails, then should return failed result`() = runTest {
        // Given
        coEvery { searchHistoryDao.addHistory(any()) } throws IOException()
        val item = SearchHistoryEntity(
            searchQuery = "",
            localName = "Name",
            searchCategory = "Category",
            lat = 75.0,
            lon = 12.3,
            searchTime = 12345678L,
            itemType = "Type",
            iconRes = 1
        )

        // When
        val result = historyRepository.addHistoryItem(item)

        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 1) { searchHistoryDao.addHistory(item) }
    }

    @Test
    fun `Given history item, when adding history item succeeds, then should return success result`() = runTest {
        // Given
        coJustRun { searchHistoryDao.addHistory(any()) }
        val item = SearchHistoryEntity(
            searchQuery = "",
            localName = "Name",
            searchCategory = "Category",
            lat = 75.0,
            lon = 12.3,
            searchTime = 12345678L,
            itemType = "Type",
            iconRes = 1
        )

        // When
        val result = historyRepository.addHistoryItem(item)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { searchHistoryDao.addHistory(item) }
    }
}