package com.focusghostmode.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single captured event (notification or call) during a Ghost Mode session.
 */
@Entity(
    tableName = "captured_events",
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class CapturedEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val eventType: EventType,
    val timestamp: Long,           // epoch millis

    // Notification fields
    val appPackage: String? = null,
    val appName: String? = null,
    val notifTitle: String? = null,
    val notifText: String? = null,

    // Call fields
    val callerNumber: String? = null,
    val callerName: String? = null,
    val callType: CallType? = null
) {
    enum class EventType { NOTIFICATION, CALL }
    enum class CallType { INCOMING, MISSED, REJECTED }

    /** Returns a human-readable summary for the timeline. */
    fun summary(): String = when (eventType) {
        EventType.NOTIFICATION -> buildString {
            append(appName ?: appPackage ?: "Unknown App")
            if (!notifTitle.isNullOrBlank()) append(": $notifTitle")
            if (!notifText.isNullOrBlank() && notifText != notifTitle) {
                append(" – ${notifText.take(60)}${if (notifText.length > 60) "…" else ""}")
            }
        }
        EventType.CALL -> buildString {
            val caller = callerName?.takeIf { it.isNotBlank() } ?: callerNumber ?: "Unknown"
            append(when (callType) {
                CallType.MISSED -> "Missed call from $caller"
                CallType.INCOMING -> "Incoming call from $caller"
                CallType.REJECTED -> "Rejected call from $caller"
                null -> "Call from $caller"
            })
        }
    }
}
