package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.BlockEventEntity
import com.example.data.local.FocusGuardDatabase
import com.example.security.PinSecurityManager
import com.example.ui.overlay.BlockedOverlayActivity
import com.example.usage.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsageEnforcerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private val lastEnforcedMap = HashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startMonitoringLoop()
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        if (monitorJob?.isActive == true) return

        val database = FocusGuardDatabase.getInstance(applicationContext)
        val dao = database.focusGuardDao()
        val pinSecurityManager = PinSecurityManager(applicationContext)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    val settings = pinSecurityManager.settingsFlow.first()
                    if (settings.usageEnforcerEnabled &&
                        powerManager.isInteractive &&
                        UsageStatsHelper.hasUsageStatsPermission(applicationContext)
                    ) {
                        val enabledLimits = dao.getEnabledLimitsOnce()
                        if (enabledLimits.isNotEmpty()) {
                            val foregroundPkg =
                                UsageStatsHelper.getForegroundPackageName(applicationContext)
                            if (foregroundPkg != null && foregroundPkg != packageName) {
                                val matchingRule = enabledLimits.firstOrNull {
                                    it.packageName == foregroundPkg
                                }
                                if (matchingRule != null) {
                                    val usageMap =
                                        UsageStatsHelper.getDailyUsageMap(applicationContext)
                                    val usedMs = usageMap[foregroundPkg] ?: 0L
                                    val limitMs = matchingRule.dailyLimitMinutes * 60_000L
                                    if (usedMs >= limitMs) {
                                        enforceLimitOverlay(
                                            dao = dao,
                                            packageName = foregroundPkg,
                                            appName = matchingRule.appName,
                                            usedMs = usedMs,
                                            limitMinutes = matchingRule.dailyLimitMinutes
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during usage enforcement cycle", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun enforceLimitOverlay(
        dao: com.example.data.local.FocusGuardDao,
        packageName: String,
        appName: String,
        usedMs: Long,
        limitMinutes: Int
    ) {
        val nowElapsed = SystemClock.elapsedRealtime()
        val lastElapsed = lastEnforcedMap[packageName] ?: 0L
        if (nowElapsed - lastElapsed < OVERLAY_COOLDOWN_MS) return
        lastEnforcedMap[packageName] = nowElapsed

        val usedText = UsageStatsHelper.formatDuration(usedMs)
        Log.i(TAG, "Daily limit exceeded for $appName ($usedText / ${limitMinutes}m)")

        val overlayIntent = BlockedOverlayActivity.createIntent(
            context = applicationContext,
            reason = BlockedOverlayActivity.REASON_USAGE_LIMIT,
            appName = appName,
            blockedPackage = packageName,
            detail = "Used $usedText today (limit: ${limitMinutes}m). Locked until midnight."
        )
        startActivity(overlayIntent)

        dao.insertBlockEvent(
            BlockEventEntity(
                dateKey = UsageStatsHelper.todayDateKey(),
                category = "USAGE_LIMIT",
                packageName = packageName,
                appName = appName,
                detail = "Daily limit reached ($usedText / ${limitMinutes}m)"
            )
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.fgs_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(getString(R.string.fgs_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        monitorJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "UsageEnforcerService"
        private const val CHANNEL_ID = "focusguard_enforcer_channel"
        private const val NOTIFICATION_ID = 4102
        private const val POLL_INTERVAL_MS = 2_000L
        private const val OVERLAY_COOLDOWN_MS = 2_500L
        const val ACTION_STOP = "com.example.service.ACTION_STOP_ENFORCER"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, UsageEnforcerForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, UsageEnforcerForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
