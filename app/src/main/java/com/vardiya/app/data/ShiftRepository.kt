package com.vardiya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.domain.Shift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "vardiya")

/** Persists shifts and pay settings as JSON in Preferences DataStore. */
class ShiftRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val shiftListSerializer = ListSerializer(Shift.serializer())

    val shifts: Flow<List<Shift>> = context.dataStore.data.map { prefs ->
        prefs[SHIFTS]?.let { runCatching { json.decodeFromString(shiftListSerializer, it) }.getOrNull() }.orEmpty()
    }

    val settings: Flow<PaySettings> = context.dataStore.data.map { prefs ->
        prefs[SETTINGS]?.let { runCatching { json.decodeFromString(PaySettings.serializer(), it) }.getOrNull() }
            ?: PaySettings()
    }

    /** Epoch millis when the running shift was started, or null when no shift is running. */
    val activeShiftStart: Flow<Long?> = context.dataStore.data.map { it[ACTIVE_START] }

    suspend fun setActiveShiftStart(epochMillis: Long?) {
        context.dataStore.edit { prefs ->
            if (epochMillis == null) prefs.remove(ACTIVE_START) else prefs[ACTIVE_START] = epochMillis
        }
    }

    suspend fun upsertShift(shift: Shift) = editShifts { list ->
        val index = list.indexOfFirst { it.id == shift.id }
        if (index >= 0) list.toMutableList().apply { set(index, shift) } else list + shift
    }

    suspend fun deleteShift(id: Long) = editShifts { list -> list.filterNot { it.id == id } }

    suspend fun saveSettings(settings: PaySettings) {
        context.dataStore.edit { it[SETTINGS] = json.encodeToString(PaySettings.serializer(), settings) }
    }

    private suspend fun editShifts(transform: (List<Shift>) -> List<Shift>) {
        context.dataStore.edit { prefs ->
            val current = prefs[SHIFTS]?.let { runCatching { json.decodeFromString(shiftListSerializer, it) }.getOrNull() }.orEmpty()
            prefs[SHIFTS] = json.encodeToString(shiftListSerializer, transform(current))
        }
    }

    private companion object {
        val SHIFTS = stringPreferencesKey("shifts")
        val SETTINGS = stringPreferencesKey("settings")
        val ACTIVE_START = longPreferencesKey("active_shift_start")
    }
}
