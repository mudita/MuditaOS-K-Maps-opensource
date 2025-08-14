package com.mudita.maps.data.api

import com.google.gson.GsonBuilder
import com.mudita.maps.data.api.dtos.MapResponseType
import com.mudita.maps.data.api.dtos.MapResponseTypeDeserializer
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitHelper {

    private val gson = GsonBuilder()
        .registerTypeAdapter(MapResponseType::class.java, MapResponseTypeDeserializer())
        .create()

    fun getInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(30L, TimeUnit.SECONDS)
            .cache(null)
            .build()
    }
}