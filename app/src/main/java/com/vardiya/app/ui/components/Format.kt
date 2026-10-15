package com.vardiya.app.ui.components

import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val TurkishLocale: Locale = Locale.forLanguageTag("tr-TR")

private val currency: NumberFormat = NumberFormat.getCurrencyInstance(TurkishLocale).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}

fun formatMoney(amount: Double): String = currency.format(amount)

/** 450 → "7 sa 30 dk". */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m dk"
        m == 0 -> "$h sa"
        else -> "$h sa $m dk"
    }
}

/** 480 → "08:00". */
fun formatClock(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60 % 24, minuteOfDay % 60)

fun formatMonth(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL_STANDALONE, TurkishLocale).replaceFirstChar { it.titlecase(TurkishLocale) } +
        " ${month.year}"

val DayMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", TurkishLocale)
val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM EEE", TurkishLocale)
