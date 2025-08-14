package com.mudita.menu.repository

import com.mudita.map.common.BuildConfig
import com.mudita.menu.repository.model.MenuItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MenuRepositoryImplTest {

    private lateinit var menuRepository: MenuRepository

    @BeforeEach
    fun setup() {
        menuRepository = MenuRepositoryImpl()
    }

    @Test
    fun `When getMenuItems called, should return list of menu items`() {
        // When
        val result = menuRepository.getMenuItems()

        // Then
        if (BuildConfig.IS_PLAN_ROUTE_ENABLED) {
            assertEquals(3, result.size)
            assertEquals(listOf(MenuItem.ManageMaps, MenuItem.Navigation, MenuItem.Settings), result)
        } else {
            assertEquals(2, result.size)
            assertEquals(listOf(MenuItem.ManageMaps, MenuItem.Settings), result)
        }
    }
}