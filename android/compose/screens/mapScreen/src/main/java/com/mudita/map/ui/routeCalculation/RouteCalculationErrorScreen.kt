package com.mudita.map.ui.routeCalculation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktButtonAttributes
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.map.common.R
import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.model.navigation.NavigationPoint
import com.mudita.map.common.ui.routeCalculationScreenSmallTopMargin
import com.mudita.map.repository.NavigationModeItem
import com.mudita.maps.frontitude.R.string
import net.osmand.data.LatLon
import net.osmand.router.errors.RouteCalculationError

@Composable
fun RouteCalculationErrorScreen(
    error: RouteCalculationError,
    navigationItem: NavigationItem,
    navigationMode: NavigationModeItem,
    onDismissErrorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorWhite)
            .padding(top = routeCalculationScreenSmallTopMargin, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                modifier = Modifier
                    .padding(16.dp)
                    .size(32.dp),
                painter = painterResource(id = R.drawable.ic_warning),
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = error.getErrorTitle(),
                textAlign = TextAlign.Center,
                style = KompaktTypography900.titleMedium,
                color = colorBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = error.getErrorMessage(navigationItem = navigationItem, navigationMode = navigationMode),
                textAlign = TextAlign.Center,
                style = KompaktTypography500.bodyMedium.copy(letterSpacing = 0.2.sp),
                color = colorBlack,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        KompaktSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            text = stringResource(string.maps_routeplanning_button_plannewroute),
            attributes = KompaktButtonAttributes.Large,
            onClick = onDismissErrorClick,
        )
    }
}

@Composable
fun RouteCalculationError.getErrorTitle(): String {
    return when (this) {
        is RouteCalculationError.EmptyRoute,
        is RouteCalculationError.RouteNotFound,
        is RouteCalculationError.StartPointTooFarFromRoad,
        is RouteCalculationError.EndPointTooFarFromRoad,
        is RouteCalculationError.IntermediatePointTooFarFromRoad,
        is RouteCalculationError.ApplicationModeNotSupported ->
            stringResource(string.maps_routeplanning_error_h1_cantcalculateroute)

        is RouteCalculationError.RouteIsTooComplex ->
            stringResource(string.maps_routeplanning_error_h1_therouteistoolong)

        is RouteCalculationError.SelectedServiceNotAvailable,
        is RouteCalculationError.CalculationTimeLimitExceeded ->
            stringResource(string.common_error_somethingwentwrong)

        else -> stringResource(string.common_error_somethingwentwrong)
    }
}

@Composable
fun RouteCalculationError.getErrorMessage(navigationItem: NavigationItem, navigationMode: NavigationModeItem): String {
    return when (this) {
        is RouteCalculationError.EmptyRoute,
        is RouteCalculationError.RouteNotFound ->
            stringResource(string.maps_routeplanning_error_body_theremaybesomthingimpassable)

        is RouteCalculationError.StartPointTooFarFromRoad ->
            stringResource(string.maps_routeplanning_error_body_yourcurrentloactionistoofar)

        is RouteCalculationError.EndPointTooFarFromRoad ->
            stringResource(
                id = string.maps_routeplanning_error_body_placenameistoofarfromthe,
                navigationItem.endPoint?.address.orEmpty()
            )

        is RouteCalculationError.IntermediatePointTooFarFromRoad ->
            stringResource(
                id = string.maps_routeplanning_error_body_placenameistoofarfromthe,
                navigationItem.intermediatePoints.getOrNull(this.intermediatePointIndex)?.address.orEmpty()
            )

        is RouteCalculationError.SelectedServiceNotAvailable ->
            stringResource(string.common_dialog_button_tryagain)

        is RouteCalculationError.ApplicationModeNotSupported -> {
            when (navigationMode) {
                NavigationModeItem.Walking -> stringResource(string.maps_routeplanning_error_body_theroutecantbetravelledonfoot)
                NavigationModeItem.Cycling -> stringResource(string.maps_routeplanning_error_body_theroutecantbetravelled)
                NavigationModeItem.Driving -> stringResource(string.maps_routeplanning_error_body_theroutecantbetravelledbycar)
            }
        }

        is RouteCalculationError.RouteIsTooComplex ->
            stringResource(string.maps_routeplanning_error_body_weareworkingtoimprove)

        is RouteCalculationError.CalculationTimeLimitExceeded ->
            stringResource(string.maps_routeplanning_error_body_ittooktoolongtocalculate)

        else ->
            stringResource(string.common_dialog_button_tryagain)
    }
}

@Preview
@Composable
private fun RouteCalculationErrorScreenPreview() {
    RouteCalculationErrorScreen(
        error = RouteCalculationError.IntermediatePointTooFarFromRoad("", 0),
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
