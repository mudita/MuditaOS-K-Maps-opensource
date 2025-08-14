package com.mudita.map.ui.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.maps.frontitude.R
import com.mudita.map.R as mapsR

@Composable
fun MapsWelcomeScreen(
    modifier: Modifier = Modifier,
    onContinueClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = mapsR.drawable.ic_welcome_to_maps),
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(id = R.string.maps_offlinenavigation_h1_welcometomaps),
                style = KompaktTypography900.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
                text = stringResource(id = R.string.maps_savedlocations_dialog_body_asthisappdoesntuse),
                style = KompaktTypography500.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        KompaktPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            size = KompaktTheme.buttonStyle.large,
            text = stringResource(R.string.common_button_cta_continue),
            onClick = onContinueClick
        )
    }
}

@Composable
@Preview(backgroundColor = 0xffffffff, showBackground = true)
fun MapsWelcomeScreenPreview() {
    MapsWelcomeScreen(
        modifier = Modifier,
        onContinueClick = {}
    )
}
