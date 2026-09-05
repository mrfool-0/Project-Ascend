package com.ascend.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class, NutritionTargetEntity::class, FoodEntity::class,
        SavedMealEntity::class, SavedMealItemEntity::class, FoodLogEntity::class,
        WeightEntryEntity::class, BodyMeasurementEntity::class, ExerciseEntity::class,
        WorkoutTemplateEntity::class, WorkoutExerciseEntity::class, WorkoutSessionEntity::class,
        WorkoutSetEntity::class, HabitEntity::class, HabitCompletionEntity::class,
        QuestEntity::class, QuestCompletionEntity::class, XpTransactionEntity::class,
        AchievementEntity::class, UnlockedAchievementEntity::class, DailySummaryEntity::class,
        CoachMessageEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun dao(): AscendDao

    companion object {
        @Volatile private var instance: AscendDatabase? = null

        fun create(context: Context): AscendDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AscendDatabase::class.java, "ascend.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN focusAreas TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN injuries TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN injuryNotes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN workoutFrequency INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN workoutDays TEXT NOT NULL DEFAULT '1,3,5'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN futureVision TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN coreReason TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN minimumPromise TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN googleAccountEmail TEXT")
                db.execSQL("ALTER TABLE food ADD COLUMN barcode TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS coach_message (id TEXT NOT NULL, role TEXT NOT NULL, message TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_coach_message_createdAt ON coach_message(createdAt)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val existing = mutableSetOf<String>()
                db.query("PRAGMA table_info(`food`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) existing += cursor.getString(nameIndex)
                }
                if ("saturatedFatGrams" !in existing) db.execSQL("ALTER TABLE food ADD COLUMN saturatedFatGrams REAL")
                if ("sugarGrams" !in existing) db.execSQL("ALTER TABLE food ADD COLUMN sugarGrams REAL")
                if ("sodiumMg" !in existing) db.execSQL("ALTER TABLE food ADD COLUMN sodiumMg REAL")
            }
        }
    }
}
