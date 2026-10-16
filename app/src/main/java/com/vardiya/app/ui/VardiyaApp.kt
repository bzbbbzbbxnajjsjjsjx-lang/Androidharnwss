package com.vardiya.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vardiya.app.ui.screens.CalendarScreen
import com.vardiya.app.ui.screens.EditorRequest
import com.vardiya.app.ui.screens.SettingsScreen
import com.vardiya.app.ui.screens.ShiftEditorSheet
import com.vardiya.app.ui.screens.ShiftsScreen
import com.vardiya.app.ui.screens.SummaryScreen
import java.time.LocalDate

private enum class Destination(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    SUMMARY("Özet", Icons.Filled.Home, Icons.Outlined.Home),
    CALENDAR("Takvim", Icons.Filled.DateRange, Icons.Outlined.DateRange),
    SHIFTS("Vardiyalar", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Filled.List),
    SETTINGS("Ayarlar", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun VardiyaApp(viewModel: VardiyaViewModel = viewModel()) {
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    AnimatedContent(
        targetState = ready,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "opening",
    ) { isReady ->
        if (isReady) MainContent(viewModel) else OpeningScreen()
    }
}

/** Opening screen: the M3 Expressive loading indicator morphs through its shapes. */
@Composable
private fun OpeningScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LoadingIndicator(modifier = Modifier.size(96.dp))
                Text("Vardiya", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun MainContent(viewModel: VardiyaViewModel) {
    val activeShiftStart by viewModel.activeShiftStart.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.SUMMARY) }
    var editor by remember { mutableStateOf<EditorRequest?>(null) }

    Scaffold(
        bottomBar = {
            // Full-width, tall (80dp) Material 3 navigation bar with always-visible labels.
            NavigationBar {
                Destination.entries.forEach { item ->
                    val selected = destination == item
                    NavigationBarItem(
                        selected = selected,
                        onClick = { destination = item },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = destination == Destination.SHIFTS,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { editor = EditorRequest(LocalDate.now()) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Vardiya") },
                )
            }
        },
    ) { padding ->
        when (destination) {
            Destination.SUMMARY -> SummaryScreen(
                summary = summary,
                activeShiftStart = activeShiftStart,
                onStartShift = viewModel::startShift,
                onFinishShift = viewModel::finishShift,
                contentPadding = padding,
            )
            Destination.CALENDAR -> CalendarScreen(
                summary = summary,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onToday = viewModel::currentMonth,
                onAddShift = { editor = EditorRequest(it) },
                onEditShift = { if (!it.isLive) editor = EditorRequest(it.localDate, it) },
                contentPadding = padding,
            )
            Destination.SHIFTS -> ShiftsScreen(
                summary = summary,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onToday = viewModel::currentMonth,
                onEditShift = { if (!it.isLive) editor = EditorRequest(it.localDate, it) },
                contentPadding = padding,
            )
            Destination.SETTINGS -> SettingsScreen(
                settings = settings,
                onSave = viewModel::saveSettings,
                contentPadding = padding,
            )
        }
    }

    editor?.let { request ->
        key(request) {
            ShiftEditorSheet(
                request = request,
                onDismiss = { editor = null },
                onSave = { viewModel.saveShift(it) },
                onDelete = { viewModel.deleteShift(it) },
            )
        }
    }
}
