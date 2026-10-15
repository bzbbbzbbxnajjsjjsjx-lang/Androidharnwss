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

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
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
) {
    val hourlyRate: Double get() = if (monthlyHours > 0) monthlySalary / monthlyHours else 0.0
}

/** Predefined shift slots offered in the editor. */
enum class ShiftTemplate(val label: String, val startMinute: Int, val endMinute: Int) {
    MORNING("Sabah", 8 * 60, 16 * 60),
    EVENING("Akşam", 16 * 60, 0),
    NIGHT("Gece", 0, 8 * 60),
    LONG_DAY("12 saat", 8 * 60, 20 * 60),
}
