package com.focusghostmode.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single Ghost Mode focus session.
 */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,           // epoch millis
    val endTime: Long? = null,     // null if still active
    val notificationCount: Int = 0,
    val callCount: Int = 0,
    val mostActiveApp: String? = null,
    val isActive: Boolean = true
) {
    /** Duration in milliseconds. Returns 0 if session has not ended. */
    fun durationMillis(): Long {
        return if (endTime != null) endTime - startTime else 0L
    }

    /** Duration formatted as "Xh Ym" string. */
    fun durationFormatted(): String {
        val millis = durationMillis()
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
