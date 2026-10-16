package com.vardiya.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vardiya.app.domain.EarningsCalculator
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.ui.components.TurkishLocale
import com.vardiya.app.ui.components.WavyRing
import com.vardiya.app.ui.components.formatMoney
import com.vardiya.app.ui.components.formatMonth
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One big wavy ring that flows and fills one lap per minute while a shift runs, with the timer,
 * shift progress and this shift's earnings inside. Start and finish buttons sit at the bottom.
 */
@Composable
fun SummaryScreen(
    summary: MonthSummary,
    activeShiftStart: Long?,
    onStartShift: () -> Unit,
    onFinishShift: () -> Unit,
    contentPadding: PaddingValues,
) {
    val settings = summary.settings
    val running = activeShiftStart != null
    val haptic = LocalHapticFeedback.current

    val now by produceState(System.currentTimeMillis(), activeShiftStart) {
        while (activeShiftStart != null) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedSeconds = if (activeShiftStart != null) ((now - activeShiftStart) / 1000).coerceAtLeast(0) else 0L
    val shiftEarned = if (activeShiftStart != null) {
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(activeShiftStart), zone)
        val end = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
        val holidaySeconds = (EarningsCalculator.holidayOverlapMinutes(start, end) * 60L).coerceAtMost(elapsedSeconds)
        settings.hourlyRate / 3600.0 *
            ((elapsedSeconds - holidaySeconds) + holidaySeconds * settings.holidayMultiplier)
    } else {
        0.0
    }

    // The ring completes one lap per minute so the movement is clearly visible.
    val minuteProgress by produceState(0f, activeShiftStart) {
        if (activeShiftStart == null) {
            value = 0f
        } else {
            while (true) {
                withFrameMillis {
                    value = ((System.currentTimeMillis() - activeShiftStart).coerceAtLeast(0) % 60_000L) / 60_000f
                }
            }
        }
    }
    // The wave grows in when a shift starts and flattens when it ends.
    val waveAmplitude by animateFloatAsState(if (running) 1f else 0f, tween(durationMillis = 500), label = "waveAmplitude")

    // A light tick every full minute (one lap of the ring).
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
                    progress = { minuteProgress },
                    amplitude = { waveAmplitude },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(300.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (running) "Vardiya sürüyor · %${"%.1f".format(TurkishLocale, shiftPercent)}" else "Vardiya başlamadı",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "%02d:%02d:%02d".format(elapsedSeconds / 3600, elapsedSeconds / 60 % 60, elapsedSeconds % 60),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        formatMoney(shiftEarned),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "${formatMonth(summary.month)} kazancın",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatMoney(summary.totalPay),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpressiveButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onStartShift()
                },
                enabled = !running,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Başlat", style = MaterialTheme.typography.titleMedium)
            }
            ExpressiveButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFinishShift()
                },
                enabled = running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Bitir", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Pill button whose corners morph to a squarer shape while pressed (M3 Expressive). */
@Composable
private fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
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
        enabled = enabled,
        shape = RoundedCornerShape(percent = corner),
        interactionSource = interaction,
        colors = colors,
        content = content,
    )
}

/** 225.0 → "225", 1.5 → "1,5". */
internal fun Double.formatHours(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')
