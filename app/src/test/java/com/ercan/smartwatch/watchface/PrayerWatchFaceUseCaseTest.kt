package com.ercan.smartwatch.watchface

import com.ercan.smartwatch.data.model.PrayerTimesDay
import com.ercan.smartwatch.data.model.UserSettings
import com.ercan.smartwatch.data.model.CalculationMethod
import com.ercan.smartwatch.data.repo.PrayerTimesRepository
import com.ercan.smartwatch.data.repo.PrayerTimesWindow
import com.ercan.smartwatch.data.store.SettingsStore
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrayerWatchFaceUseCaseTest {
    private val istanbulZone = ZoneId.of("Europe/Istanbul")
    private val today = LocalDate.of(2026, 2, 13)
    private val tomorrow = today.plusDays(1)

    @Test
    fun returnsSetupRequiredWhenSettingsAreMissing() = runTest {
        val useCase = PrayerWatchFaceUseCase(
            settingsStore = FakeSettingsStore(
                UserSettings(
                    city = "",
                    country = "",
                    methodId = 13,
                    schoolId = 1,
                    calendarMethod = "ANGLE_BASED"
                )
            ),
            repository = FakeRepository(
                result = Result.failure(IllegalStateException("Should not be called"))
            )
        )

        val state = useCase.loadState(
            ZonedDateTime.of(today, LocalTime.of(10, 0), istanbulZone)
        )

        assertThat(state is PrayerWatchFaceUiState.SetupRequired).isTrue()
    }

    @Test
    fun returnsErrorWhenRepositoryFails() = runTest {
        val useCase = PrayerWatchFaceUseCase(
            settingsStore = FakeSettingsStore(validSettings()),
            repository = FakeRepository(
                result = Result.failure(IOException("network down"))
            )
        )

        val state = useCase.loadState(
            ZonedDateTime.of(today, LocalTime.of(10, 0), istanbulZone)
        )

        assertThat(state).isInstanceOf(PrayerWatchFaceUiState.Error::class.java)
        val error = state as PrayerWatchFaceUiState.Error
        assertThat(error.message).isEqualTo("Unable to load")
    }

    @Test
    fun returnsContentWhenRepositoryProvidesData() = runTest {
        val prayerWindow = PrayerTimesWindow(
            today = day(today),
            tomorrow = day(tomorrow),
            isStale = true,
            loadedAtMillis = 0L
        )

        val useCase = PrayerWatchFaceUseCase(
            settingsStore = FakeSettingsStore(validSettings()),
            repository = FakeRepository(result = Result.success(prayerWindow))
        )

        val now = ZonedDateTime.of(today, LocalTime.of(12, 30), istanbulZone)
        val state = useCase.loadState(now)

        assertThat(state).isInstanceOf(PrayerWatchFaceUiState.Content::class.java)
        val content = state as PrayerWatchFaceUiState.Content

        val expectedTimeText = ZonedDateTime.of(today, LocalTime.of(13, 0), istanbulZone).format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
        )

        assertThat(content.nextPrayerName).isEqualTo("Dhuhr")
        assertThat(content.nextPrayerTimeText).isEqualTo(expectedTimeText)
        assertThat(content.countdownText).isEqualTo("in 00h 30m")
        assertThat(content.isStale).isTrue()
    }

    private fun validSettings(): UserSettings {
        return UserSettings(
            city = "Istanbul",
            country = "Turkey",
            methodId = 13,
            schoolId = 1,
            calendarMethod = "ANGLE_BASED"
        )
    }

    private fun day(date: LocalDate): PrayerTimesDay {
        return PrayerTimesDay(
            gregorianDate = date,
            timezone = istanbulZone.id,
            fajr = LocalTime.of(6, 0),
            dhuhr = LocalTime.of(13, 0),
            asr = LocalTime.of(16, 0),
            maghrib = LocalTime.of(18, 30),
            isha = LocalTime.of(20, 0)
        )
    }

    private class FakeSettingsStore(
        private val settings: UserSettings
    ) : SettingsStore {
        override val settingsFlow: Flow<UserSettings> = flowOf(settings)

        override suspend fun get(): UserSettings = settings

        override suspend fun save(settings: UserSettings) = Unit
    }

    private class FakeRepository(
        private val result: Result<PrayerTimesWindow>
    ) : PrayerTimesRepository {
        override suspend fun getTodayAndTomorrow(settings: UserSettings): Result<PrayerTimesWindow> = result

        override suspend fun getCalculationMethods(): List<CalculationMethod> = emptyList()

        override suspend fun refreshCalculationMethods(): List<CalculationMethod> = emptyList()
    }
}
