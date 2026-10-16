package com.vardiya.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Totals for one week or month. */
data class PeriodReport(
    val start: LocalDate,
    val end: LocalDate,
    val shiftCount: Int,
    val dayCount: Int,
    val workedMinutes: Int,
    val breakMinutes: Int,
    val overtimeMinutes: Int,
    val holidayMinutes: Int,
    val pay: Double,
)

data class Reports(
    val weekly: List<PeriodReport> = emptyList(),
    val monthly: List<PeriodReport> = emptyList(),
)

object ReportBuilder {

    /** Weekly (Monday–Sunday) and monthly totals over all shifts, newest first. */
    fun build(shifts: List<Shift>, settings: PaySettings): Reports {
        // Overtime depends on the month's running total, so every month is paid on its own first.
        val pays = shifts.map { YearMonth.from(it.localDate) }.distinct()
            .flatMap { EarningsCalculator.summarize(it, shifts, settings).shifts }

        val weekly = pays.groupBy { it.shift.localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .map { (monday, list) -> report(monday, monday.plusDays(6), list) }
            .sortedByDescending { it.start }
        val monthly = pays.groupBy { YearMonth.from(it.shift.localDate) }
            .map { (month, list) -> report(month.atDay(1), month.atEndOfMonth(), list) }
            .sortedByDescending { it.start }
        return Reports(weekly, monthly)
    }

    private fun report(start: LocalDate, end: LocalDate, pays: List<ShiftPay>) = PeriodReport(
        start = start,
        end = end,
        shiftCount = pays.size,
        dayCount = pays.map { it.shift.localDate }.distinct().size,
        workedMinutes = pays.sumOf { it.shift.workedMinutes },
        breakMinutes = pays.sumOf { it.shift.breakMinutes },
        overtimeMinutes = pays.sumOf { it.overtimeMinutes },
        holidayMinutes = pays.sumOf { it.holidayMinutes },
        pay = pays.sumOf { it.pay },
    )
}
