package com.mudita.map.ui.routeCalculation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktButtonAttributes
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.kompakt.commonUi.components.progress.KompaktAnimatedLoader
import com.mudita.map.common.ui.routeCalculationScreenDefaultTopMargin
import com.mudita.map.common.ui.routeCalculationScreenSmallTopMargin
import com.mudita.maps.frontitude.R.string

@Composable
fun RouteCalculationScreen(
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit,
    stage: RouteCalculationState.InProgress.Stage,
    hasIntermediatePoints: Boolean,
    modifier: Modifier = Modifier
) {
    val topMargin: Dp = if (hasIntermediatePoints) routeCalculationScreenSmallTopMargin else routeCalculationScreenDefaultTopMargin

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorWhite)
            .padding(top = topMargin, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KompaktAnimatedLoader(
                modifier = Modifier.padding(12.dp),
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = when (stage) {
                    RouteCalculationState.InProgress.Stage.Started,
                    RouteCalculationState.InProgress.Stage.Continued -> stringResource(string.maps_planningroute_status_calculatingroute)
                    RouteCalculationState.InProgress.Stage.Alert -> stringResource(string.maps_routeplanning_h1_stillcalculating)
                },
                textAlign = TextAlign.Center,
                style = KompaktTypography900.titleMedium,
                color = colorBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = when (stage) {
                    RouteCalculationState.InProgress.Stage.Started -> stringResource(string.maps_routeplanning_body_thiscantakeupto7)
                    RouteCalculationState.InProgress.Stage.Alert -> stringResource(string.maps_routeplanning_body_thecalculationistaking)
                    RouteCalculationState.InProgress.Stage.Continued -> stringResource(string.maps_routeplanning_body_calculationtimesoften)
                },
                textAlign = TextAlign.Center,
                style = KompaktTypography500.bodyMedium.copy(letterSpacing = 0.2.sp),
                color = colorBlack,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (stage == RouteCalculationState.InProgress.Stage.Alert) {
            KompaktPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = stringResource(string.maps_routeplanning_button_keepwaiting),
                size = KompaktTheme.buttonStyle.large,
                onClick = onContinueClick,
            )
        }
        KompaktSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            text = stringResource(string.maps_routeplanning_button_cancelroute),
            attributes = KompaktButtonAttributes.Large,
            onClick = onCancelClick,
        )
    }
}

@Composable
@Preview
fun RouteCalculationScreenPreview() {
    RouteCalculationScreen(
        onCancelClick = {},
        onContinueClick = {},
        stage = RouteCalculationState.InProgress.Stage.Started,
        hasIntermediatePoints = false,
    )
}

@Composable
@Preview
fun RouteCalculationScreenWithAlertPreview() {
    RouteCalculationScreen(
        onCancelClick = {},
        onContinueClick = {},
        stage = RouteCalculationState.InProgress.Stage.Alert,
        hasIntermediatePoints = false,
    )
}

@Composable
@Preview
fun RouteCalculationScreenWhenContinuedPreview() {
    RouteCalculationScreen(
        onCancelClick = {},
        onContinueClick = {},
        stage = RouteCalculationState.InProgress.Stage.Continued,
        hasIntermediatePoints = false,
    )
}
