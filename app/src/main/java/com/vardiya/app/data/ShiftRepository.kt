package com.vardiya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vardiya.app.domain.ActiveShift
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.domain.Shift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    /** The running shift with its breaks, or null when no shift is running. */
    val activeShift: Flow<ActiveShift?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_START]?.let { ActiveShift(it, prefs[BREAK_START], prefs[PAST_BREAK] ?: 0L) }
    }.distinctUntilChanged()

    suspend fun startShift(nowMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_START] = nowMillis
            prefs.remove(BREAK_START)
            prefs.remove(PAST_BREAK)
        }
    }

    suspend fun startBreak(nowMillis: Long) {
        context.dataStore.edit { prefs ->
            if (prefs[ACTIVE_START] != null && prefs[BREAK_START] == null) prefs[BREAK_START] = nowMillis
        }
    }

    suspend fun endBreak(nowMillis: Long) {
        context.dataStore.edit { prefs ->
            val breakStart = prefs[BREAK_START] ?: return@edit
            prefs[PAST_BREAK] = (prefs[PAST_BREAK] ?: 0L) + (nowMillis - breakStart).coerceAtLeast(0)
            prefs.remove(BREAK_START)
        }
    }

    suspend fun clearActiveShift() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACTIVE_START)
            prefs.remove(BREAK_START)
            prefs.remove(PAST_BREAK)
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
        val BREAK_START = longPreferencesKey("active_break_start")
        val PAST_BREAK = longPreferencesKey("active_past_break")
    }
}
