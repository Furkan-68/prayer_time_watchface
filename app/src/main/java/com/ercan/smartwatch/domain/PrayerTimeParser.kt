package com.ercan.smartwatch.domain

import com.ercan.smartwatch.data.api.TimingsDataDto
import com.ercan.smartwatch.data.model.PrayerTimesDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object PrayerTimeParser {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val looseTimeRegex = Regex("""\b(\d{1,2}:\d{2})\b""")
    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")

    fun parse(data: TimingsDataDto): PrayerTimesDay {
        val timings = data.timings

        return PrayerTimesDay(
            gregorianDate = LocalDate.parse(data.date.gregorian.date, dateFormatter),
            timezone = data.meta.timezone,
            fajr = parseTime(timings["Fajr"]),
            dhuhr = parseTime(timings["Dhuhr"]),
            asr = parseTime(timings["Asr"]),
            maghrib = parseTime(timings["Maghrib"]),
            isha = parseTime(timings["Isha"])
        )
    }

    private fun parseTime(raw: String?): LocalTime {
        require(!raw.isNullOrBlank()) { "Missing prayer time value" }
        val token = looseTimeRegex.find(raw)?.groups?.get(1)?.value
            ?: throw IllegalArgumentException("Invalid prayer time: $raw")
        return LocalTime.parse(token, timeFormatter)
    }
}
