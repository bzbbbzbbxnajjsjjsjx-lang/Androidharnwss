package com.vardiya.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.app.data.ShiftRepository
import com.vardiya.app.domain.ActiveShift
import com.vardiya.app.domain.EarningsCalculator
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.domain.ReportBuilder
import com.vardiya.app.domain.Reports
import com.vardiya.app.domain.Shift
import com.vardiya.app.domain.TurkishHolidays
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

    /** The running shift with its breaks, null when none is running. */
    val activeShift: StateFlow<ActiveShift?> =
        repository.activeShift.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Current time every second while a shift is running, null otherwise. Only the summary screen uses it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val secondClock: Flow<Long?> = repository.activeShift.flatMapLatest { active ->
        if (active == null) flowOf<Long?>(null) else flow<Long?> {
            while (true) {
                emit(System.currentTimeMillis())
                delay(1_000)
            }
        }
    }

    /** Ticks each time the running shift completes a full worked minute, null while no shift runs. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val minuteClock: Flow<Long?> = repository.activeShift.flatMapLatest { active ->
        if (active == null) flowOf<Long?>(null) else flow<Long?> {
            while (true) {
                val now = System.currentTimeMillis()
                emit(now)
                // Worked time stands still during a break; otherwise wake just after the next full minute.
                delay(if (active.onBreak) 60_000 else 60_000 - active.workedMillis(now) % 60_000 + 50)
            }
        }
    }

    /** All saved shifts plus the running one, changing at most once a minute. */
    private val shiftsWithLive: Flow<List<Shift>> =
        combine(repository.shifts, repository.activeShift, minuteClock) { shifts, active, now ->
            val live = if (active != null && now != null) {
                active.toShift(now, ZoneId.systemDefault())?.copy(id = Shift.LIVE_ID)
            } else {
                null
            }
            shifts + listOfNotNull(live)
        }

    /**
     * Month totals in whole minutes, the same granularity a saved shift has. A running shift is
     * included. Changes at most once a minute, so Calendar and Shifts do not recompose every second.
     */
    val summary: StateFlow<MonthSummary> =
        combine(_month, shiftsWithLive, repository.settings) { month, shifts, settings ->
            EarningsCalculator.summarize(month, shifts, settings)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            EarningsCalculator.summarize(YearMonth.now(), emptyList(), PaySettings()),
        )

    /** Weekly and monthly totals over every shift. */
    val reports: StateFlow<Reports> =
        combine(shiftsWithLive, repository.settings) { shifts, settings -> ReportBuilder.build(shifts, settings) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Reports())

    /**
     * Pay for the seconds of the running shift's current worked minute, ticking every second.
     * Stands still during a break. Collected only by the summary screen, so the per-second work
     * stops when another tab is open.
     */
    val partialPay: StateFlow<Double> =
        combine(summary, repository.activeShift, secondClock) { summary, active, now ->
            if (active == null || now == null) return@combine 0.0
            val zone = ZoneId.systemDefault()
            val startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(active.startMillis), zone)
            if (YearMonth.from(startTime) != summary.month) return@combine 0.0
            val nowTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
            val settings = summary.settings
            val seconds = (active.workedMillis(now) / 1000 % 60).toInt()
            val multiplier = when {
                TurkishHolidays.on(nowTime.toLocalDate())?.let { !nowTime.isBefore(it.start) } == true -> settings.holidayMultiplier
                summary.workedMinutes >= settings.monthlyHours * 60 -> settings.overtimeMultiplier
                else -> 1.0
            }
            settings.hourlyRate / 3600.0 * seconds * multiplier
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

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
        viewModelScope.launch { repository.startShift(System.currentTimeMillis()) }
    }

    /** Starts a break, or ends the one that is running. */
    fun toggleBreak() {
        val active = activeShift.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            if (active.onBreak) repository.endBreak(now) else repository.startBreak(now)
        }
    }

    fun finishShift() {
        val active = activeShift.value ?: return
        val shift = active.toShift(System.currentTimeMillis(), ZoneId.systemDefault())
        viewModelScope.launch {
            if (shift != null) repository.upsertShift(shift.copy(id = System.currentTimeMillis()))
            repository.clearActiveShift()
            // Show the month the shift was recorded in.
            _month.value = YearMonth.from(Instant.ofEpochMilli(active.startMillis).atZone(ZoneId.systemDefault()))
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
