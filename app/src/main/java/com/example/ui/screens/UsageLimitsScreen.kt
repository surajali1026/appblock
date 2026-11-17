package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLimitEntity
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.LuminousEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.usage.InstalledAppInfo
import com.example.usage.UsageStatsHelper
import kotlin.math.roundToInt

@Composable
fun UsageLimitsScreen(
    usageStatsGranted: Boolean,
    appLimits: List<AppLimitEntity>,
    installedApps: List<InstalledAppInfo>,
    dailyUsageMap: Map<String, Long>,
    onOpenUsageSettings: () -> Unit,
    onRefreshUsage: () -> Unit,
    onSaveLimit: (packageName: String, appName: String, limitMinutes: Int) -> Unit,
    onToggleLimitEnabled: (AppLimitEntity) -> Unit,
    onDeleteLimit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editingInitialMinutes by remember { mutableStateOf(30) }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val limitByPkg = remember(appLimits) { appLimits.associateBy { it.packageName } }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Usage Enforcer",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Daily quotas lock target apps until midnight.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefreshUsage,
                        modifier = Modifier.testTag("refresh_usage_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Usage Stats",
                            tint = LuminousEmerald
                        )
                    }
                }
            }

            if (!usageStatsGranted) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateElevated),
                        border = BorderStroke(1.dp, AmberPulse.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Usage Access Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberPulse
                            )
                            Text(
                                text = "Grant PACKAGE_USAGE_STATS so FocusGuard can compute accurate daily screen time and enforce midnight app locks.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onOpenUsageSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberPulse,
                                    contentColor = ObsidianVoid
                                ),
                                modifier = Modifier.testTag("limits_grant_usage_button")
                            ) {
                                Text("Open Usage Access Settings")
                            }
                        }
                    }
                }
            }

            if (appLimits.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Enforced Limits (${appLimits.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(appLimits, key = { "rule_${it.packageName}" }) { rule ->
                    val usedMs = dailyUsageMap[rule.packageName] ?: 0L
                    val limitMs = rule.dailyLimitMinutes * 60_000L
                    val progress = (usedMs.toFloat() / limitMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    val exceeded = usedMs >= limitMs

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(
                            1.dp,
                            if (exceeded && rule.isEnabled) CrimsonGuard.copy(alpha = 0.65f) else SlateOutline
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rule.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${UsageStatsHelper.formatDuration(usedMs)} / ${rule.dailyLimitMinutes}m limit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (exceeded) CrimsonGuard else LuminousEmerald
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = rule.isEnabled,
                                        onCheckedChange = { onToggleLimitEnabled(rule) },
                                        modifier = Modifier.testTag("limit_switch_${rule.packageName}")
                                    )
                                    IconButton(
                                        onClick = { onDeleteLimit(rule.packageName) },
                                        modifier = Modifier.testTag("delete_limit_${rule.packageName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove limit for ${rule.appName}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = if (exceeded) CrimsonGuard else LuminousEmerald,
                                trackColor = SlateElevated
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (exceeded) "LOCKED UNTIL MIDNIGHT" else "Monitoring active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (exceeded) CrimsonGuard else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        editingInitialMinutes = rule.dailyLimitMinutes
                                        editingTarget = rule.packageName to rule.appName
                                    }
                                ) {
                                    Text("Adjust Limit")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Installed Apps Today",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search apps to limit (e.g., Instagram, YouTube)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_search_field")
                )
            }

            if (filteredApps.isEmpty()) {
                item {
                    Surface(
                        color = SlateSurface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, SlateOutline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = LuminousEmerald)
                            Text(
                                text = "No matching launchable apps found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    val existingRule = limitByPkg[app.packageName]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Today: ${UsageStatsHelper.formatDuration(app.todayUsageMs)} • ${app.packageName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    editingInitialMinutes = existingRule?.dailyLimitMinutes ?: 30
                                    editingTarget = app.packageName to app.appName
                                },
                                modifier = Modifier.testTag("set_limit_${app.packageName}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAlarm,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(existingRule?.let { "${it.dailyLimitMinutes}m" } ?: "Set Limit")
                            }
                        }
                    }
                }
            }
        }
    }

    editingTarget?.let { (pkg, name) ->
        SetDailyLimitDialog(
            appName = name,
            initialMinutes = editingInitialMinutes,
            onDismiss = { editingTarget = null },
            onSave = { minutes ->
                onSaveLimit(pkg, name, minutes)
                editingTarget = null
            }
        )
    }
}

@Composable
private fun SetDailyLimitDialog(
    appName: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var sliderValue by remember(initialMinutes) { mutableFloatStateOf(initialMinutes.toFloat()) }
    val presets = listOf(10, 20, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        title = {
            Text(
                text = "Daily Limit: $appName",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "When today's usage reaches ${sliderValue.roundToInt()} minutes, FocusGuard locks the app until midnight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${sliderValue.roundToInt()} minutes / day",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LuminousEmerald
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 5f..180f,
                    modifier = Modifier.testTag("limit_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = sliderValue.roundToInt() == preset,
                            onClick = { sliderValue = preset.toFloat() },
                            label = { Text("${preset}m") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(sliderValue.roundToInt()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuminousEmerald,
                    contentColor = ObsidianVoid
                ),
                modifier = Modifier.testTag("save_limit_confirm_button")
            ) {
                Text("Save Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
