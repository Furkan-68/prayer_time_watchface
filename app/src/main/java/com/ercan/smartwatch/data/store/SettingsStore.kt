package com.ercan.smartwatch.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ercan.smartwatch.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface SettingsStore {
    val settingsFlow: Flow<UserSettings>
    suspend fun get(): UserSettings
    suspend fun save(settings: UserSettings)
}

class DataStoreSettingsStore(
    context: Context
) : SettingsStore {
    private val dataStore = context.appDataStore

    override val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            city = prefs[Keys.CITY].orEmpty(),
            country = prefs[Keys.COUNTRY].orEmpty(),
            methodId = prefs[Keys.METHOD_ID] ?: UserSettings.DEFAULT_METHOD_ID,
            schoolId = prefs[Keys.SCHOOL_ID] ?: UserSettings.DEFAULT_SCHOOL_ID,
            calendarMethod = prefs[Keys.CALENDAR_METHOD] ?: UserSettings.DEFAULT_CALENDAR_METHOD
        )
    }

    override suspend fun get(): UserSettings = settingsFlow.first()

    override suspend fun save(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.CITY] = settings.city.trim()
            prefs[Keys.COUNTRY] = settings.country.trim()
            prefs[Keys.METHOD_ID] = settings.methodId
            prefs[Keys.SCHOOL_ID] = settings.schoolId
            prefs[Keys.CALENDAR_METHOD] = settings.calendarMethod
        }
    }

    private object Keys {
        val CITY = stringPreferencesKey("city")
        val COUNTRY = stringPreferencesKey("country")
        val METHOD_ID = intPreferencesKey("method_id")
        val SCHOOL_ID = intPreferencesKey("school_id")
        val CALENDAR_METHOD = stringPreferencesKey("calendar_method")
    }
}
