package com.ercan.smartwatch.domain

import com.ercan.smartwatch.data.model.NextPrayerState
import com.ercan.smartwatch.data.model.PrayerName
import com.ercan.smartwatch.data.model.PrayerTimesDay
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class NextPrayerCalculator {
    fun calculate(
        now: ZonedDateTime,
        today: PrayerTimesDay,
        tomorrow: PrayerTimesDay
    ): NextPrayerState {
        val zoneId = today.timezone.toZoneIdOrDefault(now.zone)
        val nowInZone = now.withZoneSameInstant(zoneId)

        val todaySchedule = listOf(
            PrayerName.FAJR to today.fajr,
            PrayerName.DHUHR to today.dhuhr,
            PrayerName.ASR to today.asr,
            PrayerName.MAGHRIB to today.maghrib,
            PrayerName.ISHA to today.isha
        )

        todaySchedule.forEach { (name, localTime) ->
            val candidate = ZonedDateTime.of(today.gregorianDate, localTime, zoneId)
            if (!nowInZone.isAfter(candidate)) {
                return NextPrayerState(
                    prayerName = name,
                    prayerTime = candidate,
                    remaining = Duration.between(nowInZone, candidate)
                )
            }
        }

        val tomorrowFajr = ZonedDateTime.of(tomorrow.gregorianDate, tomorrow.fajr, zoneId)
        return NextPrayerState(
            prayerName = PrayerName.FAJR,
            prayerTime = tomorrowFajr,
            remaining = Duration.between(nowInZone, tomorrowFajr)
        )
    }

    private fun String.toZoneIdOrDefault(defaultZone: ZoneId): ZoneId {
        return runCatching { ZoneId.of(this) }.getOrElse { defaultZone }
    }
}
