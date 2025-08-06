package com.mudita.myplaces.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.myplaces.R
import com.mudita.myplaces.repository.MyPlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MyPlacesViewModel @Inject constructor(
    private val myPlacesRepository: MyPlacesRepository
) : ViewModel() {

    val state = MutableStateFlow(MyPlacesState())

    fun initAddMyPlace(myPlaceItem: MyPlaceItem?, isEditMode: Boolean) {
        Timber.i("initAddMyPlace: $myPlaceItem, isEditMode: $isEditMode")
        state.update {
            it.copy(
                myPlaceItem = myPlaceItem,
                isEditMode = isEditMode,
                isAddMyPlaceEnabled = if (isEditMode) false // name or address should differ from existing in order to save
                else myPlaceItem?.title.isNullOrEmpty().not()
            )
        }
    }

    fun onSearchTextChanged(text: String) = viewModelScope.launch {
        state.update { it.copy(searchedText = text) }

        if (text.isEmpty()) {
            getMyPlaces()
        } else {
            myPlacesRepository.searchMyPlaces(text).onSuccess { myPlaces ->
                state.update { it.copy(items = myPlaces) }
            }
        }
    }

    fun onSearchActivate() {
        state.update { it.copy(isSearchActive = true) }
    }

    fun onSearchDeactivate() {
        state.update {
            it.copy(
                isSearchActive = false,
                searchedText = ""
            )
        }
    }

    fun getMyPlaces() {
        viewModelScope.launch {
            myPlacesRepository.getMyPlaces().onSuccess { myPlaces ->
                state.update { it.copy(items = myPlaces) }
            }.onFailure {
                Timber.e("getMyPlaces failure: $it")
            }
        }
    }

    fun addMyPlace(item: MyPlaceItem) {
        viewModelScope.launch { myPlacesRepository.addMyPlace(item) }
    }

    fun updateMyPlace(item: MyPlaceItem) {
        viewModelScope.launch {
            myPlacesRepository.updateMyPlace(item).onSuccess {
                Timber.d("updateMyPlace success: $it")
            }.onFailure {
                Timber.e("updateMyPlace failure: $it")
            }
        }
    }

    fun validate(name: String) {
        val isAddMyPlaceEnabled = validateName(name)

        state.update {
            it.copy(
                isAddMyPlaceEnabled = isAddMyPlaceEnabled
            )
        }
    }

    private fun validateName(name: String): Boolean {
        state.value.myPlaceItem ?: return false
        return name.isNotEmpty()
    }

    fun deleteMyPlace() = state.value.myPlaceItem?.let { item ->
        viewModelScope.launch {
            myPlacesRepository.deleteMyPlace(item)
        }
    }

    fun showLeaveBottomSheet() {
        state.update { it.copy(confirmBottomSheetType = ConfirmBottomSheetType.ShowLeaveBottomSheet()) }
    }

    fun showDeleteBottomSheet() {
        state.update { it.copy(confirmBottomSheetType = ConfirmBottomSheetType.ShowDeleteBottomSheet()) }
    }

    fun hideBottomSheet() {
        state.update { it.copy(confirmBottomSheetType = null) }
    }

    data class MyPlacesState(
        val items: List<MyPlaceItem> = emptyList(),
        val searchedText: String = "",
        val isSearchActive: Boolean = false,
        val isEditMode: Boolean = false,
        val myPlaceItem: MyPlaceItem? = null,
        val isAddMyPlaceEnabled: Boolean = false,
        val confirmBottomSheetType: ConfirmBottomSheetType? = null,
    )

    sealed class ConfirmBottomSheetType(
        @StringRes open val title: Int,
        @StringRes open val desc: Int,
        @StringRes open val confirmText: Int,
        @StringRes open val cancelText: Int,
    ) {
        data class ShowLeaveBottomSheet(
            override val title: Int = R.string.my_place_sheet_title,
            override val desc: Int = R.string.my_place_sheet_desc,
            override val confirmText: Int = R.string.my_place_sheet_leave,
            override val cancelText: Int = R.string.my_place_sheet_stay,
        ) : ConfirmBottomSheetType(title, desc, confirmText, cancelText)

        data class ShowDeleteBottomSheet(
            override val title: Int = R.string.my_place_delete_sheet_title,
            override val desc: Int = R.string.my_place_delete_sheet_desc,
            override val confirmText: Int = R.string.my_place_delete_sheet_delete,
            override val cancelText: Int = R.string.my_place_delete_sheet_cancel,
        ) : ConfirmBottomSheetType(title, desc, confirmText, cancelText)
    }
}