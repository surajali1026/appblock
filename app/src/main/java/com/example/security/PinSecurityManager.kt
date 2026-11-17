package com.example.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focusguard_security"
)

data class SecuritySettings(
    val isPinConfigured: Boolean = false,
    val antiUninstallEnabled: Boolean = true,
    val reelsBlockerEnabled: Boolean = true,
    val shortsBlockerEnabled: Boolean = true,
    val tiktokBlockerEnabled: Boolean = true,
    val usageEnforcerEnabled: Boolean = true,
    val strictRedirectHome: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntilMs: Long = 0L,
    val uninstallAuthorizedUntilMs: Long = 0L
) {
    fun isUninstallAuthorized(nowMs: Long = System.currentTimeMillis()): Boolean {
        return nowMs < uninstallAuthorizedUntilMs
    }

    fun remainingLockoutSeconds(nowMs: Long = System.currentTimeMillis()): Int {
        return if (nowMs < lockoutUntilMs) ((lockoutUntilMs - nowMs) / 1000L).toInt() + 1 else 0
    }
}

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data class Invalid(val attemptsRemaining: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Int) : PinVerifyResult
    data object NotConfigured : PinVerifyResult
}

class PinSecurityManager(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val ANTI_UNINSTALL = booleanPreferencesKey("anti_uninstall_enabled")
        val REELS_BLOCKER = booleanPreferencesKey("reels_blocker_enabled")
        val SHORTS_BLOCKER = booleanPreferencesKey("shorts_blocker_enabled")
        val TIKTOK_BLOCKER = booleanPreferencesKey("tiktok_blocker_enabled")
        val USAGE_ENFORCER = booleanPreferencesKey("usage_enforcer_enabled")
        val STRICT_REDIRECT_HOME = booleanPreferencesKey("strict_redirect_home")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("lockout_until_ms")
        val UNINSTALL_AUTH_UNTIL = longPreferencesKey("uninstall_auth_until_ms")
    }

    val settingsFlow: Flow<SecuritySettings> = context.securityDataStore.data.map { prefs ->
        SecuritySettings(
            isPinConfigured = !prefs[Keys.PIN_HASH].isNullOrBlank() &&
                !prefs[Keys.PIN_SALT].isNullOrBlank(),
            antiUninstallEnabled = prefs[Keys.ANTI_UNINSTALL] ?: true,
            reelsBlockerEnabled = prefs[Keys.REELS_BLOCKER] ?: true,
            shortsBlockerEnabled = prefs[Keys.SHORTS_BLOCKER] ?: true,
            tiktokBlockerEnabled = prefs[Keys.TIKTOK_BLOCKER] ?: true,
            usageEnforcerEnabled = prefs[Keys.USAGE_ENFORCER] ?: true,
            strictRedirectHome = prefs[Keys.STRICT_REDIRECT_HOME] ?: false,
            failedAttempts = prefs[Keys.FAILED_ATTEMPTS] ?: 0,
            lockoutUntilMs = prefs[Keys.LOCKOUT_UNTIL] ?: 0L,
            uninstallAuthorizedUntilMs = prefs[Keys.UNINSTALL_AUTH_UNTIL] ?: 0L
        )
    }

    suspend fun setPin(pin: String): Boolean {
        if (!isValidPinFormat(pin)) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2Hash(pin, salt)
        context.securityDataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
            prefs[Keys.PIN_HASH] = Base64.encodeToString(hash, Base64.NO_WRAP)
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.LOCKOUT_UNTIL] = 0L
        }
        return true
    }

    suspend fun verifyPin(pin: String): PinVerifyResult {
        val prefs = context.securityDataStore.data.first()
        val storedHashB64 = prefs[Keys.PIN_HASH]
        val storedSaltB64 = prefs[Keys.PIN_SALT]

        if (storedHashB64.isNullOrBlank() || storedSaltB64.isNullOrBlank()) {
            return PinVerifyResult.NotConfigured
        }

        val now = System.currentTimeMillis()
        val lockoutUntil = prefs[Keys.LOCKOUT_UNTIL] ?: 0L
        if (now < lockoutUntil) {
            val remaining = ((lockoutUntil - now) / 1000L).toInt() + 1
            return PinVerifyResult.LockedOut(remaining)
        }

        if (!isValidPinFormat(pin)) {
            return recordFailure(prefs[Keys.FAILED_ATTEMPTS] ?: 0, now)
        }

        val salt = Base64.decode(storedSaltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(storedHashB64, Base64.NO_WRAP)
        val candidateHash = pbkdf2Hash(pin, salt)

        return if (MessageDigest.isEqual(expectedHash, candidateHash)) {
            context.securityDataStore.edit { editPrefs ->
                editPrefs[Keys.FAILED_ATTEMPTS] = 0
                editPrefs[Keys.LOCKOUT_UNTIL] = 0L
            }
            PinVerifyResult.Success
        } else {
            recordFailure(prefs[Keys.FAILED_ATTEMPTS] ?: 0, now)
        }
    }

    private suspend fun recordFailure(previousAttempts: Int, now: Long): PinVerifyResult {
        val nextAttempts = previousAttempts + 1
        return if (nextAttempts >= MAX_ATTEMPTS) {
            val lockoutUntil = now + LOCKOUT_DURATION_MS
            context.securityDataStore.edit { editPrefs ->
                editPrefs[Keys.FAILED_ATTEMPTS] = 0
                editPrefs[Keys.LOCKOUT_UNTIL] = lockoutUntil
            }
            PinVerifyResult.LockedOut((LOCKOUT_DURATION_MS / 1000L).toInt())
        } else {
            context.securityDataStore.edit { editPrefs ->
                editPrefs[Keys.FAILED_ATTEMPTS] = nextAttempts
            }
            PinVerifyResult.Invalid(MAX_ATTEMPTS - nextAttempts)
        }
    }

    suspend fun authorizeUninstallWindow(durationMs: Long = 35_000L) {
        val until = System.currentTimeMillis() + durationMs
        context.securityDataStore.edit { prefs ->
            prefs[Keys.UNINSTALL_AUTH_UNTIL] = until
        }
    }

    suspend fun setAntiUninstallEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.ANTI_UNINSTALL] = enabled
        }
    }

    suspend fun setReelsBlockerEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.REELS_BLOCKER] = enabled
        }
    }

    suspend fun setShortsBlockerEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.SHORTS_BLOCKER] = enabled
        }
    }

    suspend fun setTiktokBlockerEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.TIKTOK_BLOCKER] = enabled
        }
    }

    suspend fun setUsageEnforcerEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.USAGE_ENFORCER] = enabled
        }
    }

    suspend fun setStrictRedirectHome(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.STRICT_REDIRECT_HOME] = enabled
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
        private const val ITERATIONS = 12_000
        private const val KEY_LENGTH_BITS = 256

        fun isValidPinFormat(pin: String): Boolean {
            return pin.length == 4 && pin.all { it.isDigit() }
        }

        private fun pbkdf2Hash(pin: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return factory.generateSecret(spec).encoded
        }
    }
}
