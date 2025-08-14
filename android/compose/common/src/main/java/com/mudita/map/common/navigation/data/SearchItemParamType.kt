package com.mudita.map.common.navigation.data

import android.os.Bundle
import androidx.navigation.NavType
import com.google.gson.Gson
import com.mudita.map.common.model.SearchItem

class SearchItemParamType : NavType<SearchItem?>(isNullableAllowed = true) {
    @Suppress("DEPRECATION")
    override fun get(bundle: Bundle, key: String): SearchItem? {
        return bundle.getParcelable(key)
    }

    override fun parseValue(value: String): SearchItem? {
        return try {
            Gson().fromJson(value, SearchItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun put(bundle: Bundle, key: String, value: SearchItem?) {
        bundle.putParcelable(key, value)
    }
}