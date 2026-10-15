package com.vardiya.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

enum class HolidayKind { NATIONAL, RELIGIOUS }

/**
 * A public holiday in Türkiye. Half-day holidays (arife, 28 Ekim) start at 13:00.
 */
data class Holiday(
    val date: LocalDate,
    val name: String,
    val kind: HolidayKind,
    val halfDay: Boolean = false,
) {
    val start: LocalDateTime
        get() = if (halfDay) date.atTime(HALF_DAY_START) else date.atStartOfDay()
    val end: LocalDateTime
        get() = date.plusDays(1).atStartOfDay()

    companion object {
        val HALF_DAY_START: LocalTime = LocalTime.of(13, 0)
    }
}

/** Official public holidays (2429 sayılı Ulusal Bayram ve Genel Tatiller Kanunu). */
object TurkishHolidays {

    /**
     * First day of Ramazan Bayramı and Kurban Bayramı. Precomputed from the Umm al-Qura
     * calendar; matches the Diyanet calendar for 2024–2027. Later years may shift by one
     * day once Diyanet announces them.
     */
    private val religiousFeasts: List<Pair<LocalDate, LocalDate>> = listOf(
        LocalDate.of(2024, 4, 10) to LocalDate.of(2024, 6, 16),
        LocalDate.of(2025, 3, 30) to LocalDate.of(2025, 6, 6),
        LocalDate.of(2026, 3, 20) to LocalDate.of(2026, 5, 27),
        LocalDate.of(2027, 3, 9) to LocalDate.of(2027, 5, 16),
        LocalDate.of(2028, 2, 26) to LocalDate.of(2028, 5, 5),
        LocalDate.of(2029, 2, 14) to LocalDate.of(2029, 4, 24),
        LocalDate.of(2030, 2, 4) to LocalDate.of(2030, 4, 13),
        LocalDate.of(2031, 1, 24) to LocalDate.of(2031, 4, 2),
        LocalDate.of(2032, 1, 14) to LocalDate.of(2032, 3, 22),
        LocalDate.of(2033, 1, 3) to LocalDate.of(2033, 3, 12),
        LocalDate.of(2033, 12, 23) to LocalDate.of(2034, 3, 1),
        LocalDate.of(2034, 12, 12) to LocalDate.of(2035, 2, 19),
        LocalDate.of(2035, 12, 1) to LocalDate.of(2036, 2, 8),
        LocalDate.of(2036, 11, 19) to LocalDate.of(2037, 1, 27),
    )

    private val religiousHolidays: List<Holiday> = religiousFeasts.flatMap { (ramazan, kurban) ->
        buildList {
            add(Holiday(ramazan.minusDays(1), "Ramazan Bayramı Arifesi", HolidayKind.RELIGIOUS, halfDay = true))
            for (i in 0 until 3) {
                add(Holiday(ramazan.plusDays(i.toLong()), "Ramazan Bayramı ${i + 1}. Gün", HolidayKind.RELIGIOUS))
            }
            add(Holiday(kurban.minusDays(1), "Kurban Bayramı Arifesi", HolidayKind.RELIGIOUS, halfDay = true))
            for (i in 0 until 4) {
                add(Holiday(kurban.plusDays(i.toLong()), "Kurban Bayramı ${i + 1}. Gün", HolidayKind.RELIGIOUS))
            }
        }
    }

    private fun nationalHolidays(year: Int): List<Holiday> = listOf(
        Holiday(LocalDate.of(year, 1, 1), "Yılbaşı", HolidayKind.NATIONAL),
        Holiday(LocalDate.of(year, 4, 23), "Ulusal Egemenlik ve Çocuk Bayramı", HolidayKind.NATIONAL),
        Holiday(LocalDate.of(year, 5, 1), "Emek ve Dayanışma Günü", HolidayKind.NATIONAL),
        Holiday(LocalDate.of(year, 5, 19), "Atatürk'ü Anma, Gençlik ve Spor Bayramı", HolidayKind.NATIONAL),
        Holiday(LocalDate.of(year, 7, 15), "Demokrasi ve Milli Birlik Günü", HolidayKind.NATIONAL),
        Holiday(LocalDate.of(year, 8, 30), "Zafer Bayramı", HolidayKind.NATIONAL),
        Holiday(LocalDate.of(year, 10, 28), "Cumhuriyet Bayramı Arifesi", HolidayKind.NATIONAL, halfDay = true),
        Holiday(LocalDate.of(year, 10, 29), "Cumhuriyet Bayramı", HolidayKind.NATIONAL),
    )

    private val cache = HashMap<Int, Map<LocalDate, Holiday>>()

    /** All holidays of a year keyed by date. On a clash the holiday covering more of the day wins. */
    @Synchronized
    fun forYear(year: Int): Map<LocalDate, Holiday> = cache.getOrPut(year) {
        val map = LinkedHashMap<LocalDate, Holiday>()
        nationalHolidays(year).forEach { map[it.date] = it }
        religiousHolidays.filter { it.date.year == year }.forEach { religious ->
            val existing = map[religious.date]
            // Keep whichever covers more of the day; a full national day beats a religious arife.
            val keepExisting = existing != null && !existing.halfDay && religious.halfDay
            if (!keepExisting) map[religious.date] = religious
        }
        map.toSortedMap()
    }

    fun on(date: LocalDate): Holiday? = forYear(date.year)[date]

    fun inMonth(month: YearMonth): List<Holiday> =
        forYear(month.year).values.filter { YearMonth.from(it.date) == month }

    fun upcoming(from: LocalDate, count: Int): List<Holiday> =
        (forYear(from.year).values + forYear(from.year + 1).values)
            .filter { !it.date.isBefore(from) }
            .take(count)
}
