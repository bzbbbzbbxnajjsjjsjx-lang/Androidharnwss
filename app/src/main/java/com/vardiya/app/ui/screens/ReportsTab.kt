package com.vardiya.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vardiya.app.domain.PeriodReport
import com.vardiya.app.domain.Reports
import com.vardiya.app.ui.components.TurkishLocale
import com.vardiya.app.ui.components.formatDuration
import com.vardiya.app.ui.components.formatMoney
import com.vardiya.app.ui.components.formatMonth
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private enum class ReportPeriod(val label: String) { WEEKLY("Haftalık"), MONTHLY("Aylık") }

private val WeekDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", TurkishLocale)
private val WeekEndFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", TurkishLocale)

/** Weekly (Monday–Sunday) and monthly totals of hours, breaks and pay, newest first. */
@Composable
internal fun ReportsTab(
    reports: Reports,
    today: LocalDate,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    var period by rememberSaveable { mutableStateOf(ReportPeriod.WEEKLY) }
    val list = if (period == ReportPeriod.WEEKLY) reports.weekly else reports.monthly

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ReportPeriod.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = period == item,
                        onClick = { period = item },
                        shape = SegmentedButtonDefaults.itemShape(index, ReportPeriod.entries.size),
                    ) { Text(item.label) }
                }
            }
        }
        if (list.isEmpty()) {
            item {
                Text(
                    "Henüz kayıtlı vardiya yok.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(list, key = { "${period.name}-${it.start}" }) { report ->
            val current = !today.isBefore(report.start) && !today.isAfter(report.end)
            ReportCard(
                title = when (period) {
                    ReportPeriod.WEEKLY -> "${report.start.format(WeekDayFormatter)} – ${report.end.format(WeekEndFormatter)}"
                    ReportPeriod.MONTHLY -> formatMonth(YearMonth.from(report.start))
                },
                badge = when {
                    !current -> null
                    period == ReportPeriod.WEEKLY -> "Bu hafta"
                    else -> "Bu ay"
                },
                report = report,
                highlighted = current,
            )
        }
    }
}

@Composable
private fun ReportCard(title: String, badge: String?, report: PeriodReport, highlighted: Boolean) {
    val content: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    if (badge != null) {
                        Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
                Text(formatMoney(report.pay), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            ReportRow("Çalışılan süre", formatDuration(report.workedMinutes))
            ReportRow("Vardiya / gün", "${report.shiftCount} vardiya · ${report.dayCount} gün")
            if (report.dayCount > 0) ReportRow("Günlük ortalama", formatDuration(report.workedMinutes / report.dayCount))
            if (report.breakMinutes > 0) ReportRow("Mola", formatDuration(report.breakMinutes))
            if (report.overtimeMinutes > 0) ReportRow("Fazla mesai", formatDuration(report.overtimeMinutes))
            if (report.holidayMinutes > 0) ReportRow("Bayram / tatil", formatDuration(report.holidayMinutes))
        }
    }
    if (highlighted) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) { content() }
    } else {
        ElevatedCard(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
