package com.focusghostmode

import android.app.Application
import android.util.Log

/**
 * Application class for Focus Ghost Mode.
 * Initializes global singletons at startup.
 */
class GhostApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("GhostApp", "Focus Ghost Mode started")
    }
}
var savedNotifVolume: Int
    get() = prefs.getInt(KEY_NOTIF_VOL, 5)
    set(value) = prefs.edit().putInt(KEY_NOTIF_VOL, value).apply()

var savedRingVolume: Int
    get() = prefs.getInt(KEY_RING_VOL, 5)
    set(value) = prefs.edit().putInt(KEY_RING_VOL, value).apply()
