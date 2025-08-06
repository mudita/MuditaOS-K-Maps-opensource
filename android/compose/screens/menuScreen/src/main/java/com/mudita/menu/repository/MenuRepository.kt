package com.mudita.menu.repository

import com.mudita.menu.repository.model.MenuItem

interface MenuRepository {

    fun getMenuItems(): List<MenuItem>
}
