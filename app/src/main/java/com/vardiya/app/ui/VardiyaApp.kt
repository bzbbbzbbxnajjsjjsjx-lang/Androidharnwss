package com.vardiya.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vardiya.app.domain.ActiveShift
import com.vardiya.app.domain.MonthSummary
import com.vardiya.app.domain.Reports
import com.vardiya.app.domain.Shift
import com.vardiya.app.ui.components.DeleteShiftDialog
import com.vardiya.app.ui.screens.CalendarScreen
import com.vardiya.app.ui.screens.EditorRequest
import com.vardiya.app.ui.screens.SettingsScreen
import com.vardiya.app.ui.screens.ShiftEditorSheet
import com.vardiya.app.ui.screens.ShiftsScreen
import com.vardiya.app.ui.screens.SummaryScreen
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

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

/** Collects the reports only while the calendar tab is shown. */
@Composable
private fun CalendarRoute(
    reportsFlow: StateFlow<Reports>,
    summary: MonthSummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onAddShift: (LocalDate) -> Unit,
    onEditShift: (Shift) -> Unit,
    onDeleteShift: (Shift) -> Unit,
    contentPadding: PaddingValues,
) {
    val reports by reportsFlow.collectAsStateWithLifecycle()
    CalendarScreen(
        summary = summary,
        reports = reports,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onToday = onToday,
        onAddShift = onAddShift,
        onEditShift = onEditShift,
        onDeleteShift = onDeleteShift,
        contentPadding = contentPadding,
    )
}

/** Collects the per-second pay only while the summary tab is shown. */
@Composable
private fun SummaryRoute(
    viewModel: VardiyaViewModel,
    summary: MonthSummary,
    activeShift: ActiveShift?,
    contentPadding: PaddingValues,
) {
    val partialPay by viewModel.partialPay.collectAsStateWithLifecycle()
    SummaryScreen(
        summary = summary,
        partialPay = partialPay,
        activeShift = activeShift,
        onStartShift = viewModel::startShift,
        onToggleBreak = viewModel::toggleBreak,
        onFinishShift = viewModel::finishShift,
        contentPadding = contentPadding,
    )
}

@Composable
private fun MainContent(viewModel: VardiyaViewModel) {
    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.SUMMARY) }
    var editor by remember { mutableStateOf<EditorRequest?>(null) }
    var pendingDelete by remember { mutableStateOf<Shift?>(null) }

    Scaffold(
        bottomBar = {
            VardiyaNavigationBar(destination = destination, onSelect = { destination = it })
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
            Destination.SUMMARY -> SummaryRoute(
                viewModel = viewModel,
                summary = summary,
                activeShift = activeShift,
                contentPadding = padding,
            )
            Destination.CALENDAR -> CalendarRoute(
                reportsFlow = viewModel.reports,
                summary = summary,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onToday = viewModel::currentMonth,
                onAddShift = { editor = EditorRequest(it) },
                onEditShift = { if (!it.isLive) editor = EditorRequest(it.localDate, it) },
                onDeleteShift = { pendingDelete = it },
                contentPadding = padding,
            )
            Destination.SHIFTS -> ShiftsScreen(
                summary = summary,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onToday = viewModel::currentMonth,
                onEditShift = { if (!it.isLive) editor = EditorRequest(it.localDate, it) },
                onDeleteShift = { pendingDelete = it },
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
                onDelete = { pendingDelete = it },
            )
        }
    }

    pendingDelete?.let { shift ->
        DeleteShiftDialog(
            shift = shift,
            onConfirm = { viewModel.deleteShift(shift.id) },
            onDismiss = { pendingDelete = null },
        )
    }
}
