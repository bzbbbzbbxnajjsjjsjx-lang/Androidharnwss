package com.vardiya.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vardiya.app.domain.Shift

/** Asks before a shift is deleted, naming its date and hours. */
@Composable
fun DeleteShiftDialog(shift: Shift, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = { Text("Vardiya silinsin mi?") },
        text = {
            Text(
                "${shift.localDate.format(DayMonthFormatter)}, " +
                    "${formatClock(shift.startMinute)} – ${formatClock(shift.endMinute)} vardiyası kalıcı olarak silinecek.",
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text("Sil", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

/** Trailing pay with a delete button, used by shift rows. The running shift cannot be deleted. */
@Composable
fun ShiftRowTrailing(shift: Shift, pay: Double, onDelete: (Shift) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(formatMoney(pay), style = MaterialTheme.typography.titleMedium)
        if (!shift.isLive) {
            IconButton(onClick = { onDelete(shift) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Vardiyayı sil")
            }
        }
    }
}
