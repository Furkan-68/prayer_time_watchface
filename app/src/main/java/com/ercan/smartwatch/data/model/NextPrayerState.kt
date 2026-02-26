package com.ercan.smartwatch.data.model

import java.time.Duration
import java.time.ZonedDateTime

data class NextPrayerState(
    val prayerName: PrayerName,
    val prayerTime: ZonedDateTime,
    val remaining: Duration
)
