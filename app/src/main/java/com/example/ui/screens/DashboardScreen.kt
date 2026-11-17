package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.BlockEventEntity
import com.example.security.SecuritySettings
import com.example.ui.viewmodel.PermissionHealthState

@Composable
fun DashboardScreen(
    health: PermissionHealthState,
    settings: SecuritySettings,
    todayBlockCount: Int,
    recentEvents: List<BlockEventEntity>,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onToggleEnforcerService: () -> Unit,
    onToggleReelsBlocker: (Boolean) -> Unit,
    onToggleShortsBlocker: (Boolean) -> Unit,
    onToggleTiktokBlocker: (Boolean) -> Unit,
    onToggleAntiUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FocusGuard Lite", style = MaterialTheme.typography.headlineMedium)
                    Text("Screen-time tracking and daily app limits. Accessibility blocking and anti-uninstall protection have been removed.")
                    Text("Today's limit events: $todayBlockCount")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Usage access", style = MaterialTheme.typography.titleLarge)
                    Text(if (health.usageStatsGranted) "Permission granted" else "Grant Usage Access to read daily app usage.")
                    Button(onClick = onOpenUsageAccessSettings, modifier = Modifier.fillMaxWidth()) {
                        Text(if (health.usageStatsGranted) "Review Usage Access" else "Grant Usage Access")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily limit monitor", style = MaterialTheme.typography.titleLarge)
                    Text(if (health.enforcerServiceRunning) "Monitor is running" else "Monitor is stopped")
                    Button(onClick = onToggleEnforcerService, enabled = health.usageStatsGranted, modifier = Modifier.fillMaxWidth()) {
                        Text(if (health.enforcerServiceRunning) "Stop monitor" else "Start monitor")
                    }
                    Text("Set app-specific daily limits in the Limits tab. Android may restrict background monitoring depending on battery settings.")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Optional permissions", style = MaterialTheme.typography.titleLarge)
                    Text("Notifications: ${if (health.notificationsGranted) "Allowed" else "Not allowed"}")
                    if (!health.notificationsGranted) {
                        OutlinedButton(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) { Text("Allow notifications") }
                    }
                    Text("Battery optimization: ${if (health.batteryOptimizationExempt) "Unrestricted" else "May be restricted"}")
                    OutlinedButton(onClick = onRequestBatteryOptimization, modifier = Modifier.fillMaxWidth()) { Text("Battery settings") }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recent limit events", style = MaterialTheme.typography.titleLarge)
                    if (recentEvents.isEmpty()) Text("No limit events recorded yet.")
                    else recentEvents.take(8).forEach { event ->
                        Text("${event.appName}: ${event.detail}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
