package com.mudita.map.common.navigation.data

import android.os.Bundle
import androidx.navigation.NavType
import com.google.gson.Gson
import com.mudita.map.common.model.navigation.NavigationItem

class NavigationItemParamType : NavType<NavigationItem?>(isNullableAllowed = true) {
    @Suppress("DEPRECATION")
    override fun get(bundle: Bundle, key: String): NavigationItem? {
        return bundle.getParcelable(key)
    }

    override fun parseValue(value: String): NavigationItem? {
        return try {
            Gson().fromJson(value, NavigationItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun put(bundle: Bundle, key: String, value: NavigationItem?) {
        bundle.putParcelable(key, value)
    }
}