package com.example.location

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AmapPoiResponse(
    val status: String?,
    val info: String?,
    val pois: List<AmapPoi>?
)

@JsonClass(generateAdapter = true)
data class AmapPoi(
    val id: String?,
    val name: String,
    val type: String?,
    val address: String?,
    val location: String?, // "longitude,latitude"
    val cityname: String?
)

@JsonClass(generateAdapter = true)
data class AmapRouteResponse(
    val status: String?,
    val info: String?,
    val route: AmapRoute?
)

@JsonClass(generateAdapter = true)
data class AmapRoute(
    val paths: List<AmapPath>?
)

@JsonClass(generateAdapter = true)
data class AmapPath(
    val distance: String?, // in meters
    val duration: String?  // in seconds
)

interface AmapApi {
    @GET("v3/place/text")
    suspend fun searchPoi(
        @Query("keywords") keywords: String,
        @Query("key") key: String,
        @Query("city") city: String? = null,
        @Query("offset") offset: Int = 20,
        @Query("page") page: Int = 1
    ): AmapPoiResponse

    @GET("v3/direction/driving")
    suspend fun getDrivingRoute(
        @Query("origin") origin: String, // "lng,lat"
        @Query("destination") destination: String,
        @Query("key") key: String
    ): AmapRouteResponse
}

object AmapClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    val api: AmapApi = Retrofit.Builder()
        .baseUrl("https://restapi.amap.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(AmapApi::class.java)
}
