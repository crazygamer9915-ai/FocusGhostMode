package com.focusghostmode.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.focusghostmode.utils.GhostPreferences

/**
 * Restores Ghost Mode foreground service if it was active before device restart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = GhostPreferences(context)
        if (prefs.isGhostModeActive) {
            ContextCompat.startForegroundService(context, GhostForegroundService.startIntent(context))
        }
    }
}
