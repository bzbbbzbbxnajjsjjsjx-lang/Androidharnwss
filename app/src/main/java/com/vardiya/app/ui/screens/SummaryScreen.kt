package com.vardiya.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.vardiya.app.domain.EarningsCalculator
import com.vardiya.app.domain.HolidayKind
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.TurkishHolidays
import com.vardiya.app.ui.components.MonthSwitcher
import com.vardiya.app.ui.components.MorphingShape
import com.vardiya.app.ui.components.VardiyaShapes
import com.vardiya.app.ui.components.formatClock
import com.vardiya.app.ui.components.ShortDateFormatter
import com.vardiya.app.ui.components.TurkishLocale
import com.vardiya.app.ui.components.formatDuration
import com.vardiya.app.ui.components.formatMoney
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    summary: MonthSummary,
    activeShiftStart: Long?,
    onStartShift: () -> Unit,
    onFinishShift: () -> Unit,
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
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ShiftControlCard(activeShiftStart, summary.settings, onStartShift, onFinishShift)
        }
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
    val progress = rememberEntryProgress(summary.salaryProgress.coerceIn(0f, 1f))
    // Counts up from zero on entry, then follows the live total every second.
    val money = remember { Animatable(0f) }
    LaunchedEffect(summary.totalPay) {
        money.animateTo(summary.totalPay.toFloat(), tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
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
                formatMoney(money.value.toDouble()),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            LinearWavyProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxWidth(),
                amplitude = { 1f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "İlerleme %${formatPercent(summary.salaryProgress)}",
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
    val progress = rememberEntryProgress(summary.hoursProgress.coerceIn(0f, 1f))
    // On entry the badge morphs from a circle into a cookie shape while it turns.
    val morph = remember { Morph(VardiyaShapes.Circle, VardiyaShapes.Cookie) }
    val shape = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        shape.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow))
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(104.dp),
                    amplitude = { 1f },
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.tertiaryContainer,
                )
                MorphingShape(
                    morph = morph,
                    progress = shape.value.coerceIn(0f, 1f),
                    rotation = shape.value * 120f,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(68.dp),
                ) {
                    Text(
                        "%${formatPercent(progress.value)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
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

/** Animates from 0 to [target] when the screen opens, then follows later changes of [target]. */
@Composable
private fun rememberEntryProgress(target: Float): Animatable<Float, *> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) {
        progress.animateTo(target, tween(durationMillis = 1_400, easing = FastOutSlowInEasing))
    }
    return progress
}

@Composable
private fun ShiftControlCard(
    activeShiftStart: Long?,
    settings: PaySettings,
    onStart: () -> Unit,
    onFinish: () -> Unit,
) {
    if (activeShiftStart == null) {
        ExpressiveButton(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("Vardiyayı Başlat", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val now by produceState(System.currentTimeMillis(), activeShiftStart) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val zone = ZoneId.systemDefault()
    val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(activeShiftStart), zone)
    val end = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
    val elapsedSeconds = ((now - activeShiftStart) / 1000).coerceAtLeast(0)
    val elapsedMinutes = (elapsedSeconds / 60).toInt()
    val holidayMinutes = EarningsCalculator.holidayOverlapMinutes(start, end).coerceAtMost(elapsedMinutes)
    val earned = settings.hourlyRate / 60.0 *
        ((elapsedMinutes - holidayMinutes) + holidayMinutes * settings.holidayMultiplier)
    val target = (settings.shiftHours * 3600).coerceAtLeast(1.0)
    val shiftProgress = (elapsedSeconds / target).toFloat()
    val entry = rememberEntryProgress(shiftProgress.coerceIn(0f, 1f))
    var confirmFinish by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Vardiya sürüyor · ${formatClock(start.hour * 60 + start.minute)}'de başladı",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "%02d:%02d:%02d".format(elapsedSeconds / 3600, elapsedSeconds / 60 % 60, elapsedSeconds % 60),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LinearWavyProgressIndicator(
                progress = { entry.value },
                modifier = Modifier.fillMaxWidth(),
                amplitude = { 1f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Hedef ${settings.shiftHours.formatHours()} saat · %${(shiftProgress * 100).roundToInt()}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text("≈ ${formatMoney(earned)}", style = MaterialTheme.typography.labelLarge)
            }
            ExpressiveButton(
                onClick = { confirmFinish = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Vardiyayı Bitir", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Vardiyayı bitir") },
            text = { Text("${formatDuration(elapsedMinutes)} çalışma kaydedilecek.") },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; onFinish() }) { Text("Bitir") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Devam et") } },
        )
    }
}

/** Pill button whose corners morph to a squarer shape while pressed (M3 Expressive). */
@Composable
private fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateIntAsState(
        targetValue = if (pressed) 18 else 50,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "buttonCorner",
    )
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(percent = corner),
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(),
        content = content,
    )
}

/** 0.0123 → "1,2". */
private fun formatPercent(fraction: Float): String = "%.1f".format(TurkishLocale, fraction * 100)

/** 225.0 → "225", 1.5 → "1,5". */
internal fun Double.formatHours(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')
