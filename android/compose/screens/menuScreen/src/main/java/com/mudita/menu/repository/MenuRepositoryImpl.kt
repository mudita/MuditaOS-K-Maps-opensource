package com.mudita.menu.repository

import com.mudita.map.common.BuildConfig
import com.mudita.menu.repository.model.MenuItem
import javax.inject.Inject

class MenuRepositoryImpl @Inject constructor() : MenuRepository {

    override fun getMenuItems(): List<MenuItem> = listOfNotNull(
        MenuItem.ManageMaps,
        if (BuildConfig.IS_PLAN_ROUTE_ENABLED) MenuItem.Navigation else null,
        MenuItem.Settings,
    )
}