package com.focusghostmode.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.focusghostmode.data.repository.GhostRepository
import com.focusghostmode.utils.GhostPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GhostRepository.getInstance(application)
    private val prefs = GhostPreferences(application)

    // Ghost mode on/off
    private val _isGhostModeActive = MutableLiveData(prefs.isGhostModeActive)
    val isGhostModeActive: LiveData<Boolean> = _isGhostModeActive

    // Session elapsed time in millis (ticks every second)
    private val _elapsedMillis = MutableLiveData(0L)
    val elapsedMillis: LiveData<Long> = _elapsedMillis

    // Session ID after just ending ghost mode (to open timeline)
    private val _justEndedSessionId = MutableLiveData<Long?>()
    val justEndedSessionId: LiveData<Long?> = _justEndedSessionId

    init {
        // If ghost mode is already active (e.g. after config change), start the timer
        if (prefs.isGhostModeActive) {
            startTimer()
        }
    }

    fun refreshState() {
        _isGhostModeActive.value = prefs.isGhostModeActive
        if (prefs.isGhostModeActive) startTimer()
    }

    fun setGhostModeActive(active: Boolean) {
        _isGhostModeActive.value = active
        if (active) {
            startTimer()
        }
    }

    fun onSessionEnded() {
        val sessionId = prefs.lastEndedSessionId
        if (sessionId != -1L) {
            _justEndedSessionId.value = sessionId
        }
    }

    fun clearJustEndedSession() {
        _justEndedSessionId.value = null
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (prefs.isGhostModeActive) {
                val elapsed = System.currentTimeMillis() - prefs.sessionStartTime
                _elapsedMillis.postValue(elapsed)
                delay(1000)
            }
            _elapsedMillis.postValue(0L)
        }
    }
}
