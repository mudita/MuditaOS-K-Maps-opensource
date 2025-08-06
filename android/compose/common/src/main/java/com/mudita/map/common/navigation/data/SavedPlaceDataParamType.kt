package com.mudita.map.common.navigation.data

import android.os.Bundle
import androidx.navigation.NavType
import com.google.gson.Gson
import com.mudita.map.common.model.MyPlaceItem

class SavedPlaceDataParamType : NavType<MyPlaceItem?>(isNullableAllowed = true) {
    @Suppress("DEPRECATION")
    override fun get(bundle: Bundle, key: String): MyPlaceItem? {
        return bundle.getParcelable(key)
    }

    override fun parseValue(value: String): MyPlaceItem? {
        return try {
            Gson().fromJson(value, MyPlaceItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun put(bundle: Bundle, key: String, value: MyPlaceItem?) {
        bundle.putParcelable(key, value)
    }
}