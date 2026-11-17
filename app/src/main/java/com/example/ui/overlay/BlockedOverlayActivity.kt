package com.example.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.FocusGuardTheme
import com.example.ui.theme.LuminousEmerald
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.usage.UsageStatsHelper
import kotlinx.coroutines.delay

class BlockedOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val reason = intent.getStringExtra(EXTRA_REASON) ?: REASON_USAGE_LIMIT
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Restricted App"
        val detail = intent.getStringExtra(EXTRA_DETAIL) ?: "Protected by FocusGuard."

        setContent {
            FocusGuardTheme {
                BackHandler {
                    navigateToHomeScreen()
                }
                BlockedOverlayScreen(
                    reason = reason,
                    appName = appName,
                    detail = detail,
                    onGoHome = { navigateToHomeScreen() },
                    onOpenFocusAudio = { openFocusAudioStudio() }
                )
            }
        }
    }

    private fun navigateToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun openFocusAudioStudio() {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_AUDIO)
        }
        startActivity(mainIntent)
        finish()
    }

    companion object {
        const val REASON_USAGE_LIMIT = "USAGE_LIMIT"
        const val REASON_SHORT_VIDEO = "SHORT_VIDEO"
        const val REASON_ANTI_UNINSTALL = "ANTI_UNINSTALL"

        private const val EXTRA_REASON = "extra_reason"
        private const val EXTRA_APP_NAME = "extra_app_name"
        private const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        private const val EXTRA_DETAIL = "extra_detail"

        fun createIntent(
            context: Context,
            reason: String,
            appName: String,
            blockedPackage: String,
            detail: String
        ): Intent {
            return Intent(context, BlockedOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_REASON, reason)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_BLOCKED_PACKAGE, blockedPackage)
                putExtra(EXTRA_DETAIL, detail)
            }
        }
    }
}

@Composable
private fun BlockedOverlayScreen(
    reason: String,
    appName: String,
    detail: String,
    onGoHome: () -> Unit,
    onOpenFocusAudio: () -> Unit
) {
    var countdownMs by remember { mutableLongStateOf(UsageStatsHelper.millisUntilMidnight()) }
    LaunchedEffect(Unit) {
        while (true) {
            countdownMs = UsageStatsHelper.millisUntilMidnight()
            delay(1000L)
        }
    }

    val accentColor = when (reason) {
        BlockedOverlayActivity.REASON_ANTI_UNINSTALL -> CrimsonGuard
        BlockedOverlayActivity.REASON_USAGE_LIMIT -> AmberPulse
        else -> LuminousEmerald
    }
    val badgeLabel = when (reason) {
        BlockedOverlayActivity.REASON_ANTI_UNINSTALL -> "PIN GUARD INTERCEPTION"
        BlockedOverlayActivity.REASON_USAGE_LIMIT -> "DAILY QUOTA REACHED"
        else -> "SHORT VIDEO DEFLECTED"
    }
    val icon = when (reason) {
        BlockedOverlayActivity.REASON_ANTI_UNINSTALL -> Icons.Default.Lock
        BlockedOverlayActivity.REASON_USAGE_LIMIT -> Icons.Default.TimerOff
        else -> Icons.Default.Shield
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightNavy, ObsidianVoid, ObsidianVoid)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = badgeLabel,
                    tint = accentColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Text(
                text = appName,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (reason == BlockedOverlayActivity.REASON_USAGE_LIMIT) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SlateElevated,
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unlocks at Midnight",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = UsageStatsHelper.formatCountdown(countdownMs),
                            style = MaterialTheme.typography.titleLarge,
                            color = AmberPulse
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("overlay_home_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuminousEmerald,
                    contentColor = ObsidianVoid
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.overlay_return_home),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = onOpenFocusAudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("overlay_focus_audio_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.overlay_open_focus_audio),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
