package com.mudita.map.ui.routePlanning

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.KompaktIconButton
import com.mudita.map.common.R
import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.model.navigation.NavigationPoint
import com.mudita.map.common.ui.dividerThicknessMedium
import com.mudita.map.repository.NavigationDirection
import com.mudita.map.repository.NavigationModeItem
import com.mudita.map.repository.NavigationPointItem
import com.mudita.map.ui.routeCalculation.RouteCalculationErrorScreen
import com.mudita.map.ui.routeCalculation.RouteCalculationScreen
import com.mudita.map.ui.routeCalculation.RouteCalculationState
import java.util.UUID
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.router.errors.RouteCalculationError

@Composable
fun NavigationDestination(
    navigationDirection: NavigationDirection,
    modifier: Modifier = Modifier,
    onItemClick: (NavigationPointItem) -> Unit = {},
    onItemActionClick: (NavigationPointItem) -> Unit = {},
    onCancelClick: () -> Unit = {},
    isCalculatingRoute: Boolean = false,
) {
    val points = navigationDirection.getPoints()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorWhite)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, _, _ -> }
            }
            .padding(bottom = dividerThicknessMedium)
    ) {
        Row(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
            val allItemsActive = points.all { it.isActive }
            KompaktIconButton(
                modifier = Modifier.alpha(if (allItemsActive) 1f else .25f),
                touchAreaPadding = PaddingValues(start = 16.dp, top = 8.dp),
                iconResId = R.drawable.ic_cancel,
                iconSize = 28.dp,
                enabled = allItemsActive,
                onClick = onCancelClick
            )
            NavPointItems(
                navigationDirection = navigationDirection,
                allItemsActive = allItemsActive,
                onItemClick = onItemClick,
                onItemActionClick = onItemActionClick,
                isCalculatingRoute = isCalculatingRoute,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        HorizontalDivider(thickness = dividerThicknessMedium, color = colorBlack)
    }
}

@Composable
@Preview
fun NavigationDestinationPreview() {
    NavigationDestination(
        NavigationDirection(
            start = NavigationPointItem.Start("", Location(""), true),
            intermediate = listOf(NavigationPointItem.Intermediate(UUID.randomUUID(), "V4", Location(""))),
            destination = NavigationPointItem.Destination("V4", Location(""))
        )
    )
}

@Composable
@Preview
fun NavigationDestinationAndMissingMapsPreview() {
    Column {
        NavigationDestination(
            NavigationDirection(
                start = NavigationPointItem.Start("", Location(""), true),
                intermediate = listOf(NavigationPointItem.Intermediate(UUID.randomUUID(), "V4", Location(""))),
                destination = NavigationPointItem.Destination("V4", Location(""))
            )
        )
        MissingMapsScreen(onMissingMapsClick = {})
    }
}

@Composable
@Preview
fun NavigationDestinationAndRouteCalculationErrorPreview() {
    Column {
        NavigationDestination(
            NavigationDirection(
                start = NavigationPointItem.Start("", Location(""), true),
                intermediate = listOf(
                    NavigationPointItem.Intermediate(UUID.randomUUID(), "Intermediate 1", Location("")),
                    NavigationPointItem.Intermediate(UUID.randomUUID(), "Intermediate 2", Location("")),
                    NavigationPointItem.Intermediate(UUID.randomUUID(), "Intermediate 3", Location(""))
                ),
                destination = NavigationPointItem.Destination("V4", Location(""))
            )
        )
        RouteCalculationErrorScreen(
            error = RouteCalculationError.RouteIsTooComplex(""),
            navigationItem = NavigationItem(
                startPoint = NavigationPoint(LatLon(51.772592, 19.476798), "", isCurrentLocation = true),
                intermediatePoints = listOf(
                    NavigationPoint(LatLon(52.106525, 19.948460), "Kolorowa 64")
                ),
                endPoint = NavigationPoint(LatLon(52.240654, 21.050564), "Kolejowa 6"),
            ),
            navigationMode = NavigationModeItem.Driving,
            onDismissErrorClick = {},
        )
    }
}

@Composable
@Preview
fun NavigationDestinationAndRouteCalculationPreview() {
    Column {
        NavigationDestination(
            NavigationDirection(
                start = NavigationPointItem.Start("", Location(""), true),
                intermediate = listOf(
                    NavigationPointItem.Intermediate(UUID.randomUUID(), "Intermediate 1", Location("")),
                    NavigationPointItem.Intermediate(UUID.randomUUID(), "Intermediate 2", Location("")),
                    NavigationPointItem.Intermediate(UUID.randomUUID(), "Intermediate 3", Location(""))
                ),
                destination = NavigationPointItem.Destination("V4", Location(""))
            )
        )
        RouteCalculationScreen(
            onCancelClick = {},
            onContinueClick = {},
            stage = RouteCalculationState.InProgress.Stage.Started,
            hasIntermediatePoints = true,
        )
    }
}