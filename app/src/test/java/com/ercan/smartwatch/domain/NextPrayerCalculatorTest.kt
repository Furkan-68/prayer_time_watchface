package com.ercan.smartwatch.domain

import com.google.common.truth.Truth.assertThat
import com.ercan.smartwatch.data.model.PrayerTimesDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class NextPrayerCalculatorTest {
    private val calculator = NextPrayerCalculator()
    private val zone = ZoneId.of("Europe/Istanbul")

    private val today = PrayerTimesDay(
        gregorianDate = LocalDate.of(2026, 2, 13),
        timezone = zone.id,
        fajr = LocalTime.of(6, 0),
        dhuhr = LocalTime.of(13, 0),
        asr = LocalTime.of(16, 0),
        maghrib = LocalTime.of(18, 30),
        isha = LocalTime.of(20, 0)
    )

    private val tomorrow = today.copy(gregorianDate = today.gregorianDate.plusDays(1))

    @Test
    fun beforeFajr_returnsFajr() {
        val now = ZonedDateTime.of(today.gregorianDate, LocalTime.of(5, 15), zone)

        val result = calculator.calculate(now, today, tomorrow)

        assertThat(result.prayerName.displayName).isEqualTo("Fajr")
        assertThat(result.prayerTime.toLocalTime()).isEqualTo(LocalTime.of(6, 0))
    }

    @Test
    fun betweenAsrAndMaghrib_returnsMaghrib() {
        val now = ZonedDateTime.of(today.gregorianDate, LocalTime.of(16, 45), zone)

        val result = calculator.calculate(now, today, tomorrow)

        assertThat(result.prayerName.displayName).isEqualTo("Maghrib")
        assertThat(result.prayerTime.toLocalTime()).isEqualTo(LocalTime.of(18, 30))
    }

    @Test
    fun afterIsha_rollsToTomorrowFajr() {
        val now = ZonedDateTime.of(today.gregorianDate, LocalTime.of(22, 0), zone)

        val result = calculator.calculate(now, today, tomorrow)

        assertThat(result.prayerName.displayName).isEqualTo("Fajr")
        assertThat(result.prayerTime.toLocalDate()).isEqualTo(tomorrow.gregorianDate)
        assertThat(result.prayerTime.toLocalTime()).isEqualTo(LocalTime.of(6, 0))
    }
}
