package com.example

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.security.PinSecurityManager
import com.example.security.PinVerifyResult
import com.example.usage.BatteryOptimizationHelper
import com.example.usage.UsageStatsHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FocusGuard Lite", appName)
    }

    @Test
    fun `pin security manager hashes and verifies 4 digit pin`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = PinSecurityManager(context)

        assertFalse(PinSecurityManager.isValidPinFormat("123"))
        assertFalse(PinSecurityManager.isValidPinFormat("12a4"))
        assertTrue(PinSecurityManager.isValidPinFormat("4829"))

        assertTrue(manager.setPin("4829"))
        assertEquals(PinVerifyResult.Success, manager.verifyPin("4829"))
        assertTrue(manager.verifyPin("0000") is PinVerifyResult.Invalid)
    }

    @Test
    fun `usage stats helper formats durations and midnight window`() {
        assertEquals("30m 0s", UsageStatsHelper.formatDuration(30 * 60_000L))
        assertEquals("1h 5m", UsageStatsHelper.formatDuration(65 * 60_000L))
        assertTrue(UsageStatsHelper.millisUntilMidnight() in 0L..86_400_000L)
    }

    @Test
    fun `battery optimization helper builds valid system exemption intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val requestIntent = BatteryOptimizationHelper.createRequestExemptionIntent(context)
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, requestIntent.action)
        assertEquals("package:${context.packageName}", requestIntent.dataString)

        val settingsIntent = BatteryOptimizationHelper.createBatteryOptimizationSettingsIntent()
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, settingsIntent.action)
    }
}
