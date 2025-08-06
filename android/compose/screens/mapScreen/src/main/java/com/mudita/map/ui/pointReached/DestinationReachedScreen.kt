package com.mudita.map.ui.pointReached

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
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorWhite
import com.mudita.kompakt.commonUi.components.button.KompaktPrimaryButton
import com.mudita.map.common.ui.routeStateIconSizeLarge
import com.mudita.maps.frontitude.R.string

@Composable
fun DestinationReachedScreen(
    destinationName: String = "",
    onDoneClick: () -> Unit = {}
) {
     Column(
         modifier = Modifier
             .fillMaxSize()
             .background(colorWhite)
     ) {
         Column(
             horizontalAlignment = Alignment.CenterHorizontally,
             verticalArrangement = Arrangement.Center,
             modifier = Modifier
                 .fillMaxWidth()
                 .weight(1f)
         ) {
             Icon(
                 painter = painterResource(id = com.mudita.map.common.R.drawable.icon_arrived),
                 contentDescription = null,
                 modifier = Modifier.size(routeStateIconSizeLarge)
             )
             Text(
                 text = stringResource(id = string.maps_label_youhavearrived),
                 style = KompaktTypography900.headlineLarge,
                 modifier = Modifier.padding(top = 16.dp)
             )
             Text(
                 modifier = Modifier.padding(
                     top = 12.dp,
                     start = 32.dp,
                     end = 32.dp
                 ),
                 text = destinationName,
                 style = KompaktTypography500.titleMedium,
                 maxLines = 2,
                 overflow = TextOverflow.Ellipsis,
                 textAlign = TextAlign.Center,
             )
         }
         KompaktPrimaryButton(
             text = stringResource(id = string.common_label_done),
             size = KompaktTheme.buttonStyle.large,
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(16.dp),
             onClick = onDoneClick
         )
     }
}

@Composable
@Preview
private fun DestinationReachedScreenPreview() {
    DestinationReachedScreen(
        destinationName = "Jana Czeczota 6",
        onDoneClick = {}
    )
}