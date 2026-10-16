package com.vardiya.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.ui.components.formatMoney

@Composable
fun SettingsScreen(
    settings: PaySettings,
    onSave: (PaySettings) -> Unit,
    contentPadding: PaddingValues,
) {
    var useHourly by rememberSaveable(settings) { mutableStateOf(settings.useHourlyRate) }
    var hourlyRate by rememberSaveable(settings) { mutableStateOf(settings.customHourlyRate.formatHours()) }
    var salary by rememberSaveable(settings) { mutableStateOf(settings.monthlySalary.formatHours()) }
    var hours by rememberSaveable(settings) { mutableStateOf(settings.monthlyHours.formatHours()) }
    var overtime by rememberSaveable(settings) { mutableStateOf(settings.overtimeMultiplier.formatHours()) }
    var holiday by rememberSaveable(settings) { mutableStateOf(settings.holidayMultiplier.formatHours()) }
    var shiftHours by rememberSaveable(settings) { mutableStateOf(settings.shiftHours.formatHours()) }

    val context = LocalContext.current
    val parsed = PaySettings(
        monthlySalary = salary.toDecimal() ?: settings.monthlySalary,
        monthlyHours = hours.toDecimal()?.takeIf { it > 0 } ?: settings.monthlyHours,
        overtimeMultiplier = overtime.toDecimal() ?: settings.overtimeMultiplier,
        holidayMultiplier = holiday.toDecimal() ?: settings.holidayMultiplier,
        shiftHours = shiftHours.toDecimal()?.takeIf { it > 0 } ?: settings.shiftHours,
        useHourlyRate = useHourly,
        customHourlyRate = hourlyRate.toDecimal() ?: settings.customHourlyRate,
    )

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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Ücret ayarları",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Saatlik ücretimi kendim gireceğim", modifier = Modifier.weight(1f))
            Switch(checked = useHourly, onCheckedChange = { useHourly = it })
        }
        if (useHourly) {
            NumberField("Saatlik ücret (₺)", hourlyRate) { hourlyRate = it }
        } else {
            NumberField("Aylık maaş (₺)", salary) { salary = it }
        }
        NumberField("Aylık çalışma saati", hours) { hours = it }
        NumberField("Fazla mesai çarpanı", overtime) { overtime = it }
        NumberField("Bayram / tatil çarpanı", holiday) { holiday = it }
        NumberField("Vardiya süresi (saat)", shiftHours) { shiftHours = it }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Saatlik ücretin", style = MaterialTheme.typography.labelLarge)
                Text(formatMoney(parsed.hourlyRate), style = MaterialTheme.typography.headlineMedium)
                Text(
                    (if (parsed.useHourlyRate) "Kendi girdiğin saatlik ücret. " else
                        "${formatMoney(parsed.monthlySalary)} ÷ ${parsed.monthlyHours.formatHours()} saat. " +
                        "225 saat, İş Kanunu'ndaki haftalık 45 saatin aylık karşılığıdır. ") +
                        "Fazla mesai (225 saati aşan) ×${parsed.overtimeMultiplier.formatHours()}, " +
                        "bayram ve genel tatil çalışması ×${parsed.holidayMultiplier.formatHours()} ödenir.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Button(
            onClick = {
                onSave(parsed)
                Toast.makeText(context, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()) {
            Text("Kaydet")
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() || it == ',' || it == '.' }.take(10)) },
        label = { Text(label) },
        isError = value.toDecimal() == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Accepts both "28000,50" and "28.000" style input. */
private fun String.toDecimal(): Double? {
    val normalized = if (contains(',')) replace(".", "").replace(',', '.') else this
    return normalized.toDoubleOrNull()?.takeIf { it >= 0 }
}
