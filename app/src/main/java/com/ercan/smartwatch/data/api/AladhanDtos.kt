package com.ercan.smartwatch.data.api

data class TimingsResponseDto(
    val code: Int,
    val status: String,
    val data: TimingsDataDto
)

data class TimingsDataDto(
    val timings: Map<String, String>,
    val date: DateDto,
    val meta: MetaDto
)

data class DateDto(
    val gregorian: GregorianDateDto
)

data class GregorianDateDto(
    val date: String
)

data class MetaDto(
    val timezone: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class MethodsResponseDto(
    val code: Int,
    val status: String,
    val data: Map<String, MethodDto>
)

data class MethodDto(
    val id: Int,
    val name: String
)
