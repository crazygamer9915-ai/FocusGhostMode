package com.focusghostmode.data.db

import androidx.room.*
import com.focusghostmode.data.model.CapturedEvent
import com.focusghostmode.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// FocusSession DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface FocusSessionDao {

    @Insert
    suspend fun insertSession(session: FocusSession): Long

    @Update
    suspend fun updateSession(session: FocusSession)

    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): FocusSession?

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<FocusSession>

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): FocusSession?

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("UPDATE focus_sessions SET isActive = 0, endTime = :endTime, notificationCount = :notifCount, callCount = :callCount, mostActiveApp = :mostActiveApp WHERE id = :id")
    suspend fun endSession(id: Long, endTime: Long, notifCount: Int, callCount: Int, mostActiveApp: String?)
}

// ─────────────────────────────────────────────────────────────────────────────
// CapturedEvent DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface CapturedEventDao {

    @Insert
    suspend fun insertEvent(event: CapturedEvent): Long

    @Query("SELECT * FROM captured_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSession(sessionId: Long): List<CapturedEvent>

    @Query("SELECT * FROM captured_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEventsForSessionFlow(sessionId: Long): Flow<List<CapturedEvent>>

    @Query("SELECT COUNT(*) FROM captured_events WHERE sessionId = :sessionId AND eventType = 'NOTIFICATION'")
    suspend fun getNotificationCount(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM captured_events WHERE sessionId = :sessionId AND eventType = 'CALL'")
    suspend fun getCallCount(sessionId: Long): Int

    @Query("""
        SELECT appName
        FROM captured_events 
        WHERE sessionId = :sessionId AND eventType = 'NOTIFICATION' 
        GROUP BY appName 
        ORDER BY COUNT(*) DESC 
        LIMIT 1
    """)
    suspend fun getMostActiveApp(sessionId: Long): String?
   
    @Query("DELETE FROM captured_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsForSession(sessionId: Long)
}
