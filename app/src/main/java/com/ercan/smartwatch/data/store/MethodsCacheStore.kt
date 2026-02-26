package com.ercan.smartwatch.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ercan.smartwatch.data.model.CalculationMethod
import kotlinx.coroutines.flow.first

interface CalculationMethodsCacheStore {
    suspend fun read(): List<CalculationMethod>
    suspend fun write(methods: List<CalculationMethod>)
}

class MethodsCacheStore(
    context: Context
) : CalculationMethodsCacheStore {
    private val dataStore = context.appDataStore

    override suspend fun read(): List<CalculationMethod> {
        val raw = dataStore.data.first()[Keys.METHODS] ?: return emptyList()
        return raw
            .split("\n")
            .mapNotNull { line ->
                val parts = line.split("::", limit = 2)
                if (parts.size != 2) {
                    null
                } else {
                    val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                    CalculationMethod(id = id, name = parts[1])
                }
            }
    }

    override suspend fun write(methods: List<CalculationMethod>) {
        val payload = methods.joinToString("\n") { "${it.id}::${it.name}" }
        dataStore.edit { prefs ->
            prefs[Keys.METHODS] = payload
        }
    }

    private object Keys {
        val METHODS = stringPreferencesKey("cached_methods")
    }
}
