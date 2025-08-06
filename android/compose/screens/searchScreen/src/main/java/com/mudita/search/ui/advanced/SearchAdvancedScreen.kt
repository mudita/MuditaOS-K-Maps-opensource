package com.mudita.search.ui.advanced

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorGray_5
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.kompakt.commonUi.components.fakeScroll.KompaktLazyFakeScroll
import com.mudita.kompakt.commonUi.util.NoRippleInteractionSource
import com.mudita.map.common.components.LoadingView
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemData
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.utils.conditional
import com.mudita.search.R
import com.mudita.search.ui.ShowOnMap
import com.mudita.search.ui.advanced.SearchAdvancedViewModel.Companion.ITEMS_PER_PAGE
import net.osmand.data.LatLon
import net.osmand.search.SearchUICore

@Composable
fun SearchAdvancedScreen(
    searchAdvancedViewModel: SearchAdvancedViewModel = hiltViewModel(),
    searchItem: SearchItem? = null,
    navigationItem: NavigationItem? = null,
    onBackClicked: () -> Unit = {},
    myLocation: () -> LatLon?,
    searchUICore: SearchUICore? = null,
    showOnMap: (SearchItemData.Address) -> Unit
) {
    val state = searchAdvancedViewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    val searchState by searchAdvancedViewModel.uiState.collectAsState()
    val pageSize = if (searchState.searchItems.size % ITEMS_PER_PAGE == 0) searchState.searchItems.size / ITEMS_PER_PAGE
    else (searchState.searchItems.size / ITEMS_PER_PAGE) + 1

    LaunchedEffect(key1 = "advanced_init") {
        searchAdvancedViewModel.setItem(searchItem)
        searchAdvancedViewModel.init(searchUICore, myLocation())

        if (searchState.searchItems.isEmpty() && searchItem?.itemType != SearchItemType.POSTCODE) {
            searchAdvancedViewModel.runSearch("")
        } else {
            focusRequester.requestFocus()
        }

        searchAdvancedViewModel.obtainIntent(SearchAdvancedViewModel.Intent.SearchQueryChange(searchItem?.localName?.plus(searchItem.desc) ?: ""))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (searchState.isSearching) LoadingView(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .zIndex(1F),
            text = stringResource(id = com.mudita.map.common.R.string.searching)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .conditional(searchState.isSearching) {
                    alpha(0.5F)
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                KompaktIconButton(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(start = 16.dp),
                    com.mudita.map.common.R.drawable.ic_arrow_left_black,
                    touchAreaPadding = PaddingValues(0.dp)
                ) {
                    onBackClicked.invoke()
                }
                TextField(
                    modifier = Modifier
                        .wrapContentHeight()
                        .weight(1F)
                        .focusRequester(focusRequester),
                    value = state.value.searchText,
                    textStyle = MaterialTheme.typography.titleLarge,
                    onValueChange = {
                        if (state.value.searchText != it) {
                            searchAdvancedViewModel.runSearch(it)
                        }
                        searchAdvancedViewModel.obtainIntent(SearchAdvancedViewModel.Intent.SearchQueryChange(it))
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(
                                id = if (searchItem?.itemType == SearchItemType.CITY) R.string.search_placeholder
                                else R.string.search_postcode_placeholder
                            ),
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
                        if (state.value.searchText.isNotEmpty()) KompaktIconButton(
                            modifier = Modifier.wrapContentSize(),
                            com.mudita.map.common.R.drawable.ic_cancel_small
                        ) {
                            searchAdvancedViewModel.runSearch("")
                            searchAdvancedViewModel.obtainIntent(SearchAdvancedViewModel.Intent.SearchQueryChange(""))
                        }
                    }
                )
            }
            HorizontalDivider(
                color = colorBlack,
                thickness = 1.dp,
            )
            if (
                state.value.searchText.isEmpty() &&
                (searchState.searchItem?.itemType == SearchItemType.CITY ||
                searchState.searchItem?.itemType == SearchItemType.ADDRESS ||
                searchState.searchItem?.itemType == SearchItemType.POSTCODE)
                && navigationItem == null
                && searchState.searchCityItem != null
                && searchState.searchItems.isNotEmpty()
            ) {
                ShowOnMap {
                    searchAdvancedViewModel.onShowOnMapClicked { item ->
                        item?.let { showOnMap.invoke(it) }
                    }
                }

                HorizontalDivider(
                    color = colorBlack,
                    thickness = 2.dp,
                )
            }
            if (searchState.searchItems.isNotEmpty() || searchState.isSearching) {
                KompaktLazyFakeScroll {
                    itemsIndexed(searchState.searchItems) { index, item ->
                        AdvancedSearchItemView(
                            item = item,
                            index = index,
                            lastItemIndex = searchState.searchItems.lastIndex,
                            onItemClick = {
                                searchAdvancedViewModel.onItemClicked(
                                item,
                            ) { addressItem -> if (addressItem != null) showOnMap(addressItem) }
                                searchAdvancedViewModel.obtainIntent(
                                    SearchAdvancedViewModel.Intent.SearchQueryChange("")
                                )
                            },
                            onIncreaseRadius = { searchAdvancedViewModel.increaseRadius(state.value.searchText) }
                        )
                    }
                }
            } else {
                // default view for search by postcode is blank screen
                if (searchItem?.itemType != SearchItemType.POSTCODE && state.value.searchText.isNotEmpty()) {
                    NothingFound(searchAdvancedViewModel)
                }
            }
        }
    }
}

@Composable
fun NothingFound(searchAdvancedViewModel: SearchAdvancedViewModel) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(.4F)
    ) {
        Text(
            text = stringResource(id = R.string.could_not_find_anything),
            style = KompaktTypography900.titleMedium
        )
        Text(
            text = stringResource(id = R.string.modify_query),
            textAlign = TextAlign.Center,
            style = KompaktTypography500.bodyMedium,
            modifier = Modifier.padding(
                top = 8.dp,
                start = 16.dp,
                end = 16.dp
            )
        )
        IncreaseSearchRadius {
            searchAdvancedViewModel.increaseRadius()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncreaseSearchRadius(onClick: () -> Unit = {}) {
    Card(
        onClick = { onClick.invoke() },
        border = BorderStroke(2.dp, colorBlack),
        shape = AbsoluteRoundedCornerShape(10.dp),
        modifier = Modifier.padding(top = 12.dp),
        interactionSource = NoRippleInteractionSource()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
        ) {
            Icon(
                painter = painterResource(id = com.mudita.map.common.R.drawable.ic_increase_radius),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(id = R.string.increase_radius),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colorBlack
            )
        }
    }
}