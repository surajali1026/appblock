package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FocusMusicGenerator
import com.example.ai.LyriaModelOption
import com.example.audio.AudioPlaybackState
import com.example.audio.FocusAudioPlayer
import com.example.data.local.AppLimitEntity
import com.example.data.local.BlockEventEntity
import com.example.data.local.FocusGuardDatabase
import com.example.data.local.FocusTrackEntity
import com.example.security.PinSecurityManager
import com.example.security.PinVerifyResult
import com.example.security.SecuritySettings
import com.example.service.UsageEnforcerForegroundService
import com.example.usage.InstalledAppInfo
import com.example.usage.UsageStatsHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PermissionHealthState(
    val accessibilityEnabled: Boolean = false,
    val usageStatsGranted: Boolean = false,
    val notificationsGranted: Boolean = true,
    val enforcerServiceRunning: Boolean = false
) {
    val activeCount: Int
        get() = listOf(
            accessibilityEnabled,
            usageStatsGranted,
            notificationsGranted,
            enforcerServiceRunning
        ).count { it }
}

sealed interface MusicGenUiState {
    data object Idle : MusicGenUiState
    data class Loading(val modelLabel: String) : MusicGenUiState
    data class Success(val trackTitle: String, val notes: String) : MusicGenUiState
    data class Error(val message: String) : MusicGenUiState
}

enum class PinActionPurpose {
    SETUP_PIN,
    CHANGE_PIN,
    UNINSTALL_APP,
    TOGGLE_ANTI_UNINSTALL
}

data class PinDialogUiState(
    val isVisible: Boolean = false,
    val purpose: PinActionPurpose = PinActionPurpose.UNINSTALL_APP,
    val errorMessage: String? = null,
    val isVerifying: Boolean = false
)

class FocusGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext
    private val database = FocusGuardDatabase.getInstance(appContext)
    private val dao = database.focusGuardDao()
    private val pinManager = PinSecurityManager(appContext)
    private val musicGenerator = FocusMusicGenerator(appContext)
    private val audioPlayer = FocusAudioPlayer()

    val securitySettings: StateFlow<SecuritySettings> = pinManager.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SecuritySettings()
        )

    val appLimits: StateFlow<List<AppLimitEntity>> = dao.observeAllLimits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val recentBlockEvents: StateFlow<List<BlockEventEntity>> = dao.observeRecentBlockEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val todayBlockCount: StateFlow<Int> = dao.observeTodayBlockCount(UsageStatsHelper.todayDateKey())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val focusTracks: StateFlow<List<FocusTrackEntity>> = dao.observeAllFocusTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val audioPlaybackState: StateFlow<AudioPlaybackState> = audioPlayer.playbackState

    private val _permissionHealth = MutableStateFlow(PermissionHealthState())
    val permissionHealth: StateFlow<PermissionHealthState> = _permissionHealth.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _dailyUsageMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val dailyUsageMap: StateFlow<Map<String, Long>> = _dailyUsageMap.asStateFlow()

    private val _musicGenState = MutableStateFlow<MusicGenUiState>(MusicGenUiState.Idle)
    val musicGenState: StateFlow<MusicGenUiState> = _musicGenState.asStateFlow()

    private val _pinDialogState = MutableStateFlow(PinDialogUiState())
    val pinDialogState: StateFlow<PinDialogUiState> = _pinDialogState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            UsageEnforcerForegroundService.isRunning.collect { running ->
                _permissionHealth.update { it.copy(enforcerServiceRunning = running) }
            }
        }
        refreshSystemHealthAndUsage()
    }

    fun refreshSystemHealthAndUsage() {
        viewModelScope.launch {
            val accessibility = UsageStatsHelper.isAccessibilityServiceEnabled(appContext)
            val usageStats = UsageStatsHelper.hasUsageStatsPermission(appContext)
            val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    appContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            val running = UsageEnforcerForegroundService.isRunning.value

            _permissionHealth.value = PermissionHealthState(
                accessibilityEnabled = accessibility,
                usageStatsGranted = usageStats,
                notificationsGranted = notifications,
                enforcerServiceRunning = running
            )

            val (apps, usage) = withContext(Dispatchers.IO) {
                UsageStatsHelper.getInstalledUserApps(appContext) to
                    UsageStatsHelper.getDailyUsageMap(appContext)
            }
            _installedApps.value = apps
            _dailyUsageMap.value = usage
        }
    }

    fun toggleEnforcerService() {
        if (UsageEnforcerForegroundService.isRunning.value) {
            UsageEnforcerForegroundService.stop(appContext)
            _permissionHealth.update { it.copy(enforcerServiceRunning = false) }
        } else {
            if (!UsageStatsHelper.hasUsageStatsPermission(appContext)) {
                _statusBanner.value = "Grant Usage Access permission first to run the Enforcer Service."
                return
            }
            UsageEnforcerForegroundService.start(appContext)
            _permissionHealth.update { it.copy(enforcerServiceRunning = true) }
        }
    }

    fun setReelsBlocker(enabled: Boolean) {
        viewModelScope.launch { pinManager.setReelsBlockerEnabled(enabled) }
    }

    fun setShortsBlocker(enabled: Boolean) {
        viewModelScope.launch { pinManager.setShortsBlockerEnabled(enabled) }
    }

    fun setTiktokBlocker(enabled: Boolean) {
        viewModelScope.launch { pinManager.setTiktokBlockerEnabled(enabled) }
    }

    fun setUsageEnforcerEnabled(enabled: Boolean) {
        viewModelScope.launch { pinManager.setUsageEnforcerEnabled(enabled) }
    }

    fun setStrictRedirectHome(enabled: Boolean) {
        viewModelScope.launch { pinManager.setStrictRedirectHome(enabled) }
    }

    fun saveAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertLimit(
                AppLimitEntity(
                    packageName = packageName,
                    appName = appName,
                    dailyLimitMinutes = limitMinutes.coerceIn(1, 360),
                    isEnabled = true
                )
            )
            _statusBanner.value = "Set ${limitMinutes}m daily limit for $appName."
        }
    }

    fun toggleAppLimitEnabled(rule: AppLimitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertLimit(rule.copy(isEnabled = !rule.isEnabled, updatedAtMs = System.currentTimeMillis()))
        }
    }

    fun deleteAppLimit(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteLimit(packageName)
        }
    }

    fun requestPinAction(purpose: PinActionPurpose) {
        val currentSettings = securitySettings.value
        val resolvedPurpose = if (!currentSettings.isPinConfigured &&
            purpose != PinActionPurpose.SETUP_PIN
        ) {
            if (purpose == PinActionPurpose.UNINSTALL_APP) {
                _statusBanner.value = "Configure your 4-digit Security PIN first to authorize uninstallation."
                PinActionPurpose.SETUP_PIN
            } else {
                PinActionPurpose.SETUP_PIN
            }
        } else {
            purpose
        }
        _pinDialogState.value = PinDialogUiState(
            isVisible = true,
            purpose = resolvedPurpose,
            errorMessage = null,
            isVerifying = false
        )
    }

    fun dismissPinDialog() {
        _pinDialogState.value = PinDialogUiState(isVisible = false)
    }

    fun submitNewPin(pin: String, confirmPin: String) {
        if (!PinSecurityManager.isValidPinFormat(pin)) {
            _pinDialogState.update { it.copy(errorMessage = "PIN must be exactly 4 numeric digits.") }
            return
        }
        if (pin != confirmPin) {
            _pinDialogState.update { it.copy(errorMessage = "PINs do not match.") }
            return
        }
        viewModelScope.launch {
            _pinDialogState.update { it.copy(isVerifying = true, errorMessage = null) }
            val saved = pinManager.setPin(pin)
            if (saved) {
                _pinDialogState.value = PinDialogUiState(isVisible = false)
                _statusBanner.value = "4-digit Security PIN saved securely."
            } else {
                _pinDialogState.update { it.copy(isVerifying = false, errorMessage = "Unable to save PIN.") }
            }
        }
    }

    fun verifyPinAndExecute(pin: String, newPin: String = "") {
        val purpose = _pinDialogState.value.purpose
        viewModelScope.launch {
            _pinDialogState.update { it.copy(isVerifying = true, errorMessage = null) }
            when (val result = pinManager.verifyPin(pin)) {
                is PinVerifyResult.Success -> {
                    when (purpose) {
                        PinActionPurpose.UNINSTALL_APP -> {
                            pinManager.authorizeUninstallWindow(35_000L)
                            _pinDialogState.value = PinDialogUiState(isVisible = false)
                            launchSystemUninstallIntent()
                        }

                        PinActionPurpose.TOGGLE_ANTI_UNINSTALL -> {
                            val nextValue = !securitySettings.value.antiUninstallEnabled
                            pinManager.setAntiUninstallEnabled(nextValue)
                            _pinDialogState.value = PinDialogUiState(isVisible = false)
                            _statusBanner.value = if (nextValue) {
                                "Hardened Anti-Uninstall Guard enabled."
                            } else {
                                "Anti-Uninstall Guard paused."
                            }
                        }

                        PinActionPurpose.CHANGE_PIN -> {
                            if (!PinSecurityManager.isValidPinFormat(newPin)) {
                                _pinDialogState.update {
                                    it.copy(
                                        isVerifying = false,
                                        errorMessage = "New PIN must be 4 numeric digits."
                                    )
                                }
                                return@launch
                            }
                            pinManager.setPin(newPin)
                            _pinDialogState.value = PinDialogUiState(isVisible = false)
                            _statusBanner.value = "Security PIN updated."
                        }

                        PinActionPurpose.SETUP_PIN -> {
                            _pinDialogState.value = PinDialogUiState(isVisible = false)
                        }
                    }
                }

                is PinVerifyResult.Invalid -> {
                    _pinDialogState.update {
                        it.copy(
                            isVerifying = false,
                            errorMessage = "Incorrect PIN. ${result.attemptsRemaining} attempts remaining."
                        )
                    }
                }

                is PinVerifyResult.LockedOut -> {
                    _pinDialogState.update {
                        it.copy(
                            isVerifying = false,
                            errorMessage = "Too many wrong attempts. Try again in ${result.remainingSeconds}s."
                        )
                    }
                }

                PinVerifyResult.NotConfigured -> {
                    _pinDialogState.update {
                        it.copy(
                            isVerifying = false,
                            purpose = PinActionPurpose.SETUP_PIN,
                            errorMessage = "Set a 4-digit PIN first."
                        )
                    }
                }
            }
        }
    }

    private fun launchSystemUninstallIntent() {
        val packageUri = Uri.parse("package:${appContext.packageName}")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(uninstallIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch system uninstall dialog", e)
            _statusBanner.value = "Unable to open system uninstaller: ${e.localizedMessage}"
        }
    }

    fun generateFocusMusic(title: String, prompt: String, option: LyriaModelOption) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) {
            _musicGenState.value = MusicGenUiState.Error("Enter a musical mood or focus prompt.")
            return
        }
        val trackTitle = title.trim().ifEmpty {
            cleanPrompt.take(36).replaceFirstChar { it.uppercase() }
        }

        viewModelScope.launch {
            _musicGenState.value = MusicGenUiState.Loading(option.label)
            try {
                val generated = musicGenerator.generateFocusTrack(cleanPrompt, option)
                val entity = FocusTrackEntity(
                    title = trackTitle,
                    prompt = cleanPrompt,
                    modelUsed = generated.modelId,
                    filePath = generated.filePath,
                    mimeType = generated.mimeType,
                    durationLabel = generated.durationLabel,
                    lyricsOrNotes = generated.lyricsOrNotes
                )
                val id = withContext(Dispatchers.IO) { dao.insertFocusTrack(entity) }
                _musicGenState.value = MusicGenUiState.Success(
                    trackTitle = trackTitle,
                    notes = generated.lyricsOrNotes
                )
                audioPlayer.playTrack(id, generated.filePath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Music generation error", e)
                _musicGenState.value = MusicGenUiState.Error(
                    e.localizedMessage ?: "Failed to generate focus music via Firebase AI Logic."
                )
            }
        }
    }

    fun toggleTrackPlayback(track: FocusTrackEntity) {
        audioPlayer.togglePlay(track.id, track.filePath)
    }

    fun toggleLooping() {
        audioPlayer.toggleLooping()
    }

    fun seekAudioTo(positionMs: Int) {
        audioPlayer.seekTo(positionMs)
    }

    fun deleteTrack(track: FocusTrackEntity) {
        if (audioPlayer.playbackState.value.activeTrackId == track.id) {
            audioPlayer.stopAndRelease()
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(track.filePath).delete() }
            dao.deleteFocusTrack(track.id)
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun resetMusicGenState() {
        _musicGenState.value = MusicGenUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAndRelease()
    }

    companion object {
        private const val TAG = "FocusGuardViewModel"
    }
}
