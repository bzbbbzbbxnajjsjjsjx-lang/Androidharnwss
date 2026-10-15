package com.vardiya.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.app.data.ShiftRepository
import com.vardiya.app.domain.EarningsCalculator
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.domain.Shift
import com.vardiya.app.domain.shiftFromClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class VardiyaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShiftRepository(application)

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    val settings: StateFlow<PaySettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaySettings())

    val summary: StateFlow<MonthSummary> =
        combine(_month, repository.shifts, repository.settings) { month, shifts, settings ->
            EarningsCalculator.summarize(month, shifts, settings)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            EarningsCalculator.summarize(YearMonth.now(), emptyList(), PaySettings()),
        )

    /** Start time of the running shift (epoch millis), null when none is running. */
    val activeShiftStart: StateFlow<Long?> =
        repository.activeShiftStart.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** False until stored data is loaded and the opening shape animation has played. */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    init {
        viewModelScope.launch {
            repository.shifts.first()
            delay(OPENING_ANIMATION_MS)
            _ready.value = true
        }
    }

    fun startShift() {
        viewModelScope.launch { repository.setActiveShiftStart(System.currentTimeMillis()) }
    }

    fun finishShift() {
        val startMillis = activeShiftStart.value ?: return
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMillis), zone)
        val shift = shiftFromClock(start, LocalDateTime.now(zone))
        viewModelScope.launch {
            if (shift != null) repository.upsertShift(shift.copy(id = System.currentTimeMillis()))
            repository.setActiveShiftStart(null)
            // Show the month the shift was recorded in.
            _month.value = YearMonth.from(start)
        }
    }

    fun previousMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
    fun currentMonth() { _month.value = YearMonth.now() }

    fun saveShift(shift: Shift) {
        val id = if (shift.id == 0L) System.currentTimeMillis() else shift.id
        viewModelScope.launch { repository.upsertShift(shift.copy(id = id)) }
    }

    fun deleteShift(id: Long) {
        viewModelScope.launch { repository.deleteShift(id) }
    }

    fun saveSettings(settings: PaySettings) {
        viewModelScope.launch { repository.saveSettings(settings) }
    }

    private companion object {
        const val OPENING_ANIMATION_MS = 1_200L
    }
}
