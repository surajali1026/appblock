package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BlockEventEntity
import com.example.security.SecuritySettings
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LuminousEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.viewmodel.PermissionHealthState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ShieldHeroCard(
                    health = health,
                    todayBlockCount = todayBlockCount,
                    isPinConfigured = settings.isPinConfigured
                )
            }

            item {
                Text(
                    text = "System Guard Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PermissionStatusCard(
                        icon = Icons.Default.AccessibilityNew,
                        title = stringResource(R.string.accessibility_perm_title),
                        description = stringResource(R.string.accessibility_perm_desc),
                        isGranted = health.accessibilityEnabled,
                        actionLabel = if (health.accessibilityEnabled) "Manage" else stringResource(R.string.btn_grant_access),
                        onClick = onOpenAccessibilitySettings,
                        tag = "grant_accessibility_button"
                    )
                    PermissionStatusCard(
                        icon = Icons.Default.QueryStats,
                        title = stringResource(R.string.usage_perm_title),
                        description = stringResource(R.string.usage_perm_desc),
                        isGranted = health.usageStatsGranted,
                        actionLabel = if (health.usageStatsGranted) "Review" else stringResource(R.string.btn_grant_access),
                        onClick = onOpenUsageAccessSettings,
                        tag = "grant_usage_access_button"
                    )
                    if (!health.notificationsGranted) {
                        PermissionStatusCard(
                            icon = Icons.Default.NotificationsActive,
                            title = stringResource(R.string.notification_perm_title),
                            description = stringResource(R.string.notification_perm_desc),
                            isGranted = false,
                            actionLabel = stringResource(R.string.btn_grant_access),
                            onClick = onRequestNotificationPermission,
                            tag = "grant_notifications_button"
                        )
                    }
                    PermissionStatusCard(
                        icon = Icons.Default.BatteryChargingFull,
                        title = stringResource(R.string.battery_perm_title),
                        description = stringResource(R.string.battery_perm_desc),
                        isGranted = health.batteryOptimizationExempt,
                        actionLabel = if (health.batteryOptimizationExempt) {
                            stringResource(R.string.btn_battery_exempted)
                        } else {
                            stringResource(R.string.btn_exempt_battery)
                        },
                        onClick = onRequestBatteryOptimization,
                        tag = "grant_battery_exemption_button"
                    )
                    EnforcerServiceCard(
                        isRunning = health.enforcerServiceRunning,
                        usageGranted = health.usageStatsGranted,
                        onToggle = onToggleEnforcerService
                    )
                }
            }

            item {
                Text(
                    text = "Active Distraction & Tamper Shields",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShieldToggleRow(
                            icon = Icons.Default.VideoLibrary,
                            title = "Instagram & Social Reels Blocker",
                            subtitle = "Detects Reels/Spotlight views and triggers instant Back",
                            checked = settings.reelsBlockerEnabled,
                            onCheckedChange = onToggleReelsBlocker,
                            tag = "toggle_reels_blocker"
                        )
                        ShieldToggleRow(
                            icon = Icons.Default.VideoLibrary,
                            title = "YouTube Shorts Blocker",
                            subtitle = "Intercepts ReelWatchActivity & Shorts players",
                            checked = settings.shortsBlockerEnabled,
                            onCheckedChange = onToggleShortsBlocker,
                            tag = "toggle_shorts_blocker"
                        )
                        ShieldToggleRow(
                            icon = Icons.Default.VideoLibrary,
                            title = "TikTok Feed Blocker",
                            subtitle = "Blocks short-form feed entry immediately",
                            checked = settings.tiktokBlockerEnabled,
                            onCheckedChange = onToggleTiktokBlocker,
                            tag = "toggle_tiktok_blocker"
                        )
                        ShieldToggleRow(
                            icon = Icons.Default.Lock,
                            title = "System Anti-Uninstall Guard",
                            subtitle = "Blocks App Info & System Uninstall via Accessibility (PIN-locked)",
                            checked = settings.antiUninstallEnabled,
                            onCheckedChange = { onToggleAntiUninstall() },
                            tag = "toggle_anti_uninstall_dashboard"
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Interceptions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${recentEvents.size} logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recentEvents.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = LuminousEmerald
                            )
                            Text(
                                text = "No distraction or tamper interceptions recorded yet. Enable Accessibility and Usage Enforcer above to activate real-time protection.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentEvents.take(12), key = { it.id }) { event ->
                    BlockEventRow(event = event)
                }
            }
        }
    }
}

@Composable
private fun ShieldHeroCard(
    health: PermissionHealthState,
    todayBlockCount: Int,
    isPinConfigured: Boolean
) {
    val allCoreReady = health.accessibilityEnabled && health.usageStatsGranted
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, SlateOutline),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_shield),
                contentDescription = "FocusGuard Shield Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianVoid.copy(alpha = 0.25f),
                                ObsidianVoid.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (allCoreReady) LuminousEmerald.copy(alpha = 0.18f)
                        else AmberPulse.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (allCoreReady) LuminousEmerald else AmberPulse)
                            )
                            Text(
                                text = if (allCoreReady) stringResource(R.string.status_protected)
                                else stringResource(R.string.status_attention),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (allCoreReady) LuminousEmerald else AmberPulse
                            )
                        }
                    }
                    Surface(
                        color = SlateElevated.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (isPinConfigured) "PIN LOCKED" else "PIN UNSET",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPinConfigured) ElectricCyan else CrimsonGuard,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.hero_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.hero_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricPill(
                        label = "TODAY BLOCKED",
                        value = todayBlockCount.toString(),
                        accent = LuminousEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "SYSTEM GUARDS",
                        value = "${health.activeCount}/5",
                        accent = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SlateElevated.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, SlateOutline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(
            1.dp,
            if (isGranted) LuminousEmerald.copy(alpha = 0.4f) else AmberPulse.copy(alpha = 0.45f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isGranted) LuminousEmerald.copy(alpha = 0.14f)
                        else AmberPulse.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) LuminousEmerald else AmberPulse
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = if (isGranted) "Enabled" else "Required",
                        tint = if (isGranted) LuminousEmerald else AmberPulse,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.testTag(tag)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun EnforcerServiceCard(
    isRunning: Boolean,
    usageGranted: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateElevated),
        border = BorderStroke(1.dp, if (isRunning) LuminousEmerald.copy(alpha = 0.5f) else SlateOutline),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Background Usage Enforcer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isRunning) "Foreground monitor active — locks over-limit apps until midnight."
                    else if (usageGranted) "Ready to start real-time daily limit enforcement."
                    else "Requires Usage Access permission before starting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) SlateSurface else LuminousEmerald,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onSurface else ObsidianVoid
                ),
                modifier = Modifier.testTag("toggle_enforcer_service_button")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = if (isRunning) stringResource(R.string.btn_stop_enforcer)
                    else stringResource(R.string.btn_start_enforcer)
                )
            }
        }
    }
}

@Composable
private fun ShieldToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) LuminousEmerald else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun BlockEventRow(event: BlockEventEntity) {
    val accent = when (event.category) {
        "ANTI_UNINSTALL" -> CrimsonGuard
        "USAGE_LIMIT" -> AmberPulse
        else -> LuminousEmerald
    }
    val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMs))
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateOutline),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}
