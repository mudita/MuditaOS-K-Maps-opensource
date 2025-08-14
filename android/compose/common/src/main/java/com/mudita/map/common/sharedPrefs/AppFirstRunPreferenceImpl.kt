package com.mudita.map.common.sharedPrefs

import android.content.SharedPreferences
import javax.inject.Inject

class AppFirstRunPreferenceImpl @Inject constructor(private val sharedPreferences: SharedPreferences) : AppFirstRunPreference {

    companion object {
        const val APP_FIRST_RUN_KEY = "app_first_run"
    }

    override fun isAppOpenedFirstTime(): Boolean {
        return sharedPreferences.getBoolean(APP_FIRST_RUN_KEY, true)
    }

    override fun setAppOpened() {
        sharedPreferences.edit().putBoolean(APP_FIRST_RUN_KEY, false).apply()
    }
}
