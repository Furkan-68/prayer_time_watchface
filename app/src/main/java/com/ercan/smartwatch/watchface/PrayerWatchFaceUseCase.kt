package com.ercan.smartwatch.watchface

import com.ercan.smartwatch.data.repo.PrayerTimesRepository
import com.ercan.smartwatch.data.store.SettingsStore
import com.ercan.smartwatch.domain.NextPrayerCalculator
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class PrayerWatchFaceUseCase(
    private val settingsStore: SettingsStore,
    private val repository: PrayerTimesRepository,
    private val calculator: NextPrayerCalculator = NextPrayerCalculator()
) {
    suspend fun loadState(now: ZonedDateTime = ZonedDateTime.now()): PrayerWatchFaceUiState {
        val settings = settingsStore.get()
        if (!settings.isConfigured()) {
            return PrayerWatchFaceUiState.SetupRequired(now = now)
        }

        val timingsResult = repository.getTodayAndTomorrow(settings)
        val window = timingsResult.getOrElse {
            return PrayerWatchFaceUiState.Error(
                now = now,
                message = "Unable to load"
            )
        }

        val zoneId = runCatching { ZoneId.of(window.today.timezone) }.getOrElse { now.zone }
        val nowInZone = now.withZoneSameInstant(zoneId)
        val nextPrayer = calculator.calculate(nowInZone, window.today, window.tomorrow)

        val nextTimeText = nextPrayer.prayerTime.format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
        )

        return PrayerWatchFaceUiState.Content(
            now = now,
            nextPrayerName = nextPrayer.prayerName.displayName,
            nextPrayerTimeText = nextTimeText,
            countdownText = formatCountdown(nextPrayer.remaining),
            isStale = window.isStale
        )
    }

    private fun formatCountdown(duration: Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "in %02dh %02dm".format(hours, minutes)
    }
}
