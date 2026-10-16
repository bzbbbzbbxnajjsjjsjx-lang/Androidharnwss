package com.vardiya.app.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A worked shift. Times are minutes from midnight; an [endMinute] at or before
 * [startMinute] means the shift ends the next day (e.g. 16:00–00:00 or 22:00–06:00).
 */
@Serializable
data class Shift(
    val id: Long,
    val date: String,
    val startMinute: Int,
    val endMinute: Int,
    val breakMinutes: Int = 0,
    val note: String = "",
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
    val start: LocalDateTime get() = localDate.atStartOfDay().plusMinutes(startMinute.toLong())
    val end: LocalDateTime get() = start.plusMinutes(spanMinutes.toLong())

    /** Wall-clock length of the shift, including the break. */
    val spanMinutes: Int
        get() = Math.floorMod(endMinute - startMinute, MINUTES_PER_DAY).let { if (it == 0) MINUTES_PER_DAY else it }

    /** Paid minutes: span minus break. */
    val workedMinutes: Int get() = (spanMinutes - breakMinutes).coerceAtLeast(0)

    val isLive: Boolean get() = id == LIVE_ID

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        /** Id of the not-yet-saved shift that is currently running. */
        const val LIVE_ID = -1L
    }
}

@Serializable
data class PaySettings(
    /** Net monthly salary in TL. */
    val monthlySalary: Double = 28_000.0,
    /** Hours the salary covers in a month; 225 is the İş Kanunu basis (45 h/week × 30/7). */
    val monthlyHours: Double = 225.0,
    /** Fazla mesai: worked hours beyond [monthlyHours] are paid with this multiplier (İş K. md. 41: %50). */
    val overtimeMultiplier: Double = 1.5,
    /** Bayram/genel tatil çalışması: normal wage + one extra day's wage (İş K. md. 47). */
    val holidayMultiplier: Double = 2.0,
    /** Planned length of a shift; the live shift progress bar fills up over this many hours. */
    val shiftHours: Double = 8.0,
    /** When true the user enters [customHourlyRate] directly instead of a monthly salary. */
    val useHourlyRate: Boolean = false,
    /** Hourly wage in TL, used when [useHourlyRate] is set. */
    val customHourlyRate: Double = 0.0,
) {
    val hourlyRate: Double
        get() = when {
            useHourlyRate -> customHourlyRate
            monthlyHours > 0 -> monthlySalary / monthlyHours
            else -> 0.0
        }
}

/**
 * The shift started with the start button and not yet finished. Breaks are tracked in wall-clock
 * millis: [pastBreakMillis] for finished breaks, [breakStartMillis] while a break is running.
 * Pay stops while on a break.
 */
data class ActiveShift(
    val startMillis: Long,
    val breakStartMillis: Long? = null,
    val pastBreakMillis: Long = 0L,
) {
    val onBreak: Boolean get() = breakStartMillis != null

    fun breakMillis(nowMillis: Long): Long =
        pastBreakMillis + (breakStartMillis?.let { (nowMillis - it).coerceAtLeast(0) } ?: 0L)

    /** Paid time so far: elapsed time minus all breaks. */
    fun workedMillis(nowMillis: Long): Long =
        ((nowMillis - startMillis).coerceAtLeast(0) - breakMillis(nowMillis)).coerceAtLeast(0)

    /**
     * The shift as it would be saved at [nowMillis]: worked time in whole minutes, the rest of
     * the span recorded as break. Null when less than a minute has been worked.
     */
    fun toShift(nowMillis: Long, zone: java.time.ZoneId): Shift? {
        val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(startMillis), zone)
        val end = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zone)
        val shift = shiftFromClock(start, end) ?: return null
        val worked = (workedMillis(nowMillis) / 60_000).toInt().coerceAtMost(shift.spanMinutes)
        return if (worked < 1) null else shift.copy(breakMinutes = shift.spanMinutes - worked)
    }
}

/** Predefined shift slots offered in the editor. */
enum class ShiftTemplate(val label: String, val startMinute: Int, val endMinute: Int) {
    MORNING("Sabah", 8 * 60, 16 * 60),
    EVENING("Akşam", 16 * 60, 0),
    NIGHT("Gece", 0, 8 * 60),
    LONG_DAY("12 saat", 8 * 60, 20 * 60),
}

/** Builds the shift recorded by the start/finish button, capped at 24 hours. */
fun shiftFromClock(start: LocalDateTime, end: LocalDateTime): Shift? {
    val span = java.time.Duration.between(start, end).toMinutes().coerceAtMost(Shift.MINUTES_PER_DAY.toLong()).toInt()
    if (span < 1) return null
    val startMinute = start.hour * 60 + start.minute
    return Shift(
        id = 0L,
        date = start.toLocalDate().toString(),
        startMinute = startMinute,
        endMinute = (startMinute + span) % Shift.MINUTES_PER_DAY,
    )
}
