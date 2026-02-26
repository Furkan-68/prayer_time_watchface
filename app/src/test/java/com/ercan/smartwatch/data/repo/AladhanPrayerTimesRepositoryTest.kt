package com.ercan.smartwatch.data.repo

import com.google.common.truth.Truth.assertThat
import com.ercan.smartwatch.data.api.AladhanApi
import com.ercan.smartwatch.data.api.DateDto
import com.ercan.smartwatch.data.api.GregorianDateDto
import com.ercan.smartwatch.data.api.MethodDto
import com.ercan.smartwatch.data.api.MethodsResponseDto
import com.ercan.smartwatch.data.api.MetaDto
import com.ercan.smartwatch.data.api.TimingsDataDto
import com.ercan.smartwatch.data.api.TimingsResponseDto
import com.ercan.smartwatch.data.model.CalculationMethod
import com.ercan.smartwatch.data.model.PrayerTimesDay
import com.ercan.smartwatch.data.model.UserSettings
import com.ercan.smartwatch.data.store.CalculationMethodsCacheStore
import com.ercan.smartwatch.data.store.CachedPrayerTimesWindow
import com.ercan.smartwatch.data.store.PrayerTimesCacheStore
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AladhanPrayerTimesRepositoryTest {
    private val fixedInstant = Instant.parse("2026-02-13T10:00:00Z")
    private val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    private val settings = UserSettings(
        city = "Istanbul",
        country = "Turkey",
        methodId = 13,
        schoolId = 1,
        calendarMethod = "ANGLE_BASED"
    )

    @Test
    fun returnsStaleCacheWhenNetworkFails() = runTest {
        val today = LocalDate.of(2026, 2, 13)
        val tomorrow = today.plusDays(1)
        val staleCache = CachedPrayerTimesWindow(
            settingsHash = settings.settingsHash(),
            updatedAtMillis = fixedInstant.minusSeconds(90_000).toEpochMilli(),
            today = day(today),
            tomorrow = day(tomorrow)
        )

        val cache = InMemoryPrayerTimesCache(staleCache)
        val methodsCache = InMemoryMethodsCache()
        val api = object : AladhanApi {
            override suspend fun getTimingsByCity(
                date: String,
                city: String,
                country: String,
                method: Int,
                school: Int,
                calendarMethod: String
            ): TimingsResponseDto {
                throw IOException("network down")
            }

            override suspend fun getMethods(): MethodsResponseDto {
                return MethodsResponseDto(code = 200, status = "OK", data = emptyMap())
            }
        }

        val repository = AladhanPrayerTimesRepository(
            api = api,
            timingsCacheStore = cache,
            methodsCacheStore = methodsCache,
            clock = fixedClock
        )

        val result = repository.getTodayAndTomorrow(settings)

        assertThat(result.isSuccess).isTrue()
        val window = result.getOrThrow()
        assertThat(window.isStale).isTrue()
        assertThat(window.today.gregorianDate).isEqualTo(today)
    }

    @Test
    fun fetchesMethodsAndCachesThem() = runTest {
        val cache = InMemoryPrayerTimesCache(null)
        val methodsCache = InMemoryMethodsCache()
        val api = object : AladhanApi {
            override suspend fun getTimingsByCity(
                date: String,
                city: String,
                country: String,
                method: Int,
                school: Int,
                calendarMethod: String
            ): TimingsResponseDto {
                error("not needed")
            }

            override suspend fun getMethods(): MethodsResponseDto {
                return MethodsResponseDto(
                    code = 200,
                    status = "OK",
                    data = mapOf(
                        "13" to MethodDto(id = 13, name = "Diyanet Isleri Baskanligi, Turkey")
                    )
                )
            }
        }

        val repository = AladhanPrayerTimesRepository(
            api = api,
            timingsCacheStore = cache,
            methodsCacheStore = methodsCache,
            clock = fixedClock
        )

        val methods = repository.refreshCalculationMethods()

        assertThat(methods).containsExactly(
            CalculationMethod(id = 13, name = "Diyanet Isleri Baskanligi, Turkey")
        )
        assertThat(methodsCache.cached).containsExactly(
            CalculationMethod(id = 13, name = "Diyanet Isleri Baskanligi, Turkey")
        )
    }

    @Test
    fun getCalculationMethodsReturnsCachedValuesWithoutNetwork() = runTest {
        val cache = InMemoryPrayerTimesCache(null)
        val methodsCache = InMemoryMethodsCache().apply {
            cached = listOf(CalculationMethod(id = 13, name = "Diyanet Isleri Baskanligi, Turkey"))
        }
        val api = object : AladhanApi {
            override suspend fun getTimingsByCity(
                date: String,
                city: String,
                country: String,
                method: Int,
                school: Int,
                calendarMethod: String
            ): TimingsResponseDto {
                error("not needed")
            }

            override suspend fun getMethods(): MethodsResponseDto {
                throw IOException("should not be called")
            }
        }

        val repository = AladhanPrayerTimesRepository(
            api = api,
            timingsCacheStore = cache,
            methodsCacheStore = methodsCache,
            clock = fixedClock
        )

        val methods = repository.getCalculationMethods()

        assertThat(methods).containsExactly(
            CalculationMethod(id = 13, name = "Diyanet Isleri Baskanligi, Turkey")
        )
    }

    private fun day(date: LocalDate): PrayerTimesDay {
        return PrayerTimesDay(
            gregorianDate = date,
            timezone = "Europe/Istanbul",
            fajr = LocalTime.of(6, 0),
            dhuhr = LocalTime.of(13, 0),
            asr = LocalTime.of(16, 0),
            maghrib = LocalTime.of(18, 30),
            isha = LocalTime.of(20, 0)
        )
    }

    private class InMemoryPrayerTimesCache(
        initial: CachedPrayerTimesWindow?
    ) : PrayerTimesCacheStore {
        private var cache = initial

        override suspend fun read(): CachedPrayerTimesWindow? = cache

        override suspend fun write(window: CachedPrayerTimesWindow) {
            cache = window
        }
    }

    private class InMemoryMethodsCache : CalculationMethodsCacheStore {
        var cached: List<CalculationMethod> = emptyList()

        override suspend fun read(): List<CalculationMethod> = cached

        override suspend fun write(methods: List<CalculationMethod>) {
            cached = methods
        }
    }
}
