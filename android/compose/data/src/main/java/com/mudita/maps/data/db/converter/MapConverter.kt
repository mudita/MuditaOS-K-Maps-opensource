package com.mudita.maps.data.db.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import javax.inject.Inject
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

@ProvidedTypeConverter
class MapConverter @Inject constructor(private val gson: Gson) {
    @TypeConverter
    fun toString(map: Map<String, String>?): String? = map?.let(gson::toJson)

    @OptIn(ExperimentalStdlibApi::class)
    @TypeConverter
    fun toMap(json: String?): Map<String, String>? = json?.let {
        gson.fromJson(it, typeOf<Map<String, String>>().javaType)
    }
}
