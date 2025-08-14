package net.osmand.plus.utils

object BooleanUtils {
    fun Boolean?.toNotNull(default: Boolean = false): Boolean = this ?: default
}
