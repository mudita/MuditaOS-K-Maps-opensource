package com.mudita.menu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.appBar.KompaktTopAppBar
import com.mudita.map.common.ui.topAppBarHeight
import com.mudita.map.common.R as commonR
import com.mudita.maps.frontitude.R.string
import com.mudita.menu.repository.model.MenuItem

@Composable
fun MenuScreen(
    onItemClicked: (MenuItem?) -> Unit,
) {
    val viewModel: MenuViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val hasDownloadErrors by viewModel.hasDownloadErrors.collectAsState()

    Column(
        modifier = Modifier
            .background(colorWhite)
            .fillMaxSize()
            .imePadding(),
        verticalArrangement = Arrangement.Top,
    ) {
        KompaktTopAppBar(
            title = stringResource(string.maps_common_screentitle_menu),
            titleTextStyle = KompaktTypography900.titleMedium,
            navigationIconResId = commonR.drawable.ic_arrow_left_black,
            barHeight = topAppBarHeight,
            onNavigationIconClick = { onItemClicked(null) },
        )
        state.menuItems.forEachIndexed { index, item ->
            val showAlert = item == MenuItem.ManageMaps && hasDownloadErrors
            MenuItemView(menuItem = item, isLast = index == state.menuItems.lastIndex, showAlert = showAlert) {
                onItemClicked(it)
            }
        }
    }
}