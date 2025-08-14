package com.mudita.map.ui.routePlanning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.map.common.R
import com.mudita.map.common.ui.navigationPointItemHeight
import com.mudita.map.repository.NavigationDirection
import com.mudita.map.repository.NavigationPointItem
import com.mudita.map.repository.displayableAddress
import com.mudita.maps.frontitude.R.plurals
import com.mudita.maps.frontitude.R.string
import java.util.UUID
import net.osmand.Location

@Composable
fun NavPointItem(
    item: NavigationPointItem,
    allItemsActive: Boolean,
    onItemClick: () -> Unit = {},
    onItemActionClick: () -> Unit = {},
    intermediatePointActionVisible: Boolean = true,
) {
    Row {
        Column(
            modifier = Modifier
                .height(if (item !is NavigationPointItem.Destination) 65.dp else navigationPointItemHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = item.iconLeft),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(28.dp)
                    .alpha(if (item.isActive) 1F else .25F)
            )
            if (item !is NavigationPointItem.Destination) {
                Icon(
                    modifier = Modifier
                        .alpha(if (allItemsActive) 1F else .25F),
                    painter = painterResource(id = R.drawable.ic_three_dot),
                    contentDescription = null,
                )
            }
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (item.isActive) 1F else .25F)
                        .clickable(enabled = item.isActive, onClick = onItemClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                            .height(navigationPointItemHeight)
                    ) {
                        val header = when (item) {
                            is NavigationPointItem.Destination -> string.common_label_to
                            is NavigationPointItem.Intermediate -> string.maps_common_label_via
                            is NavigationPointItem.Start -> null
                        }
                        if (header != null) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(id = header),
                                style = KompaktTypography900.labelSmall,
                            )
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 1.dp),
                                text = item.displayableAddress(),
                                style = KompaktTypography500.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 10.dp, end = 48.dp),
                                text = item.displayableAddress(),
                                style = KompaktTypography900.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (item is NavigationPointItem.Intermediate && item.iconRight != null
                        && intermediatePointActionVisible) {
                        KompaktIconButton(
                            modifier = Modifier
                                .alpha(if (item.isActive && item.isActionButtonActive) 1F else .25F),
                            enabled = item.isActionButtonActive,
                            iconResId = item.iconRight,
                            iconSize = 28.dp,
                            onClick = onItemActionClick,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(if (item !is NavigationPointItem.Intermediate) 8.dp else 54.dp))

                if (item !is NavigationPointItem.Intermediate && item.iconRight != null) {
                    KompaktIconButton(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .alpha(if (item.isActive && item.isActionButtonActive) 1F else .25F),
                        enabled = item.isActionButtonActive,
                        iconResId = item.iconRight,
                        iconSize = 28.dp,
                        onClick = onItemActionClick,
                    )
                }
            }

            if (item !is NavigationPointItem.Destination) {
                DashedHorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 7.dp, start = 16.dp, end = 56.dp)
                        .alpha(if (item.isActive) 1F else .25F)
                )
            }
        }
    }
}

@Composable
fun NavPointItems(
    navigationDirection: NavigationDirection,
    allItemsActive: Boolean,
    onItemClick: (NavigationPointItem) -> Unit,
    onItemActionClick: (NavigationPointItem) -> Unit,
    isCalculatingRoute: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (isCalculatingRoute && navigationDirection.intermediate.size > 1) {
            NavPointItem(
                item = navigationDirection.start,
                allItemsActive = false,
            )
            CollapsedIntermediatePoints(
                pointCount = navigationDirection.intermediate.size,
                modifier = Modifier.alpha(.25f),
            )
            NavPointItem(
                item = navigationDirection.destination,
                allItemsActive = false,
            )
        } else {
            navigationDirection.getPoints().forEach {
                NavPointItem(
                    item = it,
                    allItemsActive = allItemsActive,
                    onItemClick = { onItemClick(it) },
                    onItemActionClick = { onItemActionClick(it) },
                    intermediatePointActionVisible = !isCalculatingRoute,
                )
            }
        }
    }
}

@Composable
fun CollapsedIntermediatePoints(
    pointCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        Column(
            modifier = Modifier.height(65.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_intermediate_flag),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(28.dp)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_three_dot),
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp, end = 48.dp),
                text = pluralStringResource(
                    plurals.maps_routeplanning_placeholder_numberofstops,
                    pointCount,
                    pointCount,
                ),
                style = KompaktTypography500.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DashedHorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 56.dp)
            )
        }
    }
}

@Composable
@Preview(backgroundColor = 0xffffffff, showBackground = true)
private fun NavPointItemsPreview() {
    Column {
        NavPointItem(item = NavigationPoints.start, allItemsActive = true)
        NavPointItem(item = NavigationPoints.intermediate1, allItemsActive = true)
        NavPointItem(item = NavigationPoints.intermediate2, allItemsActive = true)
        NavPointItem(item = NavigationPoints.intermediate3, allItemsActive = true)
        NavPointItem(item = NavigationPoints.destination, allItemsActive = true)
    }
}

@Composable
@Preview(backgroundColor = 0xffffffff, showBackground = true)
private fun CollapsedPointItemsSingleStopPreview() {
    NavPointItems(
        navigationDirection = NavigationDirection(
            NavigationPoints.start.copy(isActive = false),
            listOf(NavigationPoints.intermediate1.copy(isActive = false)),
            NavigationPoints.destination.copy(isActive = false),
        ),
        allItemsActive = false,
        onItemClick = {},
        onItemActionClick = {},
        isCalculatingRoute = true,
    )
}

@Composable
@Preview(backgroundColor = 0xffffffff, showBackground = true)
private fun CollapsedPointItemsMultiStopPreview() {
    NavPointItems(
        navigationDirection = NavigationDirection(
            NavigationPoints.start.copy(isActive = false),
            listOf(
                NavigationPoints.intermediate1.copy(isActive = false),
                NavigationPoints.intermediate2.copy(isActive = false),
                NavigationPoints.intermediate3.copy(isActive = false),
            ),
            NavigationPoints.destination.copy(isActive = false),
        ),
        allItemsActive = false,
        onItemClick = {},
        onItemActionClick = {},
        isCalculatingRoute = true,
    )
}

private object NavigationPoints {
    val start =
        NavigationPointItem.Start(
            "",
            Location(""),
            isCurrentLocation = true,
            isActionButtonActive = true
        )

    val intermediate1 =
        NavigationPointItem.Intermediate(UUID.randomUUID(), "Krucza 12", Location(""))

    val intermediate2 =
        NavigationPointItem.Intermediate(UUID.randomUUID(), "Belwederska 12", Location(""))

    val intermediate3 =
        NavigationPointItem.Intermediate(
            UUID.randomUUID(),
            "Aleja Stanisława Augusta",
            Location("")
        )

    val destination = NavigationPointItem.Destination("Work", Location(""))
}
