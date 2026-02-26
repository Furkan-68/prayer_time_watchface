package com.ercan.smartwatch.data.store

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ercan.smartwatch.data.model.PrayerTimesDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

data class CachedPrayerTimesWindow(
    val settingsHash: String,
    val updatedAtMillis: Long,
    val today: PrayerTimesDay,
    val tomorrow: PrayerTimesDay
)

interface PrayerTimesCacheStore {
    suspend fun read(): CachedPrayerTimesWindow?
    suspend fun write(window: CachedPrayerTimesWindow)
}

class TimingsCacheStore(
    context: Context
) : PrayerTimesCacheStore {
    private val dataStore = context.appDataStore
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override suspend fun read(): CachedPrayerTimesWindow? {
        val prefs = dataStore.data.first()
        val settingsHash = prefs[Keys.SETTINGS_HASH] ?: return null
        val updatedAt = prefs[Keys.UPDATED_AT] ?: return null

        val today = readDay(
            date = prefs[Keys.TODAY_DATE],
            timezone = prefs[Keys.TODAY_TZ],
            fajr = prefs[Keys.TODAY_FAJR],
            dhuhr = prefs[Keys.TODAY_DHUHR],
            asr = prefs[Keys.TODAY_ASR],
            maghrib = prefs[Keys.TODAY_MAGHRIB],
            isha = prefs[Keys.TODAY_ISHA]
        ) ?: return null

        val tomorrow = readDay(
            date = prefs[Keys.TOMORROW_DATE],
            timezone = prefs[Keys.TOMORROW_TZ],
            fajr = prefs[Keys.TOMORROW_FAJR],
            dhuhr = prefs[Keys.TOMORROW_DHUHR],
            asr = prefs[Keys.TOMORROW_ASR],
            maghrib = prefs[Keys.TOMORROW_MAGHRIB],
            isha = prefs[Keys.TOMORROW_ISHA]
        ) ?: return null

        return CachedPrayerTimesWindow(
            settingsHash = settingsHash,
            updatedAtMillis = updatedAt,
            today = today,
            tomorrow = tomorrow
        )
    }

    override suspend fun write(window: CachedPrayerTimesWindow) {
        dataStore.edit { prefs ->
            prefs[Keys.SETTINGS_HASH] = window.settingsHash
            prefs[Keys.UPDATED_AT] = window.updatedAtMillis
            writeDay(prefs, DayPrefix.TODAY, window.today)
            writeDay(prefs, DayPrefix.TOMORROW, window.tomorrow)
        }
    }

    private fun readDay(
        date: String?,
        timezone: String?,
        fajr: String?,
        dhuhr: String?,
        asr: String?,
        maghrib: String?,
        isha: String?
    ): PrayerTimesDay? {
        if (date == null || timezone == null || fajr == null || dhuhr == null || asr == null || maghrib == null || isha == null) {
            return null
        }

        return PrayerTimesDay(
            gregorianDate = LocalDate.parse(date, dateFormatter),
            timezone = timezone,
            fajr = LocalTime.parse(fajr, timeFormatter),
            dhuhr = LocalTime.parse(dhuhr, timeFormatter),
            asr = LocalTime.parse(asr, timeFormatter),
            maghrib = LocalTime.parse(maghrib, timeFormatter),
            isha = LocalTime.parse(isha, timeFormatter)
        )
    }

    private fun writeDay(
        prefs: MutablePreferences,
        prefix: DayPrefix,
        day: PrayerTimesDay
    ) {
        val dateKey = if (prefix == DayPrefix.TODAY) Keys.TODAY_DATE else Keys.TOMORROW_DATE
        val tzKey = if (prefix == DayPrefix.TODAY) Keys.TODAY_TZ else Keys.TOMORROW_TZ
        val fajrKey = if (prefix == DayPrefix.TODAY) Keys.TODAY_FAJR else Keys.TOMORROW_FAJR
        val dhuhrKey = if (prefix == DayPrefix.TODAY) Keys.TODAY_DHUHR else Keys.TOMORROW_DHUHR
        val asrKey = if (prefix == DayPrefix.TODAY) Keys.TODAY_ASR else Keys.TOMORROW_ASR
        val maghribKey = if (prefix == DayPrefix.TODAY) Keys.TODAY_MAGHRIB else Keys.TOMORROW_MAGHRIB
        val ishaKey = if (prefix == DayPrefix.TODAY) Keys.TODAY_ISHA else Keys.TOMORROW_ISHA

        prefs[dateKey] = day.gregorianDate.format(dateFormatter)
        prefs[tzKey] = day.timezone
        prefs[fajrKey] = day.fajr.format(timeFormatter)
        prefs[dhuhrKey] = day.dhuhr.format(timeFormatter)
        prefs[asrKey] = day.asr.format(timeFormatter)
        prefs[maghribKey] = day.maghrib.format(timeFormatter)
        prefs[ishaKey] = day.isha.format(timeFormatter)
    }

    private enum class DayPrefix {
        TODAY,
        TOMORROW
    }

    private object Keys {
        val SETTINGS_HASH = stringPreferencesKey("cache_settings_hash")
        val UPDATED_AT = longPreferencesKey("cache_updated_at")

        val TODAY_DATE = stringPreferencesKey("cache_today_date")
        val TODAY_TZ = stringPreferencesKey("cache_today_timezone")
        val TODAY_FAJR = stringPreferencesKey("cache_today_fajr")
        val TODAY_DHUHR = stringPreferencesKey("cache_today_dhuhr")
        val TODAY_ASR = stringPreferencesKey("cache_today_asr")
        val TODAY_MAGHRIB = stringPreferencesKey("cache_today_maghrib")
        val TODAY_ISHA = stringPreferencesKey("cache_today_isha")

        val TOMORROW_DATE = stringPreferencesKey("cache_tomorrow_date")
        val TOMORROW_TZ = stringPreferencesKey("cache_tomorrow_timezone")
        val TOMORROW_FAJR = stringPreferencesKey("cache_tomorrow_fajr")
        val TOMORROW_DHUHR = stringPreferencesKey("cache_tomorrow_dhuhr")
        val TOMORROW_ASR = stringPreferencesKey("cache_tomorrow_asr")
        val TOMORROW_MAGHRIB = stringPreferencesKey("cache_tomorrow_maghrib")
        val TOMORROW_ISHA = stringPreferencesKey("cache_tomorrow_isha")
    }
}
