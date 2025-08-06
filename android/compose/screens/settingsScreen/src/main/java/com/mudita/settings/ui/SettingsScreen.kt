package com.mudita.settings.ui

import com.mudita.map.common.R as commonR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.appBar.KompaktTopAppBar
import com.mudita.map.common.components.Snackbar
import com.mudita.map.common.model.SettingItemAction
import com.mudita.map.common.model.SettingsData
import com.mudita.map.common.ui.topAppBarHeight
import com.mudita.maps.frontitude.R.string
import com.mudita.settings.ui.utils.OnResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    settingsData: SettingsData,
    isDownloading: Boolean,
    onItemClicked: (SettingItemAction.Selectable) -> Unit,
    onItemSwitched: (SettingItemAction.Switchable) -> Unit,
    onItemChecked: (SettingItemAction.Checkable.CheckableItem) -> Unit,
    onBackClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(key1 = "init_settings") {
        viewModel.initSettings(settingsData)
    }

    OnResumeEffect {
        viewModel.refreshState()
    }

    Scaffold(
        topBar = {
            KompaktTopAppBar(
                title = stringResource(string.common_label_settings),
                titleTextStyle = KompaktTypography900.titleMedium,
                navigationIconResId = commonR.drawable.ic_arrow_left_black,
                onNavigationIconClick = onBackClicked,
                barHeight = topAppBarHeight
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(text = snackbarData.visuals.message)
            }
        },
        containerColor = colorWhite,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
        ) {
            state.settingItems.forEachIndexed { index, item ->
                SettingItemView(
                    settingsData = state.settingsData,
                    settingItem = item,
                    isLast = index == state.settingItems.lastIndex,
                    isDownloading = isDownloading,
                    onItemClicked = {
                        onItemClicked(it)
                    },
                    onItemSwitched = {
                        coroutineScope.launch(Dispatchers.IO) {
                            onItemSwitched(it)
                            viewModel.onItemSwitched(it)
                        }
                    },
                    onItemChecked = {
                        coroutineScope.launch(Dispatchers.IO) {
                            onItemChecked(it)
                            val showStorageChangeConfirmation = viewModel.shouldShowStorageChangeConfirmation(it)
                            viewModel.onItemChecked(it)
                            if (showStorageChangeConfirmation) {
                                snackbarHostState.showSnackbar(context.getString(string.common_toast_storagelocationchanged))
                            }
                        }
                    },
                )
            }
        }
    }
}