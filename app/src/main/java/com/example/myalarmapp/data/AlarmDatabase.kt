package com.example.myalarmapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database for storing application alarms, tasks, habits, focus sessions, and security logs persistent on disk.
 */
@Database(
    entities = [Alarm::class, Task::class, Habit::class, HabitCompletion::class, FocusSession::class, SecurityLog::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusDao(): FocusDao
    abstract fun securityDao(): SecurityDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `dueDateTime` INTEGER,
                        `priority` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitId` INTEGER NOT NULL,
                        `completionDate` TEXT NOT NULL,
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_completions_habitId_completionDate` 
                    ON `habit_completions` (`habitId`, `completionDate`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `sessionType` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN isSmartAdaptive INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `security_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `imagePath` TEXT,
                        `latitude` REAL,
                        `longitude` REAL,
                        `details` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN dismissStyle TEXT NOT NULL DEFAULT 'simple'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeMinutes INTEGER NOT NULL DEFAULT 5")
            }
        }

        fun getDatabase(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
