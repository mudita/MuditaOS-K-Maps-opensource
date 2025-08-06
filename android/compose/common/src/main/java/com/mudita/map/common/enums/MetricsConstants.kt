package com.mudita.map.common.enums

import android.content.Context
import com.mudita.map.common.R

enum class MetricsConstants(private val key: Int, private val ttsString: String) {
    KILOMETERS_AND_METERS(R.string.si_km_m, "km-m"),
    MILES_AND_FEET(R.string.si_mi_feet, "mi-f"),
    MILES_AND_METERS(R.string.si_mi_meters, "mi-m"),
    MILES_AND_YARDS(R.string.si_mi_yard, "mi-y"),
    NAUTICAL_MILES(R.string.si_nm, "nm");

    fun toHumanString(ctx: Context): String {
        return ctx.getString(key)
    }
    fun toStringRes(): Int {
        return key
    }

    fun toTTSString(): String {
        return ttsString
    }

    companion object {
        fun fromTTSString(ttsString: String): MetricsConstants {
            return values().firstOrNull { it.ttsString == ttsString }
                ?: values().first { it.ttsString.startsWith(ttsString) } // fallback for the change in `ttsString`.
        }
    }
}