package com.ercan.smartwatch.watchface

import java.time.ZonedDateTime

sealed interface PrayerWatchFaceUiState {
    val now: ZonedDateTime

    data class SetupRequired(
        override val now: ZonedDateTime
    ) : PrayerWatchFaceUiState

    data class Content(
        override val now: ZonedDateTime,
        val nextPrayerName: String,
        val nextPrayerTimeText: String,
        val countdownText: String,
        val isStale: Boolean
    ) : PrayerWatchFaceUiState

    data class Error(
        override val now: ZonedDateTime,
        val message: String
    ) : PrayerWatchFaceUiState
}
