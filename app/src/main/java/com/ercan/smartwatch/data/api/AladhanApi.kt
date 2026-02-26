package com.ercan.smartwatch.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApi {
    @GET("v1/timingsByCity/{date}")
    suspend fun getTimingsByCity(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int,
        @Query("school") school: Int,
        @Query("calendarMethod") calendarMethod: String
    ): TimingsResponseDto

    @GET("v1/methods")
    suspend fun getMethods(): MethodsResponseDto
}
