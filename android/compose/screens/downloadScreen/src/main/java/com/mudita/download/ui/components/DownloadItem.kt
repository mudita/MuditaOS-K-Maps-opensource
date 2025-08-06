package com.mudita.download.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mudita.download.ui.progress.ProgressIndicator
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.map.common.R
import com.mudita.map.common.download.DownloadProgress
import com.mudita.map.common.download.DownloadingState

@Composable
fun DownloadItem(
    downloadingState: DownloadingState,
    downloadProgress: DownloadProgress,
    downloadStateDescription: String,
    onClick: (DownloadingState) -> Unit,
    modifier: Modifier = Modifier,
    downloadSize: String? = null,
) {
    Box(modifier = modifier) {
        DashedHorizontalDivider(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 12.dp, end = 12.dp, top = 2.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ProgressIndicator(
                    progress = downloadProgress.fraction.toFloat(),
                    modifier = Modifier.padding(top = 31.dp),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 1.dp, bottom = 4.dp)
                ) {
                    if (!downloadSize.isNullOrBlank()) {
                        Text(
                            text = downloadSize,
                            style = KompaktTypography500.labelSmall,
                        )

                        Box(
                            modifier = Modifier
                                .background(Color.Black, CircleShape)
                                .requiredSize(4.dp)
                        )
                    }

                    Text(
                        text = downloadStateDescription,
                        style = KompaktTypography500.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                painter = if (downloadingState.isError) {
                    painterResource(R.drawable.ic_alert)
                } else {
                    painterResource(R.drawable.ic_cancel)
                },
                contentDescription = null,
                modifier = Modifier
                    .clickable { onClick(downloadingState) }
                    .padding(4.dp)
                    .size(28.dp)
            )
        }
    }
}
