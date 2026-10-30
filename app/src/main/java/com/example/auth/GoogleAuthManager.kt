package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoogleUserProfile(
    val email: String,
    val displayName: String,
    val isSignedIn: Boolean,
    val isDriveConnected: Boolean,
    val driveFolderName: String = "PaperTrail Vault",
    val driveTotalBytes: Long = 15L * 1024 * 1024 * 1024, // 15 GB
    val driveUsedBytes: Long = 4L * 1024 * 1024 * 1024 + 250L * 1024 * 1024, // 4.25 GB
    val autoSyncEnabled: Boolean = true
) {
    val usedPercentage: Float
        get() = (driveUsedBytes.toFloat() / driveTotalBytes.toFloat()).coerceIn(0f, 1f)

    val formattedUsed: String
        get() = String.format("%.2f GB", driveUsedBytes / (1024.0 * 1024.0 * 1024.0))

    val formattedTotal: String
        get() = String.format("%.0f GB", driveTotalBytes / (1024.0 * 1024.0 * 1024.0))
}

class GoogleAuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("papertrail_google_auth", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_EMAIL = "gemini44270@gmail.com"
        const val DEFAULT_NAME = "Gemini Account"
        private const val KEY_SIGNED_IN = "is_signed_in"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_NAME = "user_name"
        private const val KEY_DRIVE_CONNECTED = "is_drive_connected"
        private const val KEY_AUTO_SYNC = "auto_sync_drive"
    }

    private val _currentUser = MutableStateFlow(loadProfile())
    val currentUser: StateFlow<GoogleUserProfile> = _currentUser.asStateFlow()

    private fun loadProfile(): GoogleUserProfile {
        val isSignedIn = prefs.getBoolean(KEY_SIGNED_IN, true) // Default logged in for seamless trial
        val email = prefs.getString(KEY_EMAIL, DEFAULT_EMAIL) ?: DEFAULT_EMAIL
        val name = prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
        val isDriveConnected = prefs.getBoolean(KEY_DRIVE_CONNECTED, true)
        val autoSync = prefs.getBoolean(KEY_AUTO_SYNC, true)

        return GoogleUserProfile(
            email = email,
            displayName = name,
            isSignedIn = isSignedIn,
            isDriveConnected = isDriveConnected && isSignedIn,
            autoSyncEnabled = autoSync
        )
    }

    fun signIn(email: String = DEFAULT_EMAIL, displayName: String = DEFAULT_NAME) {
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, displayName)
            .putBoolean(KEY_DRIVE_CONNECTED, true)
            .apply()

        _currentUser.value = loadProfile()
    }

    fun signOut() {
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, false)
            .putBoolean(KEY_DRIVE_CONNECTED, false)
            .apply()

        _currentUser.value = _currentUser.value.copy(
            isSignedIn = false,
            isDriveConnected = false
        )
    }

    fun setAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        _currentUser.value = _currentUser.value.copy(autoSyncEnabled = enabled)
    }

    fun toggleDriveConnection() {
        val newState = !_currentUser.value.isDriveConnected
        prefs.edit().putBoolean(KEY_DRIVE_CONNECTED, newState).apply()
        _currentUser.value = _currentUser.value.copy(isDriveConnected = newState)
    }
}
