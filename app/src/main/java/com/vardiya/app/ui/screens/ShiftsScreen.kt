package com.vardiya.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.Shift
import com.vardiya.app.ui.components.MonthSwitcher
import com.vardiya.app.ui.components.ShortDateFormatter
import com.vardiya.app.ui.components.formatClock
import com.vardiya.app.ui.components.formatDuration
import com.vardiya.app.ui.components.formatMoney

@Composable
fun ShiftsScreen(
    summary: MonthSummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onEditShift: (Shift) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            // Leave room for the FAB above the navigation bar.
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { MonthSwitcher(summary.month, onPreviousMonth, onNextMonth, onToday) }
        if (summary.shifts.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Bu ay henüz vardiya yok.\nSağ alttaki + ile ekleyebilirsin.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(summary.shifts, key = { it.shift.id }) { pay ->
            val shift = pay.shift
            val tags = buildList {
                if (shift.isLive) add("Devam ediyor")
                if (pay.holidayMinutes > 0) add("Bayram ${formatDuration(pay.holidayMinutes)}")
                if (pay.overtimeMinutes > 0) add("Fazla mesai ${formatDuration(pay.overtimeMinutes)}")
                if (shift.breakMinutes > 0) add("Mola ${shift.breakMinutes} dk")
                if (shift.note.isNotBlank()) add(shift.note)
            }
            ElevatedCard(onClick = { onEditShift(shift) }, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    overlineContent = { Text(shift.localDate.format(ShortDateFormatter)) },
                    headlineContent = {
                        Text("${formatClock(shift.startMinute)} – ${formatClock(shift.endMinute)} · ${formatDuration(shift.workedMinutes)}")
                    },
                    supportingContent = if (tags.isEmpty()) null else ({ Text(tags.joinToString(" · ")) }),
                    trailingContent = { Text(formatMoney(pay.pay), style = MaterialTheme.typography.titleMedium) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
