package com.mudita.map.ui.routePlanning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktSecondaryButton
import com.mudita.maps.frontitude.R.string

@Composable
fun MissingMapsScreen(
    onMissingMapsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorWhite)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = string.maps_routeplanning_error_h1_missingmaps),
            style = KompaktTypography900.titleMedium,
            color = colorBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(id = string.maps_routeplanning_error_body_yourouterequires),
            textAlign = TextAlign.Center,
            style = KompaktTypography500.bodyMedium,
            color = colorBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        KompaktSecondaryButton(
            modifier = Modifier.padding(top = 24.dp),
            text = stringResource(id = string.maps_routeplanning_button_seemissingmaps),
            attributes = KompaktTheme.buttonStyle.medium,
            onClick = onMissingMapsClick,
        )
    }
}

@Preview
@Composable
private fun MissingMapsScreenPreview() {
    MissingMapsScreen(onMissingMapsClick = {})
}
