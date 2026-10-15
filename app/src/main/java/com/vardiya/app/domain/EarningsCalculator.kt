package com.vardiya.app.domain

import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth

data class ShiftPay(
    val shift: Shift,
    val regularMinutes: Int,
    val overtimeMinutes: Int,
    val holidayMinutes: Int,
    val pay: Double,
)

data class MonthSummary(
    val month: YearMonth,
    val settings: PaySettings,
    val shifts: List<ShiftPay>,
) {
    val regularMinutes: Int = shifts.sumOf { it.regularMinutes }
    val overtimeMinutes: Int = shifts.sumOf { it.overtimeMinutes }
    val holidayMinutes: Int = shifts.sumOf { it.holidayMinutes }
    val workedMinutes: Int = regularMinutes + overtimeMinutes + holidayMinutes

    private val minuteRate = settings.hourlyRate / 60.0
    val regularPay: Double = regularMinutes * minuteRate
    val overtimePay: Double = overtimeMinutes * minuteRate * settings.overtimeMultiplier
    val holidayPay: Double = holidayMinutes * minuteRate * settings.holidayMultiplier
    val totalPay: Double = regularPay + overtimePay + holidayPay

    /** Share of the monthly salary earned so far (can exceed 1 with overtime). */
    val salaryProgress: Float =
        if (settings.monthlySalary > 0) (totalPay / settings.monthlySalary).toFloat() else 0f

    /** Share of the monthly hour basis worked so far. */
    val hoursProgress: Float =
        if (settings.monthlyHours > 0) (workedMinutes / (settings.monthlyHours * 60)).toFloat() else 0f

    fun payOn(date: java.time.LocalDate): Double = shifts.filter { it.shift.localDate == date }.sumOf { it.pay }
}

object EarningsCalculator {

    /**
     * Pays every shift that starts in [month]:
     * - minutes overlapping a public holiday → [PaySettings.holidayMultiplier]
     * - other minutes beyond [PaySettings.monthlyHours] for the month → [PaySettings.overtimeMultiplier]
     * - the rest → hourly rate (salary / monthly hours)
     * Breaks are deducted proportionally across the shift.
     */
    fun summarize(month: YearMonth, allShifts: List<Shift>, settings: PaySettings): MonthSummary {
        val threshold = (settings.monthlyHours * 60).toInt()
        val minuteRate = settings.hourlyRate / 60.0
        var cumulative = 0

        val pays = allShifts
            .filter { YearMonth.from(it.localDate) == month }
            .sortedBy { it.start }
            .map { shift ->
                val worked = shift.workedMinutes
                val holidayOverlap = holidayOverlapMinutes(shift.start, shift.end)
                val holiday = if (shift.spanMinutes == 0) 0 else
                    Math.round(holidayOverlap.toDouble() * worked / shift.spanMinutes).toInt()
                val nonHoliday = worked - holiday

                val before = cumulative
                cumulative += worked
                val overtimeInShift = (cumulative - threshold).coerceAtLeast(0) - (before - threshold).coerceAtLeast(0)
                val overtime = minOf(nonHoliday, overtimeInShift)
                val regular = nonHoliday - overtime

                val pay = minuteRate * (regular + overtime * settings.overtimeMultiplier + holiday * settings.holidayMultiplier)
                ShiftPay(shift, regular, overtime, holiday, pay)
            }
        return MonthSummary(month, settings, pays)
    }

    /** Minutes of [start, end) that fall on public holidays (half days count from 13:00). */
    fun holidayOverlapMinutes(start: LocalDateTime, end: LocalDateTime): Int {
        var total = 0L
        var day = start.toLocalDate()
        while (day.atStartOfDay() < end) {
            TurkishHolidays.on(day)?.let { h ->
                val from = maxOf(start, h.start)
                val to = minOf(end, h.end)
                if (from < to) total += Duration.between(from, to).toMinutes()
            }
            day = day.plusDays(1)
        }
        return total.toInt()
    }
}
