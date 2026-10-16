package com.vardiya.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vardiya.app.domain.Shift
import com.vardiya.app.domain.ShiftTemplate
import com.vardiya.app.ui.components.DayMonthFormatter
import com.vardiya.app.ui.components.formatClock
import com.vardiya.app.ui.components.formatDuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Opens the editor for a new shift on [date], or for editing [shift] when it is set. */
data class EditorRequest(val date: LocalDate, val shift: Shift? = null)

@Composable
fun ShiftEditorSheet(
    request: EditorRequest,
    onDismiss: () -> Unit,
    onSave: (Shift) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val existing = request.shift
    var date by rememberSaveable { mutableStateOf((existing?.localDate ?: request.date).toString()) }
    var start by rememberSaveable { mutableIntStateOf(existing?.startMinute ?: ShiftTemplate.MORNING.startMinute) }
    var end by rememberSaveable { mutableIntStateOf(existing?.endMinute ?: ShiftTemplate.MORNING.endMinute) }
    var breakText by rememberSaveable { mutableStateOf(existing?.breakMinutes?.takeIf { it > 0 }?.toString().orEmpty()) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var editingStart by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val breakMinutes = breakText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val draft = Shift(existing?.id ?: 0L, date, start, end, breakMinutes, note.trim())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (existing == null) "Vardiya ekle" else "Vardiyayı düzenle",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(LocalDate.parse(date).format(DayMonthFormatter))
            }

            Text("Hazır vardiyalar", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShiftTemplate.entries.forEach { template ->
                    FilterChip(
                        selected = start == template.startMinute && end == template.endMinute,
                        onClick = { start = template.startMinute; end = template.endMinute },
                        label = { Text("${template.label} ${formatClock(template.startMinute)}–${formatClock(template.endMinute)}") },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TimeButton("Başlangıç", start, Modifier.weight(1f)) { editingStart = true }
                TimeButton("Bitiş", end, Modifier.weight(1f)) { editingStart = false }
            }
            Text(
                "Süre: ${formatDuration(draft.workedMinutes)}" +
                    if (draft.end.toLocalDate() != draft.localDate) " · ertesi gün biter" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = breakText,
                onValueChange = { text -> breakText = text.filter(Char::isDigit).take(3) },
                label = { Text("Ücretsiz mola (dakika)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(60) },
                label = { Text("Not (isteğe bağlı)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existing != null) {
                    OutlinedButton(onClick = { onDelete(existing.id); onDismiss() }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sil")
                    }
                }
                Button(
                    onClick = { onSave(draft); onDismiss() },
                    enabled = draft.workedMinutes > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Kaydet")
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("İptal") } },
        ) {
            DatePicker(state = state)
        }
    }

    editingStart?.let { isStart ->
        val initial = if (isStart) start else end
        val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { editingStart = null },
            title = { Text(if (isStart) "Başlangıç saati" else "Bitiş saati") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = state.hour * 60 + state.minute
                    if (isStart) start = minutes else end = minutes
                    editingStart = null
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { editingStart = null }) { Text("İptal") } },
        )
    }
}

@Composable
private fun TimeButton(label: String, minuteOfDay: Int, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(formatClock(minuteOfDay), style = MaterialTheme.typography.titleLarge)
        }
    }
}
