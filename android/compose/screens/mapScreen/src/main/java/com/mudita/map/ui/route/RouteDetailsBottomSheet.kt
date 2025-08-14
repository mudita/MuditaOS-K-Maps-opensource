package com.mudita.map.ui.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.DashedVerticalDivider
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.map.common.R
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.map.common.ui.iconSize
import com.mudita.map.common.ui.navigationInfoBottomBarHeight
import com.mudita.map.common.ui.routeDetailsButtonsDividerHeight
import com.mudita.map.common.ui.routeDetailsButtonsSpacing
import com.mudita.map.common.ui.routeDetailsButtonsTouchAreaPadding
import com.mudita.map.common.ui.screenMargin
import com.mudita.map.common.ui.screenMarginHalf
import com.mudita.map.ui.NavigationTime

@Composable
fun RouteDetailsBottomSheet(
    isSoundEnabled: Boolean,
    isCommandsDisplayMode: Boolean,
    estimatedRouteDistance: String,
    estimatedRouteTime: NavigationTime?,
    modifier: Modifier = Modifier,
    onSoundClick: () -> Unit = {},
    onNavigationDisplayModeChange: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clickable(enabled = false) { }
            .background(colorBlack)
            .padding(top = dividerThicknessMedium)
            .background(colorWhite)
            .height(navigationInfoBottomBarHeight),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .padding(start = screenMargin)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = estimatedRouteDistance,
                    style = KompaktTypography500.labelMedium,
                    color = colorBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Image(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = screenMarginHalf),
                    painter = painterResource(R.drawable.ic_elipse),
                    contentDescription = null
                )
                Text(
                    text = estimatedRouteTime?.getDisplayText().orEmpty(),
                    style = KompaktTypography500.labelMedium,
                    color = colorBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(routeDetailsButtonsSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KompaktIconButton(
                    iconResId = if (isSoundEnabled) R.drawable.icon_sound else R.drawable.icon_sound_off,
                    iconSize = iconSize,
                    onClick =  onSoundClick,
                    touchAreaPadding = PaddingValues(routeDetailsButtonsTouchAreaPadding),
                )
                DashedVerticalDivider(modifier = Modifier.height(routeDetailsButtonsDividerHeight))
                KompaktIconButton(
                    iconResId = if (isCommandsDisplayMode) R.drawable.icon_maps else R.drawable.icon_list,
                    iconSize = iconSize,
                    onClick =  onNavigationDisplayModeChange,
                    touchAreaPadding = PaddingValues(routeDetailsButtonsTouchAreaPadding),
                )
            }
        }
    }
}

@Composable
@Preview
fun RouteDetailsBottomSheetPreview() {
    RouteDetailsBottomSheet(
        isSoundEnabled = false,
        isCommandsDisplayMode = true,
        estimatedRouteDistance = "1.2 km",
        estimatedRouteTime = NavigationTime.Minutes(5),
        onSoundClick = {},
        onNavigationDisplayModeChange = {},
    )
}