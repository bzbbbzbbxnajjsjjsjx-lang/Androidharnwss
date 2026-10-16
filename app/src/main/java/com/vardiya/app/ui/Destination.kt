package com.vardiya.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

internal enum class Destination(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    SUMMARY("Özet", Icons.Filled.Home, Icons.Outlined.Home),
    CALENDAR("Takvim", Icons.Filled.DateRange, Icons.Outlined.DateRange),
    SHIFTS("Vardiyalar", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Filled.List),
    SETTINGS("Ayarlar", Icons.Filled.Settings, Icons.Outlined.Settings),
}

/** M3 Expressive short navigation bar: 64dp, pill indicator behind the selected icon. */
@Composable
internal fun VardiyaNavigationBar(destination: Destination, onSelect: (Destination) -> Unit) {
    val haptic = LocalHapticFeedback.current
    ShortNavigationBar {
        Destination.entries.forEach { item ->
            val selected = destination == item
            ShortNavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onSelect(item)
                },
                icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                label = { Text(item.label) },
            )
        }
    }
}
