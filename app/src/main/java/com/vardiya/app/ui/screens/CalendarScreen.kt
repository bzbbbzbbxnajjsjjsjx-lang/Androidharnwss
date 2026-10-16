package com.vardiya.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vardiya.app.domain.Holiday
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.Shift
import com.vardiya.app.domain.TurkishHolidays
import com.vardiya.app.ui.components.DayMonthFormatter
import com.vardiya.app.ui.components.MonthSwitcher
import com.vardiya.app.ui.components.ShortDateFormatter
import com.vardiya.app.ui.components.formatClock
import com.vardiya.app.ui.components.formatDuration
import com.vardiya.app.ui.components.formatMoney
import java.time.DayOfWeek
import java.time.LocalDate

private val WeekDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

@Composable
fun CalendarScreen(
    summary: MonthSummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onAddShift: (LocalDate) -> Unit,
    onEditShift: (Shift) -> Unit,
    contentPadding: PaddingValues,
    today: LocalDate = LocalDate.now(),
) {
    val month = summary.month
    val holidays = remember(month) { TurkishHolidays.inMonth(month).associateBy { it.date } }
    val shiftsByDate = remember(summary) { summary.shifts.groupBy { it.shift.localDate } }
    var selectedDay by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonthSwitcher(month, onPreviousMonth, onNextMonth, onToday)

        Row(Modifier.fillMaxWidth()) {
            WeekDays.forEachIndexed { index, day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index >= 5) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val firstDay = month.atDay(1)
        val leadingBlanks = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
        val cells = leadingBlanks + month.lengthOfMonth()
        val rows = (cells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 7) {
                        val dayNumber = row * 7 + col - leadingBlanks + 1
                        Box(Modifier.weight(1f)) {
                            if (dayNumber in 1..month.lengthOfMonth()) {
                                val date = month.atDay(dayNumber)
                                DayCell(
                                    date = date,
                                    holiday = holidays[date],
                                    pay = shiftsByDate[date]?.sumOf { it.pay },
                                    isToday = date == today,
                                    onClick = { selectedDay = date.toString() },
                                )
                            }
                        }
                    }
                }
            }
        }

        Legend()

        if (holidays.isNotEmpty()) {
            Text(
                "Bu ayın resmi tatilleri",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            holidays.values.forEach { holiday ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(holiday.name) },
                        supportingContent = {
                            Text(holiday.date.format(ShortDateFormatter) + if (holiday.halfDay) " · yarım gün (13:00)" else "")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }

    selectedDay?.let { iso ->
        val date = LocalDate.parse(iso)
        DaySheet(
            date = date,
            holiday = TurkishHolidays.on(date),
            shifts = shiftsByDate[date].orEmpty().map { it.shift to it.pay },
            onDismiss = { selectedDay = null },
            onAdd = { selectedDay = null; onAddShift(date) },
            onEdit = { selectedDay = null; onEditShift(it) },
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    holiday: Holiday?,
    pay: Double?,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container = when {
        pay != null -> colors.primaryContainer
        holiday != null -> colors.tertiaryContainer
        else -> colors.surfaceContainerLow
    }
    val content = when {
        pay != null -> colors.onPrimaryContainer
        holiday != null -> colors.onTertiaryContainer
        else -> colors.onSurface
    }
    // TalkBack reads the whole day instead of just the number.
    val description = buildString {
        append(date.format(DayMonthFormatter))
        if (holiday != null) append(", ${holiday.name}" + if (holiday.halfDay) " (yarım gün)" else "")
        if (pay != null) append(", vardiya kazancı ${formatMoney(pay)}")
        if (isToday) append(", bugün")
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border = if (isToday) BorderStroke(2.dp, colors.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (holiday != null) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(if (holiday.halfDay) colors.tertiary.copy(alpha = 0.5f) else colors.tertiary, CircleShape),
                )
            }
            if (pay != null) {
                Text(
                    compactMoney(pay),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            } else {
                Spacer(Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun Legend() {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(colors.primaryContainer, "Vardiya")
        LegendItem(colors.tertiaryContainer, "Resmi tatil")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(14.dp)
                    .border(2.dp, colors.primary, MaterialTheme.shapes.extraSmall),
            )
            Spacer(Modifier.width(6.dp))
            Text("Bugün", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .background(color, MaterialTheme.shapes.extraSmall),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DaySheet(
    date: LocalDate,
    holiday: Holiday?,
    shifts: List<Pair<Shift, Double>>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Shift) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                date.format(DayMonthFormatter),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            if (holiday != null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        holiday.name + if (holiday.halfDay) " · 13:00'ten itibaren tatil" else " · tam gün tatil",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (shifts.isEmpty()) {
                Text("Bu gün için vardiya yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            shifts.forEach { (shift, pay) ->
                OutlinedCard(onClick = { onEdit(shift) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("${formatClock(shift.startMinute)} – ${formatClock(shift.endMinute)}") },
                        supportingContent = { Text(formatDuration(shift.workedMinutes) + if (shift.note.isNotBlank()) " · ${shift.note}" else "") },
                        trailingContent = { Text(formatMoney(pay), style = MaterialTheme.typography.titleMedium) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Vardiya ekle")
            }
        }
    }
}

/** 1244.44 → "₺1,2b" style short label that fits a calendar cell. */
private fun compactMoney(amount: Double): String =
    if (amount >= 1000) "₺" + "%.1f".format(amount / 1000).replace('.', ',') + "b" else "₺" + amount.toInt()
