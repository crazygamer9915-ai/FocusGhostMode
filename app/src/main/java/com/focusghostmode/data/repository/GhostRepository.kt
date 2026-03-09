package com.focusghostmode.data.repository

import android.content.Context
import com.focusghostmode.data.db.GhostDatabase
import com.focusghostmode.data.model.CapturedEvent
import com.focusghostmode.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all Ghost Mode data.
 * All UI layers interact through this repository.
 */
class GhostRepository(context: Context) {

    private val db = GhostDatabase.getInstance(context)
    private val sessionDao = db.focusSessionDao()
    private val eventDao = db.capturedEventDao()

    // ── Sessions ──────────────────────────────────────────────────────────────

    suspend fun startSession(): Long {
        val session = FocusSession(startTime = System.currentTimeMillis())
        return sessionDao.insertSession(session)
    }

    suspend fun getActiveSession(): FocusSession? = sessionDao.getActiveSession()

    suspend fun endSession(sessionId: Long) {
        val notifCount = eventDao.getNotificationCount(sessionId)
        val callCount = eventDao.getCallCount(sessionId)
        val mostActiveApp = eventDao.getMostActiveApp(sessionId)
        sessionDao.endSession(
            id = sessionId,
            endTime = System.currentTimeMillis(),
            notifCount = notifCount,
            callCount = callCount,
            mostActiveApp = mostActiveApp
        )
    }

    fun getAllSessionsFlow(): Flow<List<FocusSession>> = sessionDao.getAllSessionsFlow()

    suspend fun getAllSessions(): List<FocusSession> = sessionDao.getAllSessions()

    suspend fun getSessionById(id: Long): FocusSession? = sessionDao.getSessionById(id)

    suspend fun deleteSession(id: Long) {
        eventDao.deleteEventsForSession(id)
        sessionDao.deleteSession(id)
    }

    // ── Events ────────────────────────────────────────────────────────────────

    suspend fun captureNotification(
        sessionId: Long,
        appPackage: String?,
        appName: String?,
        title: String?,
        text: String?
    ) {
        val event = CapturedEvent(
            sessionId = sessionId,
            eventType = CapturedEvent.EventType.NOTIFICATION,
            timestamp = System.currentTimeMillis(),
            appPackage = appPackage,
            appName = appName,
            notifTitle = title,
            notifText = text
        )
        eventDao.insertEvent(event)
    }

    suspend fun captureCall(
        sessionId: Long,
        number: String?,
        name: String?,
        callType: CapturedEvent.CallType
    ) {
        val event = CapturedEvent(
            sessionId = sessionId,
            eventType = CapturedEvent.EventType.CALL,
            timestamp = System.currentTimeMillis(),
            callerNumber = number,
            callerName = name,
            callType = callType
        )
        eventDao.insertEvent(event)
    }

    suspend fun getEventsForSession(sessionId: Long): List<CapturedEvent> =
        eventDao.getEventsForSession(sessionId)

    fun getEventsForSessionFlow(sessionId: Long): Flow<List<CapturedEvent>> =
        eventDao.getEventsForSessionFlow(sessionId)

    companion object {
        @Volatile private var INSTANCE: GhostRepository? = null

        fun getInstance(context: Context): GhostRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GhostRepository(context).also { INSTANCE = it }
            }
        }
    }
}
