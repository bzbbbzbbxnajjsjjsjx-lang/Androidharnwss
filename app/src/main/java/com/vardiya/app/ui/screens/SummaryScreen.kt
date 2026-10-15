package com.vardiya.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vardiya.app.domain.HolidayKind
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.TurkishHolidays
import com.vardiya.app.ui.components.MonthSwitcher
import com.vardiya.app.ui.components.ShortDateFormatter
import com.vardiya.app.ui.components.formatDuration
import com.vardiya.app.ui.components.formatMoney
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    summary: MonthSummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    contentPadding: PaddingValues,
) {
    val upcoming = remember { TurkishHolidays.upcoming(LocalDate.now(), 4) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            // Leave room for the FAB above the navigation bar.
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            MonthSwitcher(summary.month, onPreviousMonth, onNextMonth, onToday)
        }
        item { EarningsCard(summary) }
        item { HoursCard(summary) }
        item { BreakdownCard(summary) }
        item {
            Text(
                "Yaklaşan resmi tatiller",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(upcoming, key = { it.date.toString() }) { holiday ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(holiday.name) },
                    supportingContent = {
                        Text(holiday.date.format(ShortDateFormatter) + if (holiday.halfDay) " · 13:00'ten itibaren" else "")
                    },
                    overlineContent = {
                        Text(if (holiday.kind == HolidayKind.RELIGIOUS) "Dini bayram" else "Ulusal bayram")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun EarningsCard(summary: MonthSummary) {
    val progress by animateFloatAsState(summary.salaryProgress.coerceIn(0f, 1f), label = "salaryProgress")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("Bu ay kazancın", style = MaterialTheme.typography.titleMedium)
            Text(
                formatMoney(summary.totalPay),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "İlerleme %${(summary.salaryProgress * 100).roundToInt()}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "Hedef ${formatMoney(summary.settings.monthlySalary)}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun HoursCard(summary: MonthSummary) {
    val progress by animateFloatAsState(summary.hoursProgress.coerceIn(0f, 1f), label = "hoursProgress")
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(96.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.tertiaryContainer,
                )
                Text(
                    "%${(summary.hoursProgress * 100).roundToInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Çalışılan süre", style = MaterialTheme.typography.labelLarge)
                Text(formatDuration(summary.workedMinutes), style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Aylık norm ${summary.settings.monthlyHours.formatHours()} saat · ${summary.shifts.size} vardiya",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Saatlik ücret ${formatMoney(summary.settings.hourlyRate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BreakdownCard(summary: MonthSummary) {
    val s = summary.settings
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(vertical = 8.dp)) {
            BreakdownRow("Normal mesai", summary.regularMinutes, summary.regularPay, "×1")
            BreakdownRow("Fazla mesai", summary.overtimeMinutes, summary.overtimePay, "×${s.overtimeMultiplier.formatHours()}")
            BreakdownRow("Bayram / tatil", summary.holidayMinutes, summary.holidayPay, "×${s.holidayMultiplier.formatHours()}")
        }
    }
}

@Composable
private fun BreakdownRow(title: String, minutes: Int, pay: Double, multiplier: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text("${formatDuration(minutes)} · $multiplier") },
        trailingContent = { Text(formatMoney(pay), style = MaterialTheme.typography.titleMedium) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/** 225.0 → "225", 1.5 → "1,5". */
internal fun Double.formatHours(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')
