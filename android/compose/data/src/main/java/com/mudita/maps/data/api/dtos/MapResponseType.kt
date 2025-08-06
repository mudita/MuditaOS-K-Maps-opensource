package com.mudita.maps.data.api.dtos

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

sealed class MapResponseType {

    data class MapDto(
        @SerializedName("name")
        val name: String,
        @SerializedName("size")
        val size: String,
        @SerializedName("targetSize")
        val targetSize: String,
        @SerializedName("fileName")
        val fileName: String,
        @SerializedName("timestamp")
        val timestamp: String,
    ) : MapResponseType()

    data class RegionDto(
        @SerializedName("name")
        val name: String,
        @SerializedName("size")
        val size: String?,
        @SerializedName("targetSize")
        val targetSize: String?,
        @SerializedName("fileName")
        val fileName: String?,
        @SerializedName("timestamp")
        val timestamp: String?,
        @SerializedName("regions")
        val regions: List<MapResponseType>
    ) : MapResponseType()

}

class MapResponseTypeDeserializer : JsonDeserializer<MapResponseType> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MapResponseType {
        val jsonObject = json.asJsonObject
        return if (jsonObject.has("regions")) {
            context.deserialize(json, MapResponseType.RegionDto::class.java)
        } else {
            context.deserialize(json, MapResponseType.MapDto::class.java)
        }
    }
}
