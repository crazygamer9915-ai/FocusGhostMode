package com.focusghostmode.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.focusghostmode.data.model.CapturedEvent
import com.focusghostmode.data.model.FocusSession

/**
 * Room database for Focus Ghost Mode.
 * Stores focus sessions and all captured events.
 */
@Database(
    entities = [FocusSession::class, CapturedEvent::class],
    version = 1,
    exportSchema = false
)
abstract class GhostDatabase : RoomDatabase() {

    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun capturedEventDao(): CapturedEventDao

    companion object {
        private const val DATABASE_NAME = "ghost_mode.db"

        @Volatile
        private var INSTANCE: GhostDatabase? = null

        fun getInstance(context: Context): GhostDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GhostDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
