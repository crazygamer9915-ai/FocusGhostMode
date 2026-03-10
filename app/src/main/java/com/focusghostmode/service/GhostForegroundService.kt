package com.focusghostmode.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.focusghostmode.R
import com.focusghostmode.data.repository.GhostRepository
import com.focusghostmode.ui.home.MainActivity
import com.focusghostmode.utils.GhostPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps Ghost Mode alive.
 * Manages Do Not Disturb policy and shows a persistent notification indicator.
 */
class GhostForegroundService : Service() {

    private val TAG = "GhostFgService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var repository: GhostRepository
    private lateinit var prefs: GhostPreferences
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        repository = GhostRepository.getInstance(applicationContext)
        prefs = GhostPreferences(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        Log.d(TAG, "GhostForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startGhostMode()
            ACTION_STOP -> stopGhostMode()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        restoreAudio()
        Log.d(TAG, "GhostForegroundService destroyed")
    }

    // ── Ghost Mode Lifecycle ──────────────────────────────────────────────────

    private fun startGhostMode() {
        serviceScope.launch {
            val sessionId = repository.startSession()
            prefs.activeSessionId = sessionId
            prefs.isGhostModeActive = true
            prefs.sessionStartTime = System.currentTimeMillis()
        }

        // Enable Do Not Disturb if we have permission
        enableDoNotDisturb()

        // Start foreground with ghost indicator notification
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        Log.d(TAG, "Ghost Mode started")
    }

    private fun stopGhostMode() {
        val sessionId = prefs.activeSessionId
        if (sessionId != -1L) {
            serviceScope.launch {
                repository.endSession(sessionId)
            }
        }

        prefs.isGhostModeActive = false
        prefs.activeSessionId = -1L

        restoreAudio()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Ghost Mode stopped")
    }

    // ── Do Not Disturb ────────────────────────────────────────────────────────

    private fun enableDoNotDisturb() {
    if (notificationManager.isNotificationPolicyAccessGranted) {
        // Suppress ALL interruptions - no sounds, no popups, no vibrations
        notificationManager.setInterruptionFilter(
            NotificationManager.INTERRUPTION_FILTER_NONE
        )
        Log.d(TAG, "DND enabled - full silence")
    } else {
        // Fallback: mute ringer AND notifications via AudioManager
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        
        // Also suppress notification sounds via stream volumes
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        Log.d(TAG, "Audio streams muted (DND not granted)")
    }
    
    // Save previous volume levels to restore later
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    prefs.savedNotifVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
    prefs.savedRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
}

    private fun restoreAudio() {
    if (notificationManager.isNotificationPolicyAccessGranted) {
        notificationManager.setInterruptionFilter(
            NotificationManager.INTERRUPTION_FILTER_ALL
        )
    } else {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        // Restore saved volumes
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, prefs.savedNotifVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, prefs.savedRingVolume, 0)
    }
    Log.d(TAG, "Audio restored")
}

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildForegroundNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, GhostForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("👻 Ghost Mode Active")
            .setContentText("Silently capturing events. Tap to view.")
            .setSmallIcon(R.drawable.ic_ghost)
            .setContentIntent(tapIntent)
            .addAction(R.drawable.ic_stop, "End Ghost Mode", stopIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ghost Mode Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows Ghost Mode active indicator"
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.focusghostmode.ACTION_START_GHOST"
        const val ACTION_STOP = "com.focusghostmode.ACTION_STOP_GHOST"
        const val CHANNEL_ID = "ghost_mode_channel"
        const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context) = Intent(context, GhostForegroundService::class.java).apply {
            action = ACTION_START
        }

        fun stopIntent(context: Context) = Intent(context, GhostForegroundService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
