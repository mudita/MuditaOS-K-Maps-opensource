package com.mudita.map.ui.buttons

import com.mudita.maps.frontitude.R as frontitudeR
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.map.common.R
import com.mudita.map.common.ui.mapButtonsScreenPadding

@Composable
fun MapButtons(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onZoomInClick: () -> Unit = {},
    onZoomOutClick: () -> Unit = {},
    isNotInNavigationMode: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(mapButtonsScreenPadding)
    ) {
        if (isNotInNavigationMode) {
            MapButton(
                modifier = Modifier.align(Alignment.TopStart),
                drawableRes = R.drawable.icon_hamburger,
                buttonClick = onMenuClick
            )
            MapButton(
                modifier = Modifier.align(Alignment.TopEnd),
                drawableRes = R.drawable.icon_search,
                buttonClick = onSearchClick
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            if (isNotInNavigationMode) {
                MyLocationButton(
                    buttonClick = onMyLocationClick
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            if (isNotInNavigationMode) MapZooms { zoomType ->
                when (zoomType) {
                    ZoomType.ZOOM_IN -> onZoomInClick()
                    ZoomType.ZOOM_OUT -> onZoomOutClick()
                }
            }
        }
    }
}

@Composable
fun PickLocationMapButtons(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onZoomInClick: () -> Unit = {},
    onZoomOutClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(mapButtonsScreenPadding),
    ) {
        MapButton(
            modifier = Modifier.align(Alignment.TopStart),
            drawableRes = R.drawable.ic_arrow_left_black,
            buttonClick = onBackClick
        )

        MyLocationButton(
            buttonClick = onMyLocationClick,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        MapZooms(modifier = Modifier.align(Alignment.BottomEnd)) { zoomType ->
            when (zoomType) {
                ZoomType.ZOOM_IN -> onZoomInClick()
                ZoomType.ZOOM_OUT -> onZoomOutClick()
            }
        }
    }
}

@Composable
fun CenterNavigationMapButton(
    modifier: Modifier = Modifier,
    onCenterClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 5.dp),
    ) {
        MapButtonWithText(
            modifier = Modifier.align(Alignment.BottomStart),
            textRes = frontitudeR.string.maps_navigation_label_center,
            drawableRes = R.drawable.ic_gps_location_compass,
            buttonClick = onCenterClick,
        )
    }
}

@Composable
fun MyLocationButton(modifier: Modifier = Modifier, buttonClick: () -> Unit = {}) = MapButton(
    modifier = modifier,
    drawableRes = R.drawable.icon_my_location,
    buttonClick = buttonClick
)

@Composable
@Preview
fun MapButtonsPreview() {
    MapButtons()
}

@Composable
@Preview
fun PickLocationMapButtonsPreview() {
    Column {
        PickLocationMapButtons(modifier = Modifier.weight(1f))
    }
}

@Composable
@Preview
fun CenterNavigationMapButtonPreview() {
    CenterNavigationMapButton()
}