package com.mudita.map.common.utils

import android.icu.text.DecimalFormat

object FileSize {
    private const val UNIT_KILOBYTES = "KB"

    private const val UNIT_MEGABYTES = "MB"

    private const val UNIT_GIGABYTES = "GB"

    private val kilobytesInMegabytes = 1024.0

    private val megabytesInGigabytes = 1024.0

    private val decimalFormat = DecimalFormat("#.#")

    fun formatMegabytes(megabytes: Double): String = when {
        megabytes > megabytesInGigabytes -> "${decimalFormat.format(megabytes / megabytesInGigabytes)} $UNIT_GIGABYTES"
        else -> "${decimalFormat.format(megabytes)} $UNIT_MEGABYTES"
    }

    fun formatKilobytes(kilobytes: Double): String = when {
        kilobytes < kilobytesInMegabytes -> "${decimalFormat.format(kilobytes)} $UNIT_KILOBYTES"
        kilobytes / kilobytesInMegabytes < megabytesInGigabytes -> "${decimalFormat.format(kilobytes / kilobytesInMegabytes)} $UNIT_MEGABYTES"
        else -> "${decimalFormat.format(kilobytes / kilobytesInMegabytes / megabytesInGigabytes)} $UNIT_GIGABYTES"
    }
}
