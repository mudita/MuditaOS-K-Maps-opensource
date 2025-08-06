package com.mudita.menu.ui

import com.mudita.map.common.download.HasDownloadErrorsUseCase
import com.mudita.menu.repository.MenuRepository
import com.mudita.menu.repository.model.MenuItem
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
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

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class MenuViewModelTest {

    @RelaxedMockK private lateinit var menuRepository: MenuRepository

    @MockK private lateinit var hasDownloadErrorsUseCase: HasDownloadErrorsUseCase

    private lateinit var viewModel: MenuViewModel

    private val testCoroutineScheduler = TestCoroutineScheduler()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        every { hasDownloadErrorsUseCase() } returns flowOf(false)
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `MenuViewModel is initialized with menu items preloaded`() = runTest {
        // Given
        val menuItems = listOf(
            MenuItem.ManageMaps,
            MenuItem.Navigation,
            MenuItem.Settings,
        )
        every { menuRepository.getMenuItems() } returns menuItems

        // When
        viewModel = MenuViewModel(menuRepository, hasDownloadErrorsUseCase)

        // Then
        assertEquals(menuItems, viewModel.state.value.menuItems)
    }
}
