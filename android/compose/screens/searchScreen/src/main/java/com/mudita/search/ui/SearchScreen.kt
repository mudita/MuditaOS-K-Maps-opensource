package com.mudita.search.ui

import com.mudita.map.common.R as commonR
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorGray_7
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.kompakt.commonUi.components.fakeScroll.KompaktLazyFakeScroll
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemData
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.maps.frontitude.R.string
import kotlinx.coroutines.android.awaitFrame
import net.osmand.search.SearchUICore

@Composable
fun SearchScreen(
    searchItem: SearchItem? = null,
    searchUICore: SearchUICore? = null,
    viewModel: SearchViewModel = hiltViewModel(),
    onBackClicked: () -> Unit = {},
    onSearchCategoryClicked: (SearchItemData?) -> Unit = {},
    onSearchSubCategoryClicked: (SearchItemData?) -> Unit = {},
    onSearchCityClicked: (SearchItemData?) -> Unit = {},
    showOnMap: (SearchItem) -> Unit,
) {
    val searchState by viewModel.searchState.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = "") {
        viewModel.setSearchUiCore(searchUICore)
        viewModel.updateSearchData(searchItem)
    }
    BackHandler(onBack = onBackClicked)

    Column(
        modifier = Modifier
            .background(colorWhite)
            .fillMaxSize()
            .imePadding()
    ) {
        SearchBar(
            searchText = searchState.searchText,
            onSearchTextChange = { viewModel.onSearchTextChanged(it) },
            resetSearch = { viewModel.resetSearch() },
            focusRequester = focusRequester,
            backHandler = onBackClicked,
        )
        HorizontalDivider(thickness = dividerThicknessMedium, color = colorBlack)

        if (searchState.currentItems.isNotEmpty()) {
            KompaktLazyFakeScroll(Modifier.weight(1f)) {
                itemsIndexed(searchState.currentItems) { index, item ->
                    val isLast = index == searchState.currentItems.lastIndex
                    SearchRowItem(item, viewModel::formatDistance, isLast) {
                        when (it.itemType) {
                            SearchItemType.CATEGORY -> onSearchCategoryClicked.invoke(it)
                            SearchItemType.SUB_CATEGORY -> onSearchSubCategoryClicked.invoke(it)
                            SearchItemType.CITY -> onSearchCityClicked.invoke(it)
                            else -> {
                                showOnMap.invoke(viewModel.onItemClicked(it))
                            }
                        }
                    }
                }

                if (searchState.showSearchMore) {
                    item {
                        IncreaseSearchRadius(
                            searchState.nextSearchRadius,
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        ) {
                            viewModel.onRadiusIncreased()
                        }
                    }
                }
            }
        } else if (searchState.showEmptyResults) {
            NothingFound(searchState.nextSearchRadius, Modifier.weight(1f)) {
                viewModel.onRadiusIncreased()
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (searchState.searchInProgress) {
            LoadingIndicator()
        }
    }

    LaunchedEffect(focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }
}

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    resetSearch: () -> Unit,
    focusRequester: FocusRequester,
    backHandler: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .height(65.dp)
            .padding(horizontal = 8.dp)
    ) {
        KompaktIconButton(
            iconResId = commonR.drawable.ic_arrow_left_black,
            onClick = backHandler,
        )

        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            value = searchText,
            textStyle = KompaktTypography500.titleLarge.copy(fontSize = 25.sp),
            onValueChange = onSearchTextChange,
            singleLine = true,
            decorationBox = { innerTextField ->
                innerTextField()
                if (searchText.isEmpty())
                    Text(
                        text = stringResource(id = string.common_label_search),
                        color = colorGray_7,
                        style = KompaktTypography500.titleMedium,
                    )
            },
        )

        if (searchText.isNotEmpty()) {
            KompaktIconButton(
                modifier = Modifier.wrapContentSize(),
                commonR.drawable.ic_cancel_small,
                onClick = resetSearch
            )
        }
    }
}

@Composable
fun NothingFound(
    nextSearchRadius: String?,
    modifier: Modifier = Modifier,
    onSearchMoreClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(id = string.common_search_error_h1_wecouldntfind),
            style = KompaktTypography900.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val error = if (nextSearchRadius != null) {
            stringResource(id = string.maps_search_error_body_increasethesearchradius)
        } else {
            stringResource(id = string.common_search_error_body_usedifferentkeywords)
        }
        Text(
            text = error,
            textAlign = TextAlign.Center,
            style = KompaktTypography500.bodyMedium,
            modifier = Modifier.padding(
                top = 8.dp,
                start = 24.dp,
                end = 24.dp
            )
        )
        nextSearchRadius?.let {
            IncreaseSearchRadius(
                nextSearchRadius = nextSearchRadius,
                onClick = onSearchMoreClick,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box {
        HorizontalDivider(thickness = dividerThicknessMedium, color = colorBlack)
        Row(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 12.dp)
                .align(Alignment.Center)
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = commonR.drawable.ic_spinner),
                contentDescription = null,
                tint = colorBlack
            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = stringResource(id = string.common_status_loadingdata),
                style = KompaktTypography900.labelLarge
            )
        }
    }
}

@Composable
fun IncreaseSearchRadius(
    nextSearchRadius: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    KompaktSecondaryButton(
        text = stringResource(id = string.maps_search_cta_button_increasesearchradius, nextSearchRadius.orEmpty()),
        onClick = onClick,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Preview
@Composable
private fun SearchBarPreview(modifier: Modifier = Modifier) {
    SearchBar(
        searchText = "Atlanta",
        onSearchTextChange = {},
        resetSearch = {},
        focusRequester = remember { FocusRequester() },
        backHandler = {})
}

@Preview(showBackground = true, name = "Loading preview")
@Composable
fun LoadingPreview() {
    LoadingIndicator()
}

@Preview(showBackground = true, name = "IncreaseSearchRadius")
@Composable
fun IncreaseSearchRadiusPreview() {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        IncreaseSearchRadius(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            nextSearchRadius = "500 km", onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "NothingFound")
@Composable
fun NothingFoundPreview() {
    Row(
    ) {
        NothingFound(nextSearchRadius = "2000 km") {}
    }
}