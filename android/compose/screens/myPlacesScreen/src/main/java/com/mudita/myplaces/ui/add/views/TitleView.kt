package com.mudita.myplaces.ui.add.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.myplaces.R
import com.mudita.myplaces.ui.MyPlacesViewModel
import com.mudita.map.common.R as commonR

@Composable
fun TitleView(
    viewModel: MyPlacesViewModel,
    onCloseClicked: () -> Unit,
    onAddClicked: (MyPlaceItem?) -> Unit
) {
    val state by viewModel.state.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KompaktIconButton(
            modifier = Modifier.wrapContentSize(),
            commonR.drawable.ic_cancel
        ) {
            onCloseClicked()
            viewModel.showLeaveBottomSheet()
        }
        Text(
            modifier = Modifier.wrapContentSize(),
            text = stringResource(id = R.string.my_places_title_edit),
            style = KompaktTypography900.titleLarge,
        )
        Spacer(modifier = Modifier.weight(1F))
        if (state.isEditMode) Icon(
            modifier = Modifier
                .padding(end = 24.dp)
                .clickable {
                    viewModel.showDeleteBottomSheet()
                },
            painter = painterResource(commonR.drawable.icon_star_filled),
            contentDescription = null,
        )
        KompaktPrimaryButton(
            modifier = Modifier
                .wrapContentSize()
                .padding(end = 16.dp),
            enabled = state.isAddMyPlaceEnabled,
            text = stringResource(
                id = if (state.isEditMode) commonR.string.common_save else commonR.string.common_add
            ),
        ) {
            onAddClicked(state.myPlaceItem)
        }
    }
}