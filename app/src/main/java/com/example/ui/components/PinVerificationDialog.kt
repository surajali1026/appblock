package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.LuminousEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.viewmodel.PinActionPurpose
import com.example.ui.viewmodel.PinDialogUiState

@Composable
fun PinVerificationDialog(
    state: PinDialogUiState,
    onDismiss: () -> Unit,
    onSubmitNewPin: (pin: String, confirmPin: String) -> Unit,
    onVerifyPin: (pin: String, newPin: String) -> Unit
) {
    if (!state.isVisible) return

    var pinInput by remember(state.purpose, state.isVisible) { mutableStateOf("") }
    var secondPinInput by remember(state.purpose, state.isVisible) { mutableStateOf("") }

    val isSetup = state.purpose == PinActionPurpose.SETUP_PIN
    val isChange = state.purpose == PinActionPurpose.CHANGE_PIN
    val isUninstall = state.purpose == PinActionPurpose.UNINSTALL_APP

    val title = when (state.purpose) {
        PinActionPurpose.SETUP_PIN -> stringResource(R.string.pin_dialog_setup_title)
        PinActionPurpose.CHANGE_PIN -> "Change 4-Digit Security PIN"
        PinActionPurpose.UNINSTALL_APP,
        PinActionPurpose.TOGGLE_ANTI_UNINSTALL -> stringResource(R.string.pin_dialog_verify_title)
    }

    val subtitle = when (state.purpose) {
        PinActionPurpose.SETUP_PIN -> stringResource(R.string.pin_dialog_setup_subtitle)
        PinActionPurpose.CHANGE_PIN -> "Verify your current PIN and enter a new 4-digit PIN."
        PinActionPurpose.UNINSTALL_APP -> stringResource(R.string.pin_dialog_verify_uninstall_subtitle)
        PinActionPurpose.TOGGLE_ANTI_UNINSTALL -> stringResource(R.string.pin_dialog_verify_guard_subtitle)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isUninstall) CrimsonGuard.copy(alpha = 0.16f)
                        else LuminousEmerald.copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUninstall) Icons.Default.Lock else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isUninstall) CrimsonGuard else LuminousEmerald
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Visual 4-dot PIN progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(4) { index ->
                        val filled = index < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (filled) LuminousEmerald else SlateElevated
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (filled) LuminousEmerald else SlateOutline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() }.take(4)
                        pinInput = filtered
                    },
                    label = {
                        Text(
                            if (isChange) "Current 4-digit PIN"
                            else stringResource(R.string.pin_hint)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field")
                )

                if (isSetup || isChange) {
                    OutlinedTextField(
                        value = secondPinInput,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() }.take(4)
                            secondPinInput = filtered
                        },
                        label = {
                            Text(
                                if (isChange) "New 4-digit PIN"
                                else stringResource(R.string.pin_confirm_hint)
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_confirm_input_field")
                    )
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonGuard,
                        modifier = Modifier.testTag("pin_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSetup) {
                        onSubmitNewPin(pinInput, secondPinInput)
                    } else {
                        onVerifyPin(pinInput, secondPinInput)
                    }
                },
                enabled = !state.isVerifying &&
                    pinInput.length == 4 &&
                    ((!isSetup && !isChange) || secondPinInput.length == 4),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUninstall) CrimsonGuard else LuminousEmerald,
                    contentColor = if (isUninstall) MaterialTheme.colorScheme.onError else ObsidianVoid
                ),
                modifier = Modifier.testTag("pin_confirm_button")
            ) {
                if (state.isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isSetup || isChange) {
                            stringResource(R.string.btn_save_pin)
                        } else {
                            stringResource(R.string.btn_verify_unlock)
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("pin_cancel_button")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
