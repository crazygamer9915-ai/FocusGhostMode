package com.focusghostmode.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences wrapper for persistent app state.
 */
class GhostPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ghost_prefs", Context.MODE_PRIVATE)

    var isGhostModeActive: Boolean
        get() = prefs.getBoolean(KEY_GHOST_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_GHOST_ACTIVE, value).apply()

    var activeSessionId: Long
        get() = prefs.getLong(KEY_SESSION_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_SESSION_ID, value).apply()

    var sessionStartTime: Long
        get() = prefs.getLong(KEY_START_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_START_TIME, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var lastEndedSessionId: Long
        get() = prefs.getLong(KEY_LAST_SESSION_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_SESSION_ID, value).apply()

    companion object {
        private const val KEY_GHOST_ACTIVE = "ghost_active"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
        private const val KEY_NOTIF_VOL = "notif_vol"
        private const val KEY_RING_VOL = "ring_vol"
    }
}
