package com.vardiya.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ActiveShiftAndReportsTest {

    private val zone = ZoneOffset.UTC
    private fun millis(hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 10, 14, hour, minute).toInstant(zone).toEpochMilli()

    @Test
    fun breakStopsWorkedTime() {
        // 08:00 start, break 12:00–12:30, still on a second break since 15:50, now 16:00.
        val active = ActiveShift(
            startMillis = millis(8),
            breakStartMillis = millis(15, 50),
            pastBreakMillis = 30 * 60_000L,
        )
        assertEquals(40 * 60_000L, active.breakMillis(millis(16)))
        assertEquals((8 * 60 - 40) * 60_000L, active.workedMillis(millis(16)))
    }

    @Test
    fun finishedShiftRecordsBreakMinutes() {
        val active = ActiveShift(startMillis = millis(8), pastBreakMillis = 45 * 60_000L)
        val shift = active.toShift(millis(16), zone)!!
        assertEquals(8 * 60, shift.startMinute)
        assertEquals(16 * 60, shift.endMinute)
        assertEquals(45, shift.breakMinutes)
        assertEquals(8 * 60 - 45, shift.workedMinutes)
    }

    @Test
    fun shiftThatWasOnlyBreakIsNotSaved() {
        val active = ActiveShift(startMillis = millis(8), breakStartMillis = millis(8))
        assertNull(active.toShift(millis(9), zone))
    }

    @Test
    fun customHourlyRateReplacesSalary() {
        val settings = PaySettings(useHourlyRate = true, customHourlyRate = 150.0)
        assertEquals(150.0, settings.hourlyRate, 0.0)
    }

    @Test
    fun reportsGroupByWeekAndMonth() {
        val settings = PaySettings(useHourlyRate = true, customHourlyRate = 100.0)
        val shifts = listOf(
            // Wednesday and Friday of the same week, then the next Monday (2026-10-26 is not a holiday).
            Shift(1, "2026-10-14", 8 * 60, 16 * 60),
            Shift(2, "2026-10-16", 8 * 60, 12 * 60, breakMinutes = 30),
            Shift(3, "2026-10-19", 8 * 60, 16 * 60),
            Shift(4, "2026-11-02", 8 * 60, 10 * 60),
        )
        val reports = ReportBuilder.build(shifts, settings)

        assertEquals(listOf("2026-11-02", "2026-10-19", "2026-10-12"), reports.weekly.map { it.start.toString() })
        val week = reports.weekly.last()
        assertEquals(2, week.shiftCount)
        assertEquals(8 * 60 + 3 * 60 + 30, week.workedMinutes)
        assertEquals(30, week.breakMinutes)
        assertEquals(11.5 * 100, week.pay, 0.01)

        assertEquals(listOf("2026-11-01", "2026-10-01"), reports.monthly.map { it.start.toString() })
        assertEquals(3, reports.monthly.last().shiftCount)
        assertEquals(3, reports.monthly.last().dayCount)
    }
}
