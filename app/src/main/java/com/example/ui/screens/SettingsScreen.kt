package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.security.SecuritySettings
import com.example.ui.viewmodel.PinActionPurpose

@Composable
fun SettingsScreen(
    settings: SecuritySettings,
    onRequestPinAction: (PinActionPurpose) -> Unit,
    onToggleStrictRedirectHome: (Boolean) -> Unit,
    onToggleUsageEnforcerFlag: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FocusGuard Lite settings", style = MaterialTheme.typography.headlineSmall)
                    Text("Lite keeps local PIN settings. Reels blocking and system anti-uninstall controls are not included.")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("App PIN", style = MaterialTheme.typography.titleLarge)
                    Text(if (settings.isPinConfigured) "A local 4-digit PIN is configured." else "Set a 4-digit PIN for app settings.")
                    OutlinedButton(onClick = { onRequestPinAction(if (settings.isPinConfigured) PinActionPurpose.CHANGE_PIN else PinActionPurpose.SETUP_PIN) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (settings.isPinConfigured) "Change PIN" else "Set PIN")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily usage monitor", style = MaterialTheme.typography.titleLarge)
                    Text("Enable or pause background daily-limit enforcement.")
                    Switch(checked = settings.usageEnforcerEnabled, onCheckedChange = onToggleUsageEnforcerFlag)
                }
            }
        }
    }
}
