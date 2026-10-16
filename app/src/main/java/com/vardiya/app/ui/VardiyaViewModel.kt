package com.vardiya.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.app.data.ShiftRepository
import com.vardiya.app.domain.EarningsCalculator
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.domain.Shift
import com.vardiya.app.domain.TurkishHolidays
import com.vardiya.app.domain.shiftFromClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

    /** Current time every second while a shift is running, null otherwise. Only the summary screen uses it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val secondClock: Flow<Long?> = repository.activeShiftStart.flatMapLatest { start ->
        if (start == null) flowOf<Long?>(null) else flow<Long?> {
            while (true) {
                emit(System.currentTimeMillis())
                delay(1_000)
            }
        }
    }

    /** Ticks each time the running shift completes a full minute, null while no shift runs. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val minuteClock: Flow<Long?> = repository.activeShiftStart.flatMapLatest { start ->
        if (start == null) flowOf<Long?>(null) else flow<Long?> {
            while (true) {
                val now = System.currentTimeMillis()
                emit(now)
                // Sleep until just after the next full minute of the shift.
                delay(60_000 - (now - start).coerceAtLeast(0) % 60_000 + 50)
            }
        }
    }

    /**
     * Month totals in whole minutes, the same granularity a saved shift has. A running shift is
     * included. Changes at most once a minute, so Calendar and Shifts do not recompose every second.
     */
    val summary: StateFlow<MonthSummary> =
        combine(_month, repository.shifts, repository.settings, repository.activeShiftStart, minuteClock) { month, shifts, settings, start, now ->
            if (start == null || now == null) return@combine EarningsCalculator.summarize(month, shifts, settings)
            val zone = ZoneId.systemDefault()
            val live = shiftFromClock(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(start), zone),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone),
            )?.copy(id = Shift.LIVE_ID)
            EarningsCalculator.summarize(month, shifts + listOfNotNull(live), settings)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            EarningsCalculator.summarize(YearMonth.now(), emptyList(), PaySettings()),
        )

    /**
     * Pay for the seconds of the running shift's current minute, ticking every second. Collected
     * only by the summary screen, so the per-second work stops when another tab is open.
     */
    val partialPay: StateFlow<Double> =
        combine(summary, repository.activeShiftStart, secondClock) { summary, start, now ->
            if (start == null || now == null) return@combine 0.0
            val zone = ZoneId.systemDefault()
            val startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(start), zone)
            if (YearMonth.from(startTime) != summary.month) return@combine 0.0
            val nowTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
            val settings = summary.settings
            val seconds = ((now - start) / 1000 % 60).toInt()
            val multiplier = when {
                TurkishHolidays.on(nowTime.toLocalDate())?.let { !nowTime.isBefore(it.start) } == true -> settings.holidayMultiplier
                summary.workedMinutes >= settings.monthlyHours * 60 -> settings.overtimeMultiplier
                else -> 1.0
            }
            settings.hourlyRate / 3600.0 * seconds * multiplier
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

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
