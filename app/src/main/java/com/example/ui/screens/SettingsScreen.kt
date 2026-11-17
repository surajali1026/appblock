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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.security.SecuritySettings
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.LuminousEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.viewmodel.PinActionPurpose

@Composable
fun SettingsScreen(
    settings: SecuritySettings,
    onRequestPinAction: (PinActionPurpose) -> Unit,
    onToggleStrictRedirectHome: (Boolean) -> Unit,
    onToggleUsageEnforcerFlag: (Boolean) -> Unit,
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Vault & PIN Security",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Hardened anti-uninstall guard and PBKDF2-salted 4-digit PIN lock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(
                        1.dp,
                        if (settings.isPinConfigured) LuminousEmerald.copy(alpha = 0.5f)
                        else AmberPulse.copy(alpha = 0.6f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (settings.isPinConfigured) LuminousEmerald else AmberPulse
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (settings.isPinConfigured) "4-Digit Security PIN Active"
                                    else "Configure 4-Digit Security PIN",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (settings.isPinConfigured) {
                                        "Salted PBKDF2-HMAC-SHA256 hash stored locally. Required before uninstall or disabling protection."
                                    } else {
                                        "Set a 4-digit PIN to gate app uninstallation and anti-tamper guard changes."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onRequestPinAction(
                                    if (settings.isPinConfigured) PinActionPurpose.CHANGE_PIN
                                    else PinActionPurpose.SETUP_PIN
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("configure_pin_button")
                        ) {
                            Text(
                                if (settings.isPinConfigured) "Change 4-Digit PIN"
                                else "Set 4-Digit Security PIN"
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Hardened Guard Controls",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (settings.antiUninstallEnabled) LuminousEmerald
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System Anti-Uninstall Guard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Intercepts App Info, Force Stop, and Package Installer uninstall prompts in com.android.settings and kicks back to Home.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.antiUninstallEnabled,
                                onCheckedChange = {
                                    onRequestPinAction(PinActionPurpose.TOGGLE_ANTI_UNINSTALL)
                                },
                                modifier = Modifier.testTag("settings_anti_uninstall_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhonelinkLock,
                                contentDescription = null,
                                tint = if (settings.strictRedirectHome) LuminousEmerald
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Strict Overlay on Short Videos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "In addition to GLOBAL_ACTION_BACK, push the FocusGuard overlay screen when Reels/Shorts are detected.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.strictRedirectHome,
                                onCheckedChange = onToggleStrictRedirectHome,
                                modifier = Modifier.testTag("settings_strict_overlay_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (settings.usageEnforcerEnabled) LuminousEmerald
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enforce Daily Limits in Background",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Allows UsageEnforcerForegroundService to lock over-quota apps until midnight.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.usageEnforcerEnabled,
                                onCheckedChange = onToggleUsageEnforcerFlag,
                                modifier = Modifier.testTag("settings_usage_enforcer_switch")
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateElevated),
                    border = BorderStroke(1.dp, CrimsonGuard.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CrimsonGuard
                            )
                            Text(
                                text = "Authorized App Removal",
                                style = MaterialTheme.typography.titleMedium,
                                color = CrimsonGuard
                            )
                        }
                        Text(
                            text = "System-level uninstallation is blocked by the Accessibility Guard. Verify your 4-digit PIN here to unlock a 35-second system uninstall window.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onRequestPinAction(PinActionPurpose.UNINSTALL_APP) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonGuard,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("uninstall_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(stringResource(R.string.btn_uninstall_app))
                        }
                    }
                }
            }

            item {
                Surface(
                    color = SlateSurface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, SlateOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Setup Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            color = LuminousEmerald
                        )
                        Text(
                            text = "1. Set your 4-digit Security PIN above.\n" +
                                "2. Grant Accessibility Service permission in Shield tab.\n" +
                                "3. Grant Usage Access and start the Usage Enforcer Service.\n" +
                                "4. Add daily limits in Limits tab and compose focus audio in Focus Audio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
