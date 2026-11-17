package com.example.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import com.example.service.VideoAndUninstallBlockerAccessibilityService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val todayUsageMs: Long = 0L
)

object UsageStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(
            context,
            VideoAndUninstallBlockerAccessibilityService::class.java
        )
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val componentString = splitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentString)
            if (enabledComponent == expectedComponent) {
                return true
            }
        }
        return false
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun todayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getTodayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun millisUntilMidnight(): Long {
        val now = System.currentTimeMillis()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return (nextMidnight - now).coerceAtLeast(0L)
    }

    /**
     * Computes accurate foreground usage duration (in milliseconds) per package since midnight today
     * using UsageEvents activity lifecycle transitions, with queryUsageStats fallback.
     */
    fun getDailyUsageMap(context: Context): Map<String, Long> {
        if (!hasUsageStatsPermission(context)) return emptyMap()
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()

        val startMillis = getTodayStartMillis()
        val nowMillis = System.currentTimeMillis()
        if (nowMillis <= startMillis) return emptyMap()

        val totals = HashMap<String, Long>()
        val activeStarts = HashMap<String, Long>()

        val events = usageStatsManager.queryEvents(startMillis, nowMillis)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    activeStarts[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val startedAt = activeStarts.remove(pkg)
                    if (startedAt != null && event.timeStamp >= startedAt) {
                        totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - startedAt)
                    }
                }
            }
        }

        // Add ongoing foreground session duration if an app is currently active
        for ((pkg, startedAt) in activeStarts) {
            if (nowMillis >= startedAt) {
                totals[pkg] = (totals[pkg] ?: 0L) + (nowMillis - startedAt)
            }
        }

        // Fallback/supplement with aggregated UsageStats if events yielded nothing for a package
        val aggregated = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startMillis,
            nowMillis
        )
        if (!aggregated.isNullOrEmpty()) {
            for (stat in aggregated) {
                val pkg = stat.packageName ?: continue
                if ((totals[pkg] ?: 0L) == 0L && stat.totalTimeInForeground > 0L) {
                    totals[pkg] = stat.totalTimeInForeground
                }
            }
        }

        return totals
    }

    /**
     * Determines the currently visible foreground package using recent UsageEvents.
     */
    fun getForegroundPackageName(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null

        val now = System.currentTimeMillis()
        val windowStart = now - 120_000L
        val events = usageStatsManager.queryEvents(windowStart, now)
        val event = UsageEvents.Event()
        var latestResumedPkg: String? = null
        var latestResumedTime = 0L
        val pausedAfterResume = HashMap<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (event.timeStamp >= latestResumedTime) {
                        latestResumedTime = event.timeStamp
                        latestResumedPkg = pkg
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    pausedAfterResume[pkg] = maxOf(pausedAfterResume[pkg] ?: 0L, event.timeStamp)
                }
            }
        }

        if (latestResumedPkg != null) {
            val lastPause = pausedAfterResume[latestResumedPkg] ?: 0L
            if (latestResumedTime >= lastPause) {
                return latestResumedPkg
            }
        }
        return null
    }

    fun getInstalledUserApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val usageMap = getDailyUsageMap(context)
        val seen = HashSet<String>()
        val result = ArrayList<InstalledAppInfo>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || !seen.add(pkg)) continue
            val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
            result.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    todayUsageMs = usageMap[pkg] ?: 0L
                )
            )
        }

        return result.sortedWith(
            compareByDescending<InstalledAppInfo> { it.todayUsageMs }
                .thenBy { it.appName.lowercase(Locale.getDefault()) }
        )
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatCountdown(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
