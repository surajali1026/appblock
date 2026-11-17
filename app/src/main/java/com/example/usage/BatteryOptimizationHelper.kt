package com.example.usage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helper for checking and requesting Android Doze / Battery Optimization exemptions
 * so background usage tracking and enforcement services are not throttled by OEMs.
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun createRequestExemptionIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Requests a battery optimization exemption directly via system prompt, falling back
     * to the battery optimization list or app details screen on OEM devices that restrict
     * the direct dialog intent.
     *
     * @return true if an exemption prompt or settings screen was opened, false if already exempt or failed.
     */
    fun requestBatteryOptimizationExemption(context: Context): Boolean {
        if (isIgnoringBatteryOptimizations(context)) {
            return false
        }

        val intents = listOf(
            createRequestExemptionIntent(context),
            createBatteryOptimizationSettingsIntent(),
            createAppDetailsIntent(context)
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Unable to launch intent ${intent.action}", e)
            }
        }
        return false
    }
}
