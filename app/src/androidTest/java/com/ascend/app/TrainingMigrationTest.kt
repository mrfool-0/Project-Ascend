package com.ascend.app

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.app.core.database.AscendDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TrainingMigrationTest {
    @Test fun versionFourUpgradePreservesPlayerAndRecordedWorkout() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migration-test-" + UUID.randomUUID() + ".db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        val schema = JSONObject(instrumentation.context.assets.open("com.ascend.app.core.database.AscendDatabase/4.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { sqlite ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                fun sql(value: String) = value.replace("$" + "{TABLE_NAME}", entity.getString("tableName"))
                sqlite.execSQL(sql(entity.getString("createSql")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) sqlite.execSQL(sql(indices.getJSONObject(j).getString("createSql")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i))
            sqlite.execSQL("INSERT INTO user_profile VALUES (1,'TEST','2000-01-01',175,75,75,'MALE','METRIC','MUSCLE_GAIN','MODERATE','BEGINNER','DUMBBELLS','BALANCED','EVENING','FULL_BODY,CHEST,ARMS','NONE','',3,'AUTO','1,3,5','','','',NULL,'2026-09-07',0)")
            sqlite.execSQL("INSERT INTO workout_template VALUES ('old','Full Body',0,100,45,0)")
            sqlite.execSQL("INSERT INTO exercise VALUES ('row','Row','Back','DUMBBELLS')")
            sqlite.execSQL("INSERT INTO workout_session VALUES ('session','old','2026-09-08',0,1,'kept')")
            sqlite.execSQL("INSERT INTO workout_set VALUES ('set','session','row',1,10,8,1,1)")
            sqlite.version = 4
        }
        val db = Room.databaseBuilder(context, AscendDatabase::class.java, name).addMigrations(AscendDatabase.MIGRATION_4_5, AscendDatabase.MIGRATION_5_6).build()
        try {
            val profile = requireNotNull(db.dao().observeProfile().first())
            assertEquals("TEST", profile.displayName)
            assertEquals("FULL_BODY", profile.focusAreas)
            assertEquals(45, profile.sessionMinutes)
            assertTrue(requireNotNull(db.dao().templateById("old")).active)
            assertEquals("Row", db.dao().observeExercises().first().single().name)
            assertEquals("kept", db.dao().workoutSession("session")?.notes)
            assertEquals(8, db.dao().observeWorkoutSets("session").first().single().reps)
            assertTrue(db.dao().observeDayOverrides().first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name) // Isolated UUID fixture only; never the application's database.
        }
    }
}
