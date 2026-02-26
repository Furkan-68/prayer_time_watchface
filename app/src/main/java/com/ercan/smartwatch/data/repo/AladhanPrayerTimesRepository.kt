package com.ercan.smartwatch.data.repo

import com.ercan.smartwatch.data.api.AladhanApi
import com.ercan.smartwatch.data.model.CalculationMethod
import com.ercan.smartwatch.data.model.UserSettings
import com.ercan.smartwatch.data.store.CachedPrayerTimesWindow
import com.ercan.smartwatch.data.store.CalculationMethodsCacheStore
import com.ercan.smartwatch.data.store.PrayerTimesCacheStore
import com.ercan.smartwatch.domain.PrayerTimeParser
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AladhanPrayerTimesRepository(
    private val api: AladhanApi,
    private val timingsCacheStore: PrayerTimesCacheStore,
    private val methodsCacheStore: CalculationMethodsCacheStore,
    private val clock: Clock = Clock.systemDefaultZone()
) : PrayerTimesRepository {

    override suspend fun getTodayAndTomorrow(settings: UserSettings): Result<PrayerTimesWindow> {
        if (!settings.isConfigured()) {
            return Result.failure(IllegalStateException("Settings are not configured"))
        }

        val settingsHash = settings.settingsHash()
        val cachedWindow = timingsCacheStore.read()

        if (cachedWindow != null && isFresh(cachedWindow, settingsHash)) {
            return Result.success(
                PrayerTimesWindow(
                    today = cachedWindow.today,
                    tomorrow = cachedWindow.tomorrow,
                    isStale = false,
                    loadedAtMillis = cachedWindow.updatedAtMillis
                )
            )
        }

        return try {
            val todayDate = LocalDate.now(clock)
            val tomorrowDate = todayDate.plusDays(1)

            val todayResponse = api.getTimingsByCity(
                date = todayDate.format(API_DATE_FORMATTER),
                city = settings.city,
                country = settings.country,
                method = settings.methodId,
                school = settings.schoolId,
                calendarMethod = settings.calendarMethod
            )

            val tomorrowResponse = api.getTimingsByCity(
                date = tomorrowDate.format(API_DATE_FORMATTER),
                city = settings.city,
                country = settings.country,
                method = settings.methodId,
                school = settings.schoolId,
                calendarMethod = settings.calendarMethod
            )

            val today = PrayerTimeParser.parse(todayResponse.data)
            val tomorrow = PrayerTimeParser.parse(tomorrowResponse.data)
            val nowMillis = clock.millis()

            timingsCacheStore.write(
                CachedPrayerTimesWindow(
                    settingsHash = settingsHash,
                    updatedAtMillis = nowMillis,
                    today = today,
                    tomorrow = tomorrow
                )
            )

            Result.success(
                PrayerTimesWindow(
                    today = today,
                    tomorrow = tomorrow,
                    isStale = false,
                    loadedAtMillis = nowMillis
                )
            )
        } catch (error: Throwable) {
            if (cachedWindow != null && cachedWindow.settingsHash == settingsHash) {
                Result.success(
                    PrayerTimesWindow(
                        today = cachedWindow.today,
                        tomorrow = cachedWindow.tomorrow,
                        isStale = true,
                        loadedAtMillis = cachedWindow.updatedAtMillis
                    )
                )
            } else {
                Result.failure(error)
            }
        }
    }

    override suspend fun getCalculationMethods(): List<CalculationMethod> {
        val cached = methodsCacheStore.read()
        return cached.ifEmpty { fallbackMethods() }
    }

    override suspend fun refreshCalculationMethods(): List<CalculationMethod> {
        return try {
            val methods = api.getMethods()
                .data
                .values
                .map { CalculationMethod(id = it.id, name = it.name) }
                .sortedBy { it.id }

            if (methods.isNotEmpty()) {
                methodsCacheStore.write(methods)
                methods
            } else {
                methodsCacheStore.read().ifEmpty { fallbackMethods() }
            }
        } catch (_: Throwable) {
            methodsCacheStore.read().ifEmpty { fallbackMethods() }
        }
    }

    private fun isFresh(cache: CachedPrayerTimesWindow, settingsHash: String): Boolean {
        if (cache.settingsHash != settingsHash) return false

        val age = Duration.ofMillis(clock.millis() - cache.updatedAtMillis)
        if (age > CACHE_TTL) return false

        val utcToday = LocalDate.now(clock)
        return cache.today.gregorianDate == utcToday && cache.tomorrow.gregorianDate == utcToday.plusDays(1)
    }

    private fun fallbackMethods(): List<CalculationMethod> {
        return listOf(
            CalculationMethod(1, "University of Islamic Sciences, Karachi"),
            CalculationMethod(2, "Islamic Society of North America"),
            CalculationMethod(3, "Muslim World League"),
            CalculationMethod(4, "Umm Al-Qura University, Makkah"),
            CalculationMethod(5, "Egyptian General Authority of Survey"),
            CalculationMethod(8, "Gulf Region"),
            CalculationMethod(9, "Kuwait"),
            CalculationMethod(10, "Qatar"),
            CalculationMethod(11, "Majlis Ugama Islam Singapura"),
            CalculationMethod(12, "Union Organization islamic de France"),
            CalculationMethod(13, "Diyanet Isleri Baskanligi, Turkey")
        )
    }

    private companion object {
        val CACHE_TTL: Duration = Duration.ofDays(1)
        val API_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }
}


