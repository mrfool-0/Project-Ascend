package com.ascend.app

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.app.core.database.*
import com.ascend.app.ui.components.QuestLaunchOverlay
import com.ascend.app.ui.screens.WorkoutScreen
import com.ascend.app.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkoutRegressionTest {
    @get:Rule val ui = createComposeRule()
    private val template = WorkoutTemplateEntity("qa_template", "Full body A", 0, 150, 40)
    private val session = WorkoutSessionEntity("qa_session", template.id, "2026-09-08", 0L)
    private val exercise = ExerciseEntity("qa_row", "Cable row", "Back", "GYM")
    private val link = WorkoutExerciseEntity("qa_link", template.id, exercise.id, 0, 1, 8, 12)
    private val detail = WorkoutExerciseDetail(link, exercise)
    private val initialSet = WorkoutSetEntity("qa_set", session.id, exercise.id, 1, 0.0, 0, false)
    private val launch = WorkoutLaunch(session, template, listOf(detail), listOf(initialSet))

    @Test fun zeroLoadSetIsSavedAndLockedUntilExplicitlyUnlocked() {
        var set by mutableStateOf(initialSet)
        ui.setContent {
            AscendTheme {
                WorkoutScreen(launch, listOf(set), {}, { _, weight, reps, done -> set = set.copy(weightKg = weight, reps = reps, completed = done) }, {}, {}, {})
            }
        }
        ui.onNodeWithContentDescription("Complete set 1").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Set 1 repetitions").performTextInput("12")
        ui.onNodeWithContentDescription("Complete set 1").performClick()
        ui.runOnIdle { assertEquals(0.0, set.weightKg, 0.0); assertEquals(12, set.reps); assertTrue(set.completed) }
        ui.onNodeWithContentDescription("Set 1 weight in kilograms").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Set 1 repetitions").assertIsNotEnabled()
        ui.onNodeWithText("COMPLETE QUEST").performScrollTo().assertIsEnabled()
        capture("workout-logging")
        ui.onNodeWithContentDescription("Unlock set 1 to edit").performScrollTo().performClick()
        ui.onNodeWithContentDescription("Set 1 repetitions").assertIsEnabled()
    }

    @Test fun exerciseRemovalRequiresConfirmationAndCancelPreservesIt() {
        var removed = false
        ui.setContent { AscendTheme { WorkoutScreen(launch, listOf(initialSet), {}, { _, _, _, _ -> }, {}, { removed = true }, {}) } }
        ui.onNodeWithContentDescription("Remove Cable row").performClick()
        ui.onNodeWithText("Remove exercise?").assertIsDisplayed()
        ui.onNodeWithText("Keep exercise").performClick()
        ui.runOnIdle { assertFalse(removed) }
        ui.onNodeWithContentDescription("Remove Cable row").performClick()
        ui.onNodeWithText("Remove", substring = false).performClick()
        ui.runOnIdle { assertTrue(removed) }
    }

    @Test fun reducedMotionQuestEntryCompletesWithoutArtificialDelay() {
        var finished by mutableStateOf(false)
        ui.setContent {
            AscendTheme {
                CompositionLocalProvider(LocalMotionEnabled provides false) {
                    if (!finished) QuestLaunchOverlay("Full body A") { finished = true }
                }
            }
        }
        ui.waitForIdle()
        ui.runOnIdle { assertTrue(finished) }
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
