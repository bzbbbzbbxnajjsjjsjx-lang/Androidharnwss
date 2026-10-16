package com.vardiya.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vardiya.app.domain.EarningsCalculator
import com.vardiya.app.domain.PaySettings
import com.vardiya.app.domain.Shift
import com.vardiya.app.ui.screens.CalendarScreen
import com.vardiya.app.ui.screens.SettingsScreen
import com.vardiya.app.ui.screens.ShiftsScreen
import com.vardiya.app.ui.screens.SummaryScreen
import com.vardiya.app.ui.theme.VardiyaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Renders every tab, light and dark, into `app/src/test/screenshots/`.
 * Record with `./gradlew recordRoborazziDebug`, check with `./gradlew verifyRoborazziDebug`.
 * Time is fixed so the images don't change from run to run.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// SDK 34: newer Robolectric images need JDK 21 and extra --add-opens; CI runs JDK 17.
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 16)
    private val month = YearMonth.of(2026, 10)
    private val settings = PaySettings()
    private val summary = EarningsCalculator.summarize(
        month = month,
        allShifts = listOf(1, 2, 5, 6, 7, 8, 9, 12, 13, 14, 15).mapIndexed { i, day ->
            val evening = day % 7 == 0
            Shift(
                id = i + 1L,
                date = month.atDay(day).toString(),
                startMinute = if (evening) 16 * 60 else 8 * 60,
                endMinute = if (evening) 0 else 16 * 60,
                breakMinutes = 30,
            )
        },
        settings = settings,
    )

    /** 16 Ekim 2026 13:47:12; a shift running since 08:00. */
    private val now = LocalDateTime.of(2026, 10, 16, 13, 47, 12).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val shiftStart = LocalDateTime.of(2026, 10, 16, 8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun summaryIdle() = capture("summary_idle", Destination.SUMMARY) { SummaryContent(it, running = false) }

    @Test fun summaryJustStarted() = capture("summary_just_started", Destination.SUMMARY) {
        SummaryContent(it, running = true, clock = shiftStart + 16_000)
    }

    @Test fun summaryRunning() = capture("summary_running", Destination.SUMMARY) { SummaryContent(it, running = true) }

    @Test @Config(qualifiers = "+night")
    fun summaryRunningDark() = capture("summary_running_dark", Destination.SUMMARY) { SummaryContent(it, running = true) }

    @Test fun calendar() = capture("calendar", Destination.CALENDAR) { CalendarContent(it) }

    @Test @Config(qualifiers = "+night")
    fun calendarDark() = capture("calendar_dark", Destination.CALENDAR) { CalendarContent(it) }

    @Test fun shifts() = capture("shifts", Destination.SHIFTS) {
        ShiftsScreen(summary = summary, onPreviousMonth = {}, onNextMonth = {}, onToday = {}, onEditShift = {}, contentPadding = it)
    }

    @Test fun settings() = capture("settings", Destination.SETTINGS) {
        SettingsScreen(settings = settings, onSave = {}, contentPadding = it)
    }

    @Composable
    private fun SummaryContent(padding: PaddingValues, running: Boolean, clock: Long = now) {
        SummaryScreen(
            summary = summary,
            partialPay = 0.0,
            activeShiftStart = if (running) shiftStart else null,
            onStartShift = {},
            onFinishShift = {},
            contentPadding = padding,
            clock = { clock },
        )
    }

    @Composable
    private fun CalendarContent(padding: PaddingValues) {
        CalendarScreen(
            summary = summary,
            onPreviousMonth = {},
            onNextMonth = {},
            onToday = {},
            onAddShift = {},
            onEditShift = {},
            contentPadding = padding,
            today = today,
        )
    }

    private fun capture(name: String, tab: Destination, screen: @Composable (PaddingValues) -> Unit) {
        // The wavy ring animates forever; a paused clock keeps the frame fixed.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            VardiyaTheme {
                Scaffold(bottomBar = { VardiyaNavigationBar(destination = tab, onSelect = {}) }) { screen(it) }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
