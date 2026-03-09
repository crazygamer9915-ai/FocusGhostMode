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
