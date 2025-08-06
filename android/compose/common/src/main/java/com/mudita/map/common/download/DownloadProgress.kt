package com.mudita.map.common.download

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mudita.maps.frontitude.R
import kotlin.time.Duration.Companion.seconds

data class DownloadProgress(
    val fraction: Double,
    val remainingSeconds: Double = 0.0,
) {

    companion object {
        val Empty = DownloadProgress(0.0, 0.0)

        fun create(fraction: Double, downloadSpeed: Double): DownloadProgress =
            DownloadProgress(
                fraction = fraction,
                remainingSeconds = if (downloadSpeed > 0) {
                    (1.0 - fraction) / downloadSpeed
                } else {
                    -1.0
                },
            )
    }
}

@Composable
fun DownloadProgress.formattedRemainingTime(): String =
    formattedRemainingTime(LocalContext.current)

fun DownloadProgress.formattedRemainingTime(context: Context): String =
    if (remainingSeconds < 0) {
        context.getString(R.string.common_label_calculatingdownload)
    } else {
        remainingSeconds.seconds.toComponents { hours, minutes, seconds, _ ->
            buildString {
                if (hours > 1) append(context.getString(R.string.common_status_numberofhours, hours))
                if (isNotBlank()) append(" ")
                val roundedMinutes = (minutes + if (seconds > 30) 1 else 0).coerceAtLeast(1)
                append(
                    context.resources.getQuantityString(
                        R.plurals.common_status_numberofmins,
                        roundedMinutes,
                        roundedMinutes
                    )
                )
            }
        }
    }