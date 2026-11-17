package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.local.BlockEventEntity
import com.example.data.local.FocusGuardDatabase
import com.example.security.PinSecurityManager
import com.example.security.SecuritySettings
import com.example.ui.overlay.BlockedOverlayActivity
import com.example.usage.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShortVideoBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var pinSecurityManager: PinSecurityManager
    private lateinit var database: FocusGuardDatabase

    @Volatile
    private var currentSettings: SecuritySettings = SecuritySettings()

    @Volatile
    private var lastShortVideoBlockElapsedMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        pinSecurityManager = PinSecurityManager(applicationContext)
        database = FocusGuardDatabase.getInstance(applicationContext)

        serviceScope.launch {
            pinSecurityManager.settingsFlow.collectLatest { settings ->
                currentSettings = settings
            }
        }
        Log.i(TAG, "FocusGuard Accessibility Guard connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val className = event.className?.toString().orEmpty()
        val settings = currentSettings

        // Short-video feed detection only. Anti-uninstall interception is intentionally disabled.
        // Pillar 1: Short Video & Reels Blocker
        detectShortVideoDistraction(pkg, className, settings)?.let { match ->
            handleShortVideoBlock(pkg, match.appName, match.detail, settings.strictRedirectHome)
        }
    }

    private data class ShortVideoMatch(val appName: String, val detail: String)

    private fun detectShortVideoDistraction(
        pkg: String,
        className: String,
        settings: SecuritySettings
    ): ShortVideoMatch? {
        val lowerClass = className.lowercase()
        return when (pkg) {
            PKG_INSTAGRAM -> {
                if (!settings.reelsBlockerEnabled) return null
                val classMatches = lowerClass.contains("clips") || lowerClass.contains("reel")
                val idMatches = hasAnyViewId(
                    listOf(
                        "com.instagram.android:id/clips_video_container",
                        "com.instagram.android:id/clips_viewer_view_pager",
                        "com.instagram.android:id/reel_viewer_root",
                        "com.instagram.android:id/clips_swipe_refresh_container"
                    )
                )
                if (classMatches || idMatches) {
                    ShortVideoMatch("Instagram Reels", "Short-form Reels feed deflected")
                } else null
            }

            PKG_YOUTUBE -> {
                if (!settings.shortsBlockerEnabled) return null
                val classMatches = lowerClass.contains("reelwatch") || lowerClass.contains("shorts")
                val idMatches = hasAnyViewId(
                    listOf(
                        "com.google.android.youtube:id/reel_recycler",
                        "com.google.android.youtube:id/reel_player_page_container",
                        "com.google.android.youtube:id/shorts_player_container",
                        "com.google.android.youtube:id/reel_watch_player"
                    )
                )
                if (classMatches || idMatches) {
                    ShortVideoMatch("YouTube Shorts", "YouTube Shorts player deflected")
                } else null
            }

            PKG_TIKTOK_GLOBAL, PKG_TIKTOK_ASIA -> {
                if (!settings.tiktokBlockerEnabled) return null
                ShortVideoMatch("TikTok", "Short-video feed blocked")
            }

            PKG_FACEBOOK -> {
                if (!settings.reelsBlockerEnabled) return null
                val classMatches = lowerClass.contains("reels") || lowerClass.contains("fbshorts")
                if (classMatches) {
                    ShortVideoMatch("Facebook Reels", "Facebook Reels deflected")
                } else null
            }

            PKG_SNAPCHAT -> {
                if (!settings.reelsBlockerEnabled) return null
                val idMatches = hasAnyViewId(
                    listOf("com.snapchat.android:id/spotlight_view_container")
                )
                if (lowerClass.contains("spotlight") || idMatches) {
                    ShortVideoMatch("Snapchat Spotlight", "Spotlight feed deflected")
                } else null
            }

            else -> null
        }
    }

    private fun handleShortVideoBlock(
        pkg: String,
        appName: String,
        detail: String,
        strictRedirectHome: Boolean
    ) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastShortVideoBlockElapsedMs < SHORT_VIDEO_COOLDOWN_MS) return
        lastShortVideoBlockElapsedMs = now

        Log.i(TAG, "Deflecting short video in $appName ($pkg)")
        performGlobalAction(GLOBAL_ACTION_BACK)

        if (strictRedirectHome) {
            val overlayIntent = BlockedOverlayActivity.createIntent(
                context = applicationContext,
                reason = BlockedOverlayActivity.REASON_SHORT_VIDEO,
                appName = appName,
                blockedPackage = pkg,
                detail = detail
            )
            startActivity(overlayIntent)
        }

        serviceScope.launch(Dispatchers.IO) {
            database.focusGuardDao().insertBlockEvent(
                BlockEventEntity(
                    dateKey = UsageStatsHelper.todayDateKey(),
                    category = "SHORT_VIDEO",
                    packageName = pkg,
                    appName = appName,
                    detail = detail
                )
            )
        }
    }

    private fun hasAnyViewId(viewIds: List<String>): Boolean {
        val root = rootInActiveWindow ?: return false
        for (id in viewIds) {
            val found = root.findAccessibilityNodeInfosByViewId(id)
            if (!found.isNullOrEmpty()) {
                return true
            }
        }
        return false
    }

    private fun collectVisibleTexts(
        root: AccessibilityNodeInfo,
        out: MutableList<String>,
        maxNodes: Int
    ) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "FocusGuard Accessibility Guard interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "FocusAccessibilitySvc"
        private const val SHORT_VIDEO_COOLDOWN_MS = 700L

        private const val PKG_INSTAGRAM = "com.instagram.android"
        private const val PKG_YOUTUBE = "com.google.android.youtube"
        private const val PKG_TIKTOK_GLOBAL = "com.zhiliaoapp.musically"
        private const val PKG_TIKTOK_ASIA = "com.ss.android.ugc.trill"
        private const val PKG_FACEBOOK = "com.facebook.katana"
        private const val PKG_SNAPCHAT = "com.snapchat.android"

    }
}
