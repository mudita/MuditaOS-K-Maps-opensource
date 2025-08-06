package net.osmand.plus.utils

object LetUtils {
    inline fun <T: Any, T1: Any, R> safeLet(p0: T?, p1: T1?, block: (T, T1) -> R): R? {
        return if (p0 != null && p1 != null) block(p0, p1) else null
    }

    inline fun <T: Any, T1: Any, T2: Any, R> safeLet(p0: T?, p1: T1?, p2: T2?, block: (T, T1, T2) -> R?): R? {
        return if (p0 != null && p1 != null && p2 != null) block(p0, p1, p2) else null
    }
}
