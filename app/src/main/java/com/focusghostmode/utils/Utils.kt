package com.focusghostmode.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.focusghostmode.data.model.CapturedEvent
import com.focusghostmode.data.model.FocusSession
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Time formatting helpers
// ─────────────────────────────────────────────────────────────────────────────

object TimeUtils {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val fullFormat = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
    private val fileFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

    fun formatTime(millis: Long): String = timeFormat.format(Date(millis))
    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))
    fun formatFull(millis: Long): String = fullFormat.format(Date(millis))
    fun formatForFilename(millis: Long): String = fileFormat.format(Date(millis))

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "%dh %02dm %02ds".format(hours, minutes, seconds)
            minutes > 0 -> "%dm %02ds".format(minutes, seconds)
            else -> "${seconds}s"
        }
    }

    /** Returns true if two timestamps are on the same calendar day. */
    fun isSameDay(a: Long, b: Long): Boolean {
        val fmtA = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(a))
        val fmtB = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(b))
        return fmtA == fmtB
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Export utilities
// ─────────────────────────────────────────────────────────────────────────────

object ExportUtils {

    /**
     * Exports a session timeline as a CSV file and returns a share Intent.
     */
    fun exportToCsv(
        context: Context,
        session: FocusSession,
        events: List<CapturedEvent>
    ): Intent {
        val filename = "ghost_session_${TimeUtils.formatForFilename(session.startTime)}.csv"
        val file = File(context.getExternalFilesDir(null), filename)

        FileWriter(file).use { writer ->
            // Header
            writer.append("Timestamp,Type,App/Caller,Title/Name,Details\n")
            for (event in events) {
                val ts = TimeUtils.formatFull(event.timestamp)
                when (event.eventType) {
                    CapturedEvent.EventType.NOTIFICATION -> {
                        writer.append(
                            "${ts},Notification,${event.appName ?: ""},${event.notifTitle ?: ""},${event.notifText?.replace(",", ";") ?: ""}\n"
                        )
                    }
                    CapturedEvent.EventType.CALL -> {
                        val caller = event.callerName ?: event.callerNumber ?: ""
                        writer.append(
                            "${ts},Call,${event.callType?.name ?: ""},${caller},\n"
                        )
                    }
                }
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ghost Mode Session – ${TimeUtils.formatDate(session.startTime)}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Exports a session timeline as a formatted text report and returns a share Intent.
     */
    fun exportAsText(
        context: Context,
        session: FocusSession,
        events: List<CapturedEvent>
    ): Intent {
        val sb = StringBuilder()
        sb.appendLine("╔═══════════════════════════════════╗")
        sb.appendLine("      👻 FOCUS GHOST MODE REPORT     ")
        sb.appendLine("╚═══════════════════════════════════╝")
        sb.appendLine()
        sb.appendLine("📅 Date       : ${TimeUtils.formatDate(session.startTime)}")
        sb.appendLine("⏱  Duration   : ${session.durationFormatted()}")
        sb.appendLine("🔔 Notifs     : ${session.notificationCount}")
        sb.appendLine("📞 Calls      : ${session.callCount}")
        if (session.mostActiveApp != null) {
            sb.appendLine("🏆 Most Active: ${session.mostActiveApp}")
        }
        sb.appendLine()
        sb.appendLine("── TIMELINE ──────────────────────────")
        sb.appendLine()
        for (event in events) {
            val time = TimeUtils.formatTime(event.timestamp)
            val icon = if (event.eventType == CapturedEvent.EventType.NOTIFICATION) "🔔" else "📞"
            sb.appendLine("$time  $icon  ${event.summary()}")
        }
        sb.appendLine()
        sb.appendLine("Generated by Focus Ghost Mode")

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            putExtra(Intent.EXTRA_SUBJECT, "Ghost Mode Session – ${TimeUtils.formatDate(session.startTime)}")
        }
    }
}
