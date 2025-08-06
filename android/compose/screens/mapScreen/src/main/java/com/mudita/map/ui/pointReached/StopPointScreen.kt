package com.mudita.map.ui.pointReached

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.map.common.R
import com.mudita.map.common.ui.routeStateIconSizeLarge
import com.mudita.map.ui.buttons.EndRouteButton

@Composable
fun StopPointScreen(
    modifier: Modifier = Modifier,
    onEndRouteClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    destinationName: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
    ) {
        EndRouteButton(
            onClick = onEndRouteClick,
            modifier = Modifier.align(Alignment.End),
        )

        Image(
            modifier = Modifier
                .padding(top = 66.dp)
                .size(routeStateIconSizeLarge),
            painter = painterResource(id = R.drawable.icon_stop_point),
            contentDescription = null,
        )

        Text(
            text = stringResource(com.mudita.maps.frontitude.R.string.maps_label_youhavearrivedstoppoint),
            style = KompaktTypography900.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp),
        )

        Text(
            text = destinationName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = KompaktTypography500.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        KompaktPrimaryButton(
            text = stringResource(id = com.mudita.maps.frontitude.R.string.common_button_cta_continue),
            size = KompaktTheme.buttonStyle.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            onClick = onContinueClick
        )
    }
}

@Composable
@Preview
fun StopPointScreenPreview() {
    StopPointScreen(destinationName = "Jana Czeczota 6")
}