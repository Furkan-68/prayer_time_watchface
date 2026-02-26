package com.ercan.smartwatch

import android.content.Context
import com.ercan.smartwatch.data.api.AladhanApiClient
import com.ercan.smartwatch.data.repo.AladhanPrayerTimesRepository
import com.ercan.smartwatch.data.repo.PrayerTimesRepository
import com.ercan.smartwatch.data.store.DataStoreSettingsStore
import com.ercan.smartwatch.data.store.MethodsCacheStore
import com.ercan.smartwatch.data.store.SettingsStore
import com.ercan.smartwatch.data.store.TimingsCacheStore

object ServiceLocator {
    @Volatile
    private var settingsStore: SettingsStore? = null

    @Volatile
    private var timingsCacheStore: TimingsCacheStore? = null

    @Volatile
    private var methodsCacheStore: MethodsCacheStore? = null

    @Volatile
    private var repository: PrayerTimesRepository? = null

    fun settingsStore(context: Context): SettingsStore {
        return settingsStore ?: synchronized(this) {
            settingsStore ?: DataStoreSettingsStore(context.applicationContext).also {
                settingsStore = it
            }
        }
    }

    fun timingsCacheStore(context: Context): TimingsCacheStore {
        return timingsCacheStore ?: synchronized(this) {
            timingsCacheStore ?: TimingsCacheStore(context.applicationContext).also {
                timingsCacheStore = it
            }
        }
    }

    fun methodsCacheStore(context: Context): MethodsCacheStore {
        return methodsCacheStore ?: synchronized(this) {
            methodsCacheStore ?: MethodsCacheStore(context.applicationContext).also {
                methodsCacheStore = it
            }
        }
    }

    fun prayerRepository(context: Context): PrayerTimesRepository {
        return repository ?: synchronized(this) {
            repository ?: AladhanPrayerTimesRepository(
                api = AladhanApiClient.create(),
                timingsCacheStore = timingsCacheStore(context),
                methodsCacheStore = methodsCacheStore(context)
            ).also {
                repository = it
            }
        }
    }
}
