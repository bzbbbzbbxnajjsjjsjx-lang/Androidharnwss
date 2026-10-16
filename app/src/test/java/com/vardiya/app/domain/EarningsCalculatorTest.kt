package com.vardiya.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class EarningsCalculatorTest {

    private val settings = PaySettings() // 28.000 TL / 225 h
    private val rate = 28_000.0 / 225.0
    private var nextId = 1L

    private fun shift(date: String, start: Int, end: Int, breakMinutes: Int = 0) =
        Shift(nextId++, date, start * 60, end * 60, breakMinutes)

    @Test
    fun hourlyRateIsSalaryOverMonthlyHours() {
        assertEquals(124.444, settings.hourlyRate, 0.001)
    }

    @Test
    fun regularShiftOnWorkday() {
        // 2026-10-14 is a Wednesday, no holiday.
        val summary = EarningsCalculator.summarize(YearMonth.of(2026, 10), listOf(shift("2026-10-14", 8, 16)), settings)
        assertEquals(8 * 60, summary.regularMinutes)
        assertEquals(8 * rate, summary.totalPay, 0.01)
    }

    @Test
    fun breakIsNotPaid() {
        val summary = EarningsCalculator.summarize(YearMonth.of(2026, 10), listOf(shift("2026-10-14", 8, 16, breakMinutes = 30)), settings)
        assertEquals(7.5 * rate, summary.totalPay, 0.01)
    }

    @Test
    fun overnightShiftEndsNextDay() {
        val s = shift("2026-10-14", 22, 6)
        assertEquals(8 * 60, s.spanMinutes)
        assertEquals(LocalDate.of(2026, 10, 15), s.end.toLocalDate())
    }

    @Test
    fun holidayIsPaidDouble() {
        // 29 Ekim Cumhuriyet Bayramı.
        val summary = EarningsCalculator.summarize(YearMonth.of(2026, 10), listOf(shift("2026-10-29", 8, 16)), settings)
        assertEquals(8 * 60, summary.holidayMinutes)
        assertEquals(8 * rate * 2, summary.totalPay, 0.01)
    }

    @Test
    fun arifeCountsFromOnePm() {
        // 28 Ekim yarım gün: 08–16 → 08–13 normal, 13–16 tatil.
        val summary = EarningsCalculator.summarize(YearMonth.of(2026, 10), listOf(shift("2026-10-28", 8, 16)), settings)
        assertEquals(5 * 60, summary.regularMinutes)
        assertEquals(3 * 60, summary.holidayMinutes)
    }

    @Test
    fun nightShiftIntoNewYearCountsHolidayPart() {
        // 31 Aralık 22:00 → 1 Ocak 06:00: 2 h normal, 6 h Yılbaşı.
        val summary = EarningsCalculator.summarize(YearMonth.of(2026, 12), listOf(shift("2026-12-31", 22, 6)), settings)
        assertEquals(2 * 60, summary.regularMinutes)
        assertEquals(6 * 60, summary.holidayMinutes)
    }

    @Test
    fun hoursBeyondMonthlyBasisAreOvertime() {
        // 30 × 8 h = 240 h in November 2026 (no holidays) → 15 h overtime.
        val shifts = (1..30).map { day -> shift("2026-11-%02d".format(day), 8, 16) }
        val summary = EarningsCalculator.summarize(YearMonth.of(2026, 11), shifts, settings)
        assertEquals(225 * 60, summary.regularMinutes)
        assertEquals(15 * 60, summary.overtimeMinutes)
        assertEquals(28_000.0 + 15 * rate * 1.5, summary.totalPay, 0.01)
        assertTrue(summary.salaryProgress > 1f)
    }

    @Test
    fun clockShiftKeepsStartAndLength() {
        val s = shiftFromClock(LocalDateTime.of(2026, 10, 14, 22, 15), LocalDateTime.of(2026, 10, 15, 6, 40))!!
        assertEquals("2026-10-14", s.date)
        assertEquals(22 * 60 + 15, s.startMinute)
        assertEquals(8 * 60 + 25, s.workedMinutes)
    }

    @Test
    fun clockShiftUnderOneMinuteIsDropped() {
        val t = LocalDateTime.of(2026, 10, 14, 8, 0)
        assertNull(shiftFromClock(t, t.plusSeconds(30)))
    }

    @Test
    fun clockShiftIsCappedAtOneDay() {
        val t = LocalDateTime.of(2026, 10, 14, 8, 0)
        assertEquals(24 * 60, shiftFromClock(t, t.plusHours(30))!!.workedMinutes)
    }

    @Test
    fun partialMinutePayIsAddedToTotal() {
        val summary = EarningsCalculator.summarize(YearMonth.of(2026, 10), listOf(shift("2026-10-14", 8, 9)), settings, partialMinutePay = 1.0)
        assertEquals(rate + 1.0, summary.totalPay, 0.001)
    }

    @Test
    fun shiftsOfOtherMonthsAreIgnored() {
        val summary = EarningsCalculator.summarize(YearMonth.of(2026, 10), listOf(shift("2026-09-30", 8, 16)), settings)
        assertEquals(0.0, summary.totalPay, 0.0)
    }
}

class TurkishHolidaysTest {

    @Test
    fun religiousHolidays2026() {
        assertEquals("Ramazan Bayramı 1. Gün", TurkishHolidays.on(LocalDate.of(2026, 3, 20))?.name)
        assertTrue(TurkishHolidays.on(LocalDate.of(2026, 3, 19))!!.halfDay)
        assertEquals("Kurban Bayramı 4. Gün", TurkishHolidays.on(LocalDate.of(2026, 5, 30))?.name)
    }

    @Test
    fun nationalHolidays() {
        assertEquals("Zafer Bayramı", TurkishHolidays.on(LocalDate.of(2027, 8, 30))?.name)
        assertEquals("Demokrasi ve Milli Birlik Günü", TurkishHolidays.on(LocalDate.of(2026, 7, 15))?.name)
        assertNull(TurkishHolidays.on(LocalDate.of(2026, 10, 14)))
    }

    @Test
    fun yearHasAllHolidayDays() {
        // 8 national + 2 arife + 3 Ramazan + 4 Kurban = 17 entries in 2026.
        assertEquals(17, TurkishHolidays.forYear(2026).size)
    }

    @Test
    fun upcomingCrossesYearEnd() {
        val next = TurkishHolidays.upcoming(LocalDate.of(2026, 12, 1), 1)
        assertEquals(LocalDate.of(2027, 1, 1), next.single().date)
    }
}
