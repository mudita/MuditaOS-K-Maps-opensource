package com.mudita.maps.data.api

import com.mudita.maps.data.api.dtos.MapResponseType
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Streaming

interface DownloadApiService {

    @GET(GET_INDEXES)
    suspend fun getIndexesList(): Response<List<MapResponseType>>

    @GET(DOWNLOAD_MAP)
    @Streaming
    suspend fun downloadMap(@Path(PARAM_FILE, encoded = true) file: String, @Header("Range") size: String): ResponseBody

}