package com.mudita.map.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mudita.maps.frontitude.R
import kotlin.math.round

sealed class NavigationTime {
    @Composable
    abstract fun getDisplayText(): String

    data class Seconds(val seconds: Int) : NavigationTime() {
        @Composable
        override fun getDisplayText(): String =
            stringResource(R.string.common_status_numbersec, seconds)
    }

    data class Minutes(val minutes: Int) : NavigationTime() {
        @Composable
        override fun getDisplayText(): String =
            pluralStringResource(R.plurals.common_status_numberofmins, minutes, minutes)
    }

    data class HoursMinutes(val hours: Int, val minutes: Int) : NavigationTime() {
        @Composable
        override fun getDisplayText(): String =
            stringResource(R.string.common_status_numberhournumbermin, hours, minutes)
    }

    companion object {
        fun create(seconds: Int): NavigationTime {
            val minutes = round((seconds % 3600.0) / 60).toInt()
            return when {
                seconds < 60 && minutes < 1 -> Seconds(seconds)
                seconds < 3600 -> Minutes(minutes)
                else -> HoursMinutes(seconds / 3600, minutes)
            }
        }
    }
}
