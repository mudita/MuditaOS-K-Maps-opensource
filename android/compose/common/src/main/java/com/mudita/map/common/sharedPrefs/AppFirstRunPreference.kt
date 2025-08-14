package com.mudita.map.common.sharedPrefs

interface AppFirstRunPreference {
    fun isAppOpenedFirstTime(): Boolean
    fun setAppOpened()
}
