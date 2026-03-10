package com.focusghostmode

import android.app.Application
import android.util.Log

class GhostApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("GhostApp", "Focus Ghost Mode started")
    }
}
