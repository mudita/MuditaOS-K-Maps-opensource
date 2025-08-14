package com.mudita.menu.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.map.common.download.HasDownloadErrorsUseCase
import com.mudita.menu.repository.MenuRepository
import com.mudita.menu.repository.model.MenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MenuViewModel @Inject constructor(
    menuRepository: MenuRepository,
    hasDownloadErrorsUseCase: HasDownloadErrorsUseCase,
): ViewModel() {

    val state = MutableStateFlow(MenuState(menuItems = menuRepository.getMenuItems()))

    val hasDownloadErrors: StateFlow<Boolean> = hasDownloadErrorsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    @Immutable
    data class MenuState(
        val menuItems: List<MenuItem>,
    )
}
