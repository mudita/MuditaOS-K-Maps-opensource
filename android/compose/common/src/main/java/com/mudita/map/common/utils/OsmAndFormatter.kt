package com.mudita.map.common.utils

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import com.mudita.map.common.R
import com.mudita.map.common.enums.MetricsConstants
import com.mudita.map.common.sharedPrefs.MetricUnitPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import net.osmand.LocationConvert
import net.osmand.data.Amenity
import net.osmand.util.Algorithms

@Parcelize
@Singleton
class OsmAndFormatter @Inject constructor(
    @ApplicationContext private val context: @RawValue Context,
    private val metricUnitPreference: @RawValue MetricUnitPreference
): Parcelable {

    companion object {
        const val METERS_IN_KILOMETER = 1000f
        const val METERS_IN_ONE_MILE = 1609.344f // 1609.344
        const val METERS_IN_ONE_NAUTICALMILE = 1852f // 1852
        const val YARDS_IN_ONE_METER = 1.0936f
        const val FEET_IN_ONE_METER = YARDS_IN_ONE_METER * 3f
        private const val MIN_DURATION_FOR_DATE_FORMAT = 48 * 60 * 60
        private val fixed2 = DecimalFormat("0.00")
        private val fixed1 = DecimalFormat("0.0")
        private var twelveHoursFormat = false
        private var fullTimeFormatter: TimeFormatter? = null
        private var shortTimeFormatter: TimeFormatter? = null
        const val MILS_IN_DEGREE = 17.777778f
        const val FORMAT_DEGREES_SHORT = 8
        const val FORMAT_DEGREES = LocationConvert.FORMAT_DEGREES
        const val FORMAT_MINUTES = LocationConvert.FORMAT_MINUTES
        const val FORMAT_SECONDS = LocationConvert.FORMAT_SECONDS
        const val UTM_FORMAT = LocationConvert.UTM_FORMAT
        const val OLC_FORMAT = LocationConvert.OLC_FORMAT
        const val MGRS_FORMAT = LocationConvert.MGRS_FORMAT
        const val SWISS_GRID_FORMAT = LocationConvert.SWISS_GRID_FORMAT
        const val SWISS_GRID_PLUS_FORMAT = LocationConvert.SWISS_GRID_PLUS_FORMAT

        init {
            shortTimeFormatter = TimeFormatter(Locale.getDefault(), "HH:mm", "h:mm a")
        }

        @JvmStatic
        fun getPoiStringWithoutType(amenity: Amenity, locale: String?, transliterate: Boolean): String? {
            val pc = amenity.type
            val pt = pc.getPoiTypeByKeyName(amenity.subType)
            var typeName = amenity.subType
            if (pt != null) {
                typeName = pt.translation
            } else if (typeName != null) {
                typeName = Algorithms.capitalizeFirstLetterAndLowercase(typeName.replace('_', ' '))
            }
            val localName = amenity.getName(locale, transliterate)
            if (typeName != null && localName.contains(typeName)) {
                // type is contained in name e.g.
                // localName = "Bakery the Corner"
                // type = "Bakery"
                // no need to repeat this
                return localName
            }
            return if (localName.length == 0) {
                typeName
            } else "$typeName $localName"
            //$NON-NLS-1$
        }

        @JvmStatic
        fun getPoiStringsWithoutType(amenity: Amenity, locale: String?, transliterate: Boolean): List<String?> {
            val pc = amenity.type
            val pt = pc.getPoiTypeByKeyName(amenity.subType)
            var typeName = amenity.subType
            if (pt != null) {
                typeName = pt.translation
            } else if (typeName != null) {
                typeName = Algorithms.capitalizeFirstLetterAndLowercase(typeName.replace('_', ' '))
            }
            val res: MutableList<String?> = ArrayList()
            val localName = amenity.getName(locale, transliterate)
            addPoiString(typeName, localName, res)
            for (name in amenity.getOtherNames(true)) {
                addPoiString(typeName, name, res)
            }
            for (name in amenity.getAdditionalInfoValues(false)) {
                addPoiString(typeName, name, res)
            }
            return res
        }

        private fun addPoiString(poiTypeName: String?, poiName: String, res: MutableList<String?>) {
            if (poiTypeName != null && poiName.contains(poiTypeName)) {
                res.add(poiName)
            }
            if (poiName.isEmpty()) {
                res.add(poiTypeName)
            }
            res.add("$poiTypeName $poiName")
        }
    }

    fun getFormattedDistanceValue(
        meters: Float,
        forceTrailingZeros: Boolean = false,
    ): FormattedValue {
        val mainUnitStr: Int
        val mainUnitInMeters: Float
        val mc = MetricsConstants.fromTTSString(metricUnitPreference.getMetricUnit())
        if (mc === MetricsConstants.KILOMETERS_AND_METERS) {
            mainUnitStr = R.string.km
            mainUnitInMeters = METERS_IN_KILOMETER
        } else if (mc === MetricsConstants.NAUTICAL_MILES) {
            mainUnitStr = R.string.nm
            mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE
        } else {
            mainUnitStr = R.string.mile
            mainUnitInMeters = METERS_IN_ONE_MILE
        }
        val floatDistance = meters / mainUnitInMeters
        return if (meters >= 100 * mainUnitInMeters) {
            formatValue(
                (meters / mainUnitInMeters + 0.5).toInt().toFloat(), mainUnitStr, forceTrailingZeros,
                0, context
            )
        } else if (meters > 9.99f * mainUnitInMeters) {
            formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 1, context)
        } else if (meters > 0.999f * mainUnitInMeters) {
            formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 2, context)
        } else if (mc === MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters) {
            formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 2, context)
        } else if (mc === MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters) {
            formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 2, context)
        } else if (mc === MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters) {
            formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 2, context)
        } else if (mc === MetricsConstants.NAUTICAL_MILES && meters > 0.99f * mainUnitInMeters) {
            formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 2, context)
        } else {
            if (mc === MetricsConstants.KILOMETERS_AND_METERS || mc === MetricsConstants.MILES_AND_METERS) {
                return formatValue((meters + 0.5).toInt().toFloat(), R.string.m, forceTrailingZeros, 0, context)
            } else if (mc === MetricsConstants.MILES_AND_FEET) {
                val feet = (meters * FEET_IN_ONE_METER + 0.5).toInt()
                return formatValue(feet.toFloat(), R.string.foot, forceTrailingZeros, 0, context)
            } else if (mc === MetricsConstants.MILES_AND_YARDS) {
                val yards = (meters * YARDS_IN_ONE_METER + 0.5).toInt()
                return formatValue(yards.toFloat(), R.string.yard, forceTrailingZeros, 0, context)
            }
            formatValue((meters + 0.5).toInt().toFloat(), R.string.m, forceTrailingZeros, 0, context)
        }
    }

    fun calculateRoundedDist(distInMeters: Double): Double {
        val mc = MetricsConstants.fromTTSString(metricUnitPreference.getMetricUnit())
        var mainUnitInMeter = 1.0
        var metersInSecondUnit = METERS_IN_KILOMETER.toDouble()
        if (mc === MetricsConstants.MILES_AND_FEET) {
            mainUnitInMeter = FEET_IN_ONE_METER.toDouble()
            metersInSecondUnit = METERS_IN_ONE_MILE.toDouble()
        } else if (mc === MetricsConstants.MILES_AND_METERS) {
            mainUnitInMeter = 1.0
            metersInSecondUnit = METERS_IN_ONE_MILE.toDouble()
        } else if (mc === MetricsConstants.MILES_AND_YARDS) {
            mainUnitInMeter = YARDS_IN_ONE_METER.toDouble()
            metersInSecondUnit = METERS_IN_ONE_MILE.toDouble()
        }

        // 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000 ...
        var generator = 1
        var pointer: Byte = 1
        var point = mainUnitInMeter
        var roundDist = 1.0
        while (distInMeters * point >= generator) {
            roundDist = (generator / point)
            if (pointer++ % 3 == 2) {
                generator = generator * 5 / 2
            } else {
                generator *= 2
            }

            if (point == mainUnitInMeter && metersInSecondUnit * mainUnitInMeter * 0.9f <= generator) {
                point = 1 / metersInSecondUnit
                generator = 1
                pointer = 1
            }
        }
        //Miles exceptions: 2000ft->0.5mi, 1000ft->0.25mi, 1000yd->0.5mi, 500yd->0.25mi, 1000m ->0.5mi, 500m -> 0.25mi
        if (mc === MetricsConstants.MILES_AND_METERS && roundDist == 1000.0) {
            roundDist = (0.5f * METERS_IN_ONE_MILE).toDouble()
        } else if (mc === MetricsConstants.MILES_AND_METERS && roundDist == 500.0) {
            roundDist = (0.25f * METERS_IN_ONE_MILE).toDouble()
        } else if (mc === MetricsConstants.MILES_AND_FEET && roundDist == 2000 / FEET_IN_ONE_METER.toDouble()) {
            roundDist = (0.5f * METERS_IN_ONE_MILE).toDouble()
        } else if (mc === MetricsConstants.MILES_AND_FEET && roundDist == 1000 / FEET_IN_ONE_METER.toDouble()) {
            roundDist = (0.25f * METERS_IN_ONE_MILE).toDouble()
        } else if (mc === MetricsConstants.MILES_AND_YARDS && roundDist == 1000 / YARDS_IN_ONE_METER.toDouble()) {
            roundDist = (0.5f * METERS_IN_ONE_MILE).toDouble()
        } else if (mc === MetricsConstants.MILES_AND_YARDS && roundDist == 500 / YARDS_IN_ONE_METER.toDouble()) {
            roundDist = (0.25f * METERS_IN_ONE_MILE).toDouble()
        }
        return roundDist
    }

    fun getFormattedTime(seconds: Float) = shortTimeFormatter?.format(Date(seconds.toLong() * 1000L), false)

    private fun formatValue(
        value: Float, @StringRes unitId: Int, forceTrailingZeroes: Boolean,
        decimalPlacesNumber: Int, app: Context
    ): FormattedValue {
        return formatValue(value, app.getString(unitId), forceTrailingZeroes, decimalPlacesNumber)
    }

    fun formatValue(value: Float, unit: String, forceTrailingZeroes: Boolean, decimalPlacesNumber: Int): FormattedValue {
        var pattern = "0"
        if (decimalPlacesNumber > 0) {
            val fractionDigitPattern = if (forceTrailingZeroes) '0' else '#'
            val fractionDigitsPattern = CharArray(decimalPlacesNumber)
            Arrays.fill(fractionDigitsPattern, fractionDigitPattern)
            pattern += "." + String(fractionDigitsPattern)
        }
        val decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())
        decimalFormatSymbols.groupingSeparator = ' '
        val decimalFormat = DecimalFormat(pattern)
        decimalFormat.decimalFormatSymbols = decimalFormatSymbols
        val fiveOrMoreDigits = Math.abs(value) >= 10000
        if (fiveOrMoreDigits) {
            decimalFormat.isGroupingUsed = true
            decimalFormat.groupingSize = 3
        }
        val messageFormat = MessageFormat("{0}")
        messageFormat.setFormatByArgumentIndex(0, decimalFormat)
        val formattedValue = messageFormat.format(arrayOf<Any>(value))
            .replace('\n', ' ')
        return FormattedValue(formattedValue, unit)
    }

    class FormattedValue @JvmOverloads constructor(val value: String, val unit: String, private val separateWithSpace: Boolean = true) {
        fun format(context: Context): String {
            return if (separateWithSpace) context.getString(R.string.ltr_or_rtl_combine_via_space, value, unit) else MessageFormat("{0}{1}").format(
                arrayOf<Any>(
                    value, unit
                )
            )
        }
        val formattedValue: String
            get() = if (separateWithSpace) "$value $unit" else MessageFormat("{0}{1}").format(
                arrayOf<Any>(
                    value, unit
                )
            )
    }

    class TimeFormatter @JvmOverloads constructor(
        locale: Locale, pattern: String,
        amPmPattern: String, timeZone: TimeZone? = null
    ) {
        private val simpleTimeFormat: DateFormat
        private val amPmTimeFormat: DateFormat

        init {
            simpleTimeFormat = SimpleDateFormat(pattern, locale)
            amPmTimeFormat = SimpleDateFormat(amPmPattern, locale)
            if (timeZone != null) {
                simpleTimeFormat.calendar.timeZone = timeZone
                amPmTimeFormat.calendar.timeZone = timeZone
            }
        }

        fun format(date: Date, twelveHoursFormat: Boolean): String {
            val timeFormat = if (twelveHoursFormat) amPmTimeFormat else simpleTimeFormat
            return timeFormat.format(date)
        }
    }
}