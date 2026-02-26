package com.ercan.smartwatch.data.model

import java.time.LocalDate
import java.time.LocalTime

data class PrayerTimesDay(
    val gregorianDate: LocalDate,
    val timezone: String,
    val fajr: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime
)
