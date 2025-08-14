package com.mudita.download.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.map.common.R
import com.mudita.maps.frontitude.R.string

@Preview
@Composable
fun NetworkNotAvailable(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 90.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(46.dp),
            painter = painterResource(id = R.drawable.no_internet_connection),
            contentDescription = stringResource(string.common_label_nointernetconnection),
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(string.common_label_nointernetconnection),
            style = KompaktTypography900.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(string.common_error_body_opensettingstocheck),
            style = KompaktTypography500.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}