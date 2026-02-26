package com.ercan.smartwatch.data.repo

import com.ercan.smartwatch.data.model.CalculationMethod
import com.ercan.smartwatch.data.model.PrayerTimesDay
import com.ercan.smartwatch.data.model.UserSettings

data class PrayerTimesWindow(
    val today: PrayerTimesDay,
    val tomorrow: PrayerTimesDay,
    val isStale: Boolean,
    val loadedAtMillis: Long
)

interface PrayerTimesRepository {
    suspend fun getTodayAndTomorrow(settings: UserSettings): Result<PrayerTimesWindow>
    suspend fun getCalculationMethods(): List<CalculationMethod>
    suspend fun refreshCalculationMethods(): List<CalculationMethod>
}
