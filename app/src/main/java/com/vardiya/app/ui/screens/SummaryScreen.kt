package com.vardiya.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vardiya.app.domain.ActiveShift
import com.vardiya.app.domain.EarningsCalculator
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.ui.components.PauseIcon
import com.vardiya.app.ui.components.TurkishLocale
import com.vardiya.app.ui.components.WavyRing
import com.vardiya.app.ui.components.formatMoney
import com.vardiya.app.ui.components.formatMonth
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One big wavy ring that fills in proportion to the elapsed share of the planned shift length,
 * with the timer, shift progress and this shift's earnings inside. Start and finish buttons sit at the bottom,
 * with a break button above them while a shift runs; the timer and pay stand still during a break.
 */
@Composable
fun SummaryScreen(
    summary: MonthSummary,
    partialPay: Double,
    activeShift: ActiveShift?,
    onStartShift: () -> Unit,
    onToggleBreak: () -> Unit,
    onFinishShift: () -> Unit,
    contentPadding: PaddingValues,
    clock: () -> Long = System::currentTimeMillis,
) {
    val settings = summary.settings
    val running = activeShift != null
    val onBreak = activeShift?.onBreak == true
    val haptic = LocalHapticFeedback.current
    val currentClock by rememberUpdatedState(clock)

    val now by produceState(clock(), activeShift) {
        while (activeShift != null) {
            value = currentClock()
            delay(1_000)
        }
    }
    // Worked time excludes breaks, so the timer and pay stand still while on a break.
    val elapsedSeconds = activeShift?.workedMillis(now)?.div(1000) ?: 0L
    val breakSeconds = activeShift?.breakMillis(now)?.div(1000) ?: 0L
    val shiftEarned = if (activeShift != null) {
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(activeShift.startMillis), zone)
        val end = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
        val holidaySeconds = (EarningsCalculator.holidayOverlapMinutes(start, end) * 60L).coerceAtMost(elapsedSeconds)
        settings.hourlyRate / 3600.0 *
            ((elapsedSeconds - holidaySeconds) + holidaySeconds * settings.holidayMultiplier)
    } else {
        0.0
    }

    // The ring fills with the elapsed share of the planned shift, updated every frame so it moves
    // smoothly; it stays full once the shift runs past its planned length.
    val shiftMillis = (settings.shiftHours * 3_600_000).coerceAtLeast(1.0)
    val shiftProgress by produceState(0f, activeShift, shiftMillis) {
        if (activeShift == null) {
            value = 0f
        } else {
            while (true) {
                withFrameMillis {
                    value = (activeShift.workedMillis(currentClock()) / shiftMillis).toFloat().coerceIn(0f, 1f)
                }
            }
        }
    }
    // A light tick every full minute.
    val minutesElapsed = (elapsedSeconds / 60).toInt()
    var lastMinute by remember { mutableIntStateOf(minutesElapsed) }
    LaunchedEffect(minutesElapsed, running) {
        if (running && minutesElapsed > lastMinute) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        lastMinute = minutesElapsed
    }
    val shiftPercent = elapsedSeconds / (settings.shiftHours * 3600).coerceAtLeast(1.0) * 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                WavyRing(
                    progress = { shiftProgress },
                    size = 300.dp,
                )
                // One TalkBack stop that reads status, timer and earnings together.
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        when {
                            onBreak -> "Molada · ücret durdu"
                            running -> "Vardiya sürüyor · %${"%.1f".format(TurkishLocale, shiftPercent)}"
                            else -> "Vardiya başlamadı"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (onBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatSeconds(elapsedSeconds),
                        style = MaterialTheme.typography.displaySmallEmphasized,
                    )
                    Text(
                        formatMoney(shiftEarned),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (breakSeconds > 0) {
                        Text(
                            "Mola ${formatSeconds(breakSeconds)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${formatMonth(summary.month)} kazancın",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatMoney(summary.totalPay + partialPay),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                )
            }
        }

        // Medium (56dp) buttons: pill at rest, the corners squeeze while pressed.
        val buttonHeight = ButtonDefaults.MediumContainerHeight
        if (running) {
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onToggleBreak()
                },
                shapes = ButtonDefaults.shapesFor(buttonHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight, hasStartIcon = true),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
            ) {
                if (onBreak) ButtonLabel(Icons.Filled.PlayArrow, "Moladan dön") else ButtonLabel(PauseIcon, "Mola ver")
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onStartShift()
                },
                shapes = ButtonDefaults.shapesFor(buttonHeight),
                enabled = !running,
                contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight, hasStartIcon = true),
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
            ) {
                ButtonLabel(Icons.Filled.PlayArrow, "Başlat")
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFinishShift()
                },
                shapes = ButtonDefaults.shapesFor(buttonHeight),
                enabled = running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight, hasStartIcon = true),
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
            ) {
                ButtonLabel(Icons.Filled.Check, "Bitir")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L

/** 3725 → "01:02:05". */
private fun formatSeconds(seconds: Long): String =
    "%02d:%02d:%02d".format(
        seconds / SECONDS_PER_HOUR,
        seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
        seconds % SECONDS_PER_MINUTE,
    )

/** Icon and text sized for a Medium button. */
@Composable
private fun RowScope.ButtonLabel(icon: ImageVector, text: String) {
    val height = ButtonDefaults.MediumContainerHeight
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
    Text(text, style = ButtonDefaults.textStyleFor(height))
}

/** 225.0 → "225", 1.5 → "1,5". */
internal fun Double.formatHours(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')
