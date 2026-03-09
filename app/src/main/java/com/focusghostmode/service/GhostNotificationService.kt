package com.focusghostmode.service

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.focusghostmode.data.repository.GhostRepository
import com.focusghostmode.utils.GhostPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * NotificationListenerService that silently captures all incoming notifications
 * while Ghost Mode is active. Does NOT suppress notifications visually —
 * suppression is done by enabling Do Not Disturb via GhostForegroundService.
 */
class GhostNotificationService : NotificationListenerService() {

    private val TAG = "GhostNotifService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var repository: GhostRepository
    private lateinit var prefs: GhostPreferences

    // Packages to ignore (system / ghost app itself)
    private val ignoredPackages = setOf(
        "com.focusghostmode",
        "com.focusghostmode.debug",
        "android",
        "com.android.systemui"
    )

    override fun onCreate() {
        super.onCreate()
        repository = GhostRepository.getInstance(applicationContext)
        prefs = GhostPreferences(applicationContext)
        Log.d(TAG, "GhostNotificationService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "GhostNotificationService destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Only capture when ghost mode is active
        if (!prefs.isGhostModeActive) return

        val pkg = sbn.packageName ?: return
        if (pkg in ignoredPackages) return

        // Skip group summaries and ongoing notifications (like music players)
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        val appName = getAppName(pkg)

        val sessionId = prefs.activeSessionId
        if (sessionId == -1L) return

        serviceScope.launch {
            try {
                repository.captureNotification(
                    sessionId = sessionId,
                    appPackage = pkg,
                    appName = appName,
                    title = title,
                    text = text
                )
                Log.d(TAG, "Captured notification from $appName: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // We don't need to handle removals for our use case
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
