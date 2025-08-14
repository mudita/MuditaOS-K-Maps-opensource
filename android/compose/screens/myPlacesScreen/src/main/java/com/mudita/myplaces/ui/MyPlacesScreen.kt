package com.mudita.myplaces.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorGray_5
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.kompakt.commonUi.components.fakeScroll.KompaktLazyFakeScroll
import com.mudita.kompakt.commonUi.components.fakeScroll.canScroll
import com.mudita.kompakt.commonUi.util.extensions.conditional
import com.mudita.map.common.components.EmptyView
import com.mudita.map.common.components.NoResultsView
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.myplaces.R
import com.mudita.map.common.R as commonR

@Composable
fun MyPlacesScreen(
    viewModel: MyPlacesViewModel = hiltViewModel(),
    onInfoClicked: (MyPlaceItem) -> Unit,
    onItemClicked: (MyPlaceItem) -> Unit,
    onBackClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(key1 = "my_places_init") {
        viewModel.getMyPlaces()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp),
        ) {
            KompaktIconButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(start = 8.dp),
                commonR.drawable.ic_arrow_left_black
            ) {
                if (state.isSearchActive) viewModel.onSearchDeactivate()
                else onBackClicked()
            }

            if (state.isSearchActive) SearchView { viewModel.onSearchTextChanged(it) }
            else TitleView { viewModel.onSearchActivate() }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .height(2.dp)
                .background(Color.Black)
        )

        when {
            state.items.isNotEmpty() -> PlacesView(
                items = state.items,
                onInfoClicked = onInfoClicked,
                onItemClicked = onItemClicked
            )
            state.searchedText.isNotEmpty() -> NoResultsView(state.searchedText)
            else -> EmptyView(message = R.string.my_places_empty_view)
        }
    }
}

@Composable
private fun PlacesView(items: List<MyPlaceItem>, onInfoClicked: (MyPlaceItem) -> Unit, onItemClicked: (MyPlaceItem) -> Unit) {

    val listState = rememberLazyListState()

    KompaktLazyFakeScroll(
        listState = listState,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxSize()
            .conditional(listState.canScroll) {
                padding(end = 6.dp)
            },
    ) {
        itemsIndexed(items = items) { index, item ->
            MyPlaceItemView(
                myPlaceItem = item,
                isLast = index == items.lastIndex,
                isScrollable = listState.canScroll,
                onInfoClicked = onInfoClicked
            ) {
                onItemClicked.invoke(item)
            }
        }
    }
}

@Composable
private fun TitleView(onSearchClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.wrapContentSize(),
            text = stringResource(id = R.string.my_places_title),
            style = KompaktTypography900.titleLarge,
        )
        KompaktIconButton(
            modifier = Modifier
                .wrapContentSize()
                .padding(end = 6.dp),
            commonR.drawable.icon_search
        ) {
            onSearchClicked.invoke()
        }
    }
}

@Composable
private fun SearchView(onSearchTextChanged: (String) -> Unit) {
    var searchState by remember { mutableStateOf(TextFieldValue("")) }
    TextField(
        modifier = Modifier.wrapContentHeight(),
        value = searchState,
        textStyle = MaterialTheme.typography.titleLarge,
        onValueChange = {
            searchState = it
            onSearchTextChanged(it.text)
        },
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(id = R.string.my_places_search),
                color = colorGray_5,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colorWhite,
            unfocusedContainerColor = colorWhite,
            disabledContainerColor = colorWhite,
            focusedIndicatorColor = colorWhite,
            unfocusedIndicatorColor = colorWhite,
        ),
        trailingIcon = {
            if (searchState.text.isNotEmpty()) KompaktIconButton(
                modifier = Modifier.wrapContentSize(),
                commonR.drawable.ic_cancel_small
            ) {
                searchState = TextFieldValue()
                onSearchTextChanged("")
            }
        }
    )
}