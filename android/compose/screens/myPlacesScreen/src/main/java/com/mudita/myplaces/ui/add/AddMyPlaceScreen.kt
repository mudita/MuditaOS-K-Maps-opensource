package com.mudita.myplaces.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.map.common.components.ConfirmBottomSheet
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.model.SearchItemType
import com.mudita.myplaces.ui.MyPlacesViewModel
import com.mudita.myplaces.ui.add.views.CoordinatesView
import com.mudita.myplaces.ui.add.views.EditPlaceInput
import com.mudita.myplaces.ui.add.views.TitleView

@Composable
fun AddMyPlaceScreen(
    viewModel: MyPlacesViewModel = hiltViewModel(),
    myPlaceItem: MyPlaceItem?,
    isEditMode: Boolean = false,
    onCloseClicked: () -> Unit = {},
    hideKeyboard: () -> Unit,
    onAddClicked: (MyPlaceItem?) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf(myPlaceItem?.title.orEmpty()) }
    var address by remember { mutableStateOf(myPlaceItem?.address.orEmpty()) }

    LaunchedEffect(key1 = "add_my_place_init") {
        viewModel.initAddMyPlace(myPlaceItem, isEditMode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        TitleView(
            viewModel = viewModel,
            onCloseClicked = {
                hideKeyboard()
            },
            onAddClicked = {
                hideKeyboard()
                val modifiedItem = it?.copy(
                    title = name,
                    address = address,
                    searchItemType = SearchItemType.POI
                ) ?: return@TitleView

                if (isEditMode) viewModel.updateMyPlace(modifiedItem)
                else viewModel.addMyPlace(modifiedItem)

                onAddClicked(modifiedItem)
            }
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = colorBlack,
            thickness = 1.dp
        )

        EditPlaceInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            value = myPlaceItem?.title,
        ) {
            name = it
            viewModel.validate(name)
        }
        EditPlaceInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            value = myPlaceItem?.address,
            title = "Address",
        ) {
            address = it
        }

        CoordinatesView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            latitude = myPlaceItem?.lat,
            longitude = myPlaceItem?.lng
        )

        Spacer(modifier = Modifier.weight(1F))

        state.confirmBottomSheetType?.let { data ->
            ConfirmBottomSheet(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(id = data.title),
                desc = stringResource(id = data.desc),
                confirmText = stringResource(id = data.confirmText),
                cancelText = stringResource(id = data.cancelText),
                onConfirmClick = {
                    if (state.confirmBottomSheetType is MyPlacesViewModel.ConfirmBottomSheetType.ShowDeleteBottomSheet) viewModel.deleteMyPlace()
                    onCloseClicked()
                },
                onCancelClick = { viewModel.hideBottomSheet() },
            )
        }
    }
}
