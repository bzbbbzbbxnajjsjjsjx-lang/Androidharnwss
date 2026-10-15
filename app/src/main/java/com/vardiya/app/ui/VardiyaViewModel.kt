package com.vardiya.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.app.data.ShiftRepository
import com.vardiya.app.domain.EarningsCalculator
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.domain.Shift
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

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
}
