package com.example.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class LimitedFeature(
    val title: String,
    val sessionDurationSeconds: Int,
    val cooldownSeconds: Int,
    val maxActionsPerSession: Int
) {
    IMAGE_STUDIO(
        title = "AI Image Studio",
        sessionDurationSeconds = 300, // 5 minutes session window
        cooldownSeconds = 20,         // 20s cooldown between creations
        maxActionsPerSession = 10
    ),
    GEMINI_CHAT(
        title = "Gemini AI Advisor",
        sessionDurationSeconds = 480, // 8 minutes session window
        cooldownSeconds = 12,         // 12s cooldown between prompts
        maxActionsPerSession = 20
    )
}

data class FeatureLimitState(
    val feature: LimitedFeature,
    val remainingSessionSeconds: Int,
    val isSessionActive: Boolean,
    val remainingCooldownSeconds: Int,
    val isCooldownActive: Boolean,
    val actionsUsed: Int,
    val maxActions: Int
) {
    val canPerformAction: Boolean
        get() = isSessionActive && !isCooldownActive && actionsUsed < maxActions

    val formattedSessionTime: String
        get() {
            val mins = remainingSessionSeconds / 60
            val secs = remainingSessionSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }

    val sessionProgressFraction: Float
        get() = (remainingSessionSeconds.toFloat() / feature.sessionDurationSeconds.toFloat()).coerceIn(0f, 1f)

    val cooldownProgressFraction: Float
        get() = if (feature.cooldownSeconds > 0) {
            (remainingCooldownSeconds.toFloat() / feature.cooldownSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

class UsageLimitTracker(
    private val feature: LimitedFeature,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var sessionTimerJob: Job? = null
    private var cooldownTimerJob: Job? = null

    private val _state = MutableStateFlow(
        FeatureLimitState(
            feature = feature,
            remainingSessionSeconds = feature.sessionDurationSeconds,
            isSessionActive = true,
            remainingCooldownSeconds = 0,
            isCooldownActive = false,
            actionsUsed = 0,
            maxActions = feature.maxActionsPerSession
        )
    )
    val state: StateFlow<FeatureLimitState> = _state.asStateFlow()

    init {
        startSessionCountdown()
    }

    private fun startSessionCountdown() {
        sessionTimerJob?.cancel()
        sessionTimerJob = scope.launch {
            while (isActive && _state.value.remainingSessionSeconds > 0) {
                delay(1000)
                val newRemaining = _state.value.remainingSessionSeconds - 1
                _state.value = _state.value.copy(
                    remainingSessionSeconds = newRemaining,
                    isSessionActive = newRemaining > 0
                )
            }
        }
    }

    fun triggerActionConsumed(): Boolean {
        if (!_state.value.canPerformAction) return false

        val updatedActions = _state.value.actionsUsed + 1
        _state.value = _state.value.copy(
            actionsUsed = updatedActions,
            remainingCooldownSeconds = feature.cooldownSeconds,
            isCooldownActive = feature.cooldownSeconds > 0
        )

        startCooldownCountdown()
        return true
    }

    private fun startCooldownCountdown() {
        cooldownTimerJob?.cancel()
        cooldownTimerJob = scope.launch {
            while (isActive && _state.value.remainingCooldownSeconds > 0) {
                delay(1000)
                val newCooldown = _state.value.remainingCooldownSeconds - 1
                _state.value = _state.value.copy(
                    remainingCooldownSeconds = newCooldown,
                    isCooldownActive = newCooldown > 0
                )
            }
        }
    }

    fun refreshSession() {
        sessionTimerJob?.cancel()
        cooldownTimerJob?.cancel()

        _state.value = FeatureLimitState(
            feature = feature,
            remainingSessionSeconds = feature.sessionDurationSeconds,
            isSessionActive = true,
            remainingCooldownSeconds = 0,
            isCooldownActive = false,
            actionsUsed = 0,
            maxActions = feature.maxActionsPerSession
        )
        startSessionCountdown()
    }

    fun addBonusMinutes(minutes: Int = 3) {
        val extra = minutes * 60
        val newTime = _state.value.remainingSessionSeconds + extra
        _state.value = _state.value.copy(
            remainingSessionSeconds = newTime,
            isSessionActive = true
        )
        if (sessionTimerJob == null || !sessionTimerJob!!.isActive) {
            startSessionCountdown()
        }
    }
}
