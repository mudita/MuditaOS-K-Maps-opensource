package com.mudita.maptypes

import com.mudita.map.common.R as commonR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorGray_11
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.kompakt.commonUi.components.KompaktRadioButton
import com.mudita.map.common.enums.MapType
import com.mudita.map.common.model.MapTypeItem
import com.mudita.map.common.model.MapTypesData


@Composable
fun MapTypesScreen(
    viewModel: MapTypesViewModel = hiltViewModel(),
    mapTypesData: MapTypesData,
    onItemClicked: (MapTypeItem) -> Unit,
    onBackClicked: () -> Unit = {}
) {

    val state by viewModel.state.collectAsState()

    LaunchedEffect(key1 = "init_map_types") {
        viewModel.initWithData(mapTypesData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            KompaktIconButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(start = 8.dp),
                commonR.drawable.ic_arrow_left_black
            ) { onBackClicked() }
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(start = 18.dp),
                text = stringResource(R.string.map_types_title),
                style = KompaktTypography900.headlineSmall
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
                .height(2.dp)
                .background(Color.Black)
        )

        state.mapTypeItems.forEachIndexed { index, item ->
            MapTypeRadioButton(
                item = item,
                data = state.mapTypesData,
                isLast = index == state.mapTypeItems.size - 1,
            ) {
                onItemClicked(item)
                viewModel.onItemChecked(item)
            }
        }
    }
}

@Composable
@Preview
fun MapTypeRadioButton(
    item: MapTypeItem = MapTypeItem.Driving(),
    data: MapTypesData = MapTypesData(MapType.DRIVING),
    isLast: Boolean = false,
    onCheckedChanged: () -> Unit = {},
) {
    val isSwitched = data.currentMapType == item.mapType
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .padding(top = 6.dp)
    ) {
        KompaktRadioButton(
            isSwitchedOn = isSwitched,
            onCheckedChange = {
                onCheckedChanged()
            }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 24.dp)
                .weight(1F)
        ) {
            Text(
                text = stringResource(id = item.title),
                style = KompaktTypography900.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = stringResource(id = item.desc),
                style = KompaktTypography500.labelSmall
            )
            Spacer(modifier = Modifier.height(30.dp))
            if (isLast.not()) HorizontalDivider(thickness = 1.dp, color = colorGray_11)
        }
    }
}