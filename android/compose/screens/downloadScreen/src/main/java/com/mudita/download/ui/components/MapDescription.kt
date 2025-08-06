package com.mudita.download.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTheme
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.maps.frontitude.R

@Composable
internal fun MapDescription(
    parentName: String?,
    mapSize: String,
    modifier: Modifier = Modifier,
    contentColor: Color = colorBlack,
    updateAvailable: Boolean = false,
    isDownloading: Boolean = false,
    downloadStatus: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        if (!isDownloading && parentName != null) {
            Text(
                text = parentName,
                style = KompaktTypography500.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier,
            )

            Spacer(contentColor)
        }

        Text(
            text = mapSize,
            style = KompaktTypography500.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier,
        )

        if (isDownloading && downloadStatus != null) {
            Spacer(contentColor)

            Text(
                text = downloadStatus,
                style = KompaktTypography500.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!isDownloading && updateAvailable) {
            Spacer(contentColor)

            Text(
                text = stringResource(id = R.string.common_status_updateavalible),
                style = KompaktTypography500.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Spacer(color: Color) {
    Box(
        modifier = Modifier
            .background(color, CircleShape)
            .requiredSize(4.dp)
    )
}

@Preview
@Composable
private fun MapDescriptionPreview() {
    KompaktTheme {
        Column {
            MapDescription(
                parentName = "Poland",
                mapSize = "353 MB",
                updateAvailable = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun MapDescriptionDownloadingPreview() {
    KompaktTheme {
        Column {
            MapDescription(
                parentName = "Poland",
                mapSize = "353 MB",
                updateAvailable = true,
                modifier = Modifier.padding(16.dp),
                isDownloading = true,
                downloadStatus = "1 minute",
            )
        }
    }
}
