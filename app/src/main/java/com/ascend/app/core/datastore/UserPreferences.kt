package com.ascend.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ascendDataStore by preferencesDataStore("ascend_preferences")

data class AppPreferences(
    val onboardingComplete: Boolean = false,
    val morningNotifications: Boolean = true,
    val workoutNotifications: Boolean = true,
    val nutritionNotifications: Boolean = true,
    val eveningNotifications: Boolean = true,
    val mealSections: Set<String> = setOf("BREAKFAST", "LUNCH", "DINNER", "SNACKS"),
    val theme: String = "VOID",
    val calorieTolerancePercent: Int = 10,
    val healthConnectEnabled: Boolean = false,
)

class UserPreferences(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val morning = booleanPreferencesKey("notification_morning")
        val workout = booleanPreferencesKey("notification_workout")
        val nutrition = booleanPreferencesKey("notification_nutrition")
        val evening = booleanPreferencesKey("notification_evening")
        val mealSections = stringPreferencesKey("meal_sections")
        val theme = stringPreferencesKey("theme")
        val tolerance = intPreferencesKey("calorie_tolerance")
        val healthConnect = booleanPreferencesKey("health_connect")
    }

    val values: Flow<AppPreferences> = context.ascendDataStore.data.map { preferences ->
        AppPreferences(
            onboardingComplete = preferences[Keys.onboarding] ?: false,
            morningNotifications = preferences[Keys.morning] ?: true,
            workoutNotifications = preferences[Keys.workout] ?: true,
            nutritionNotifications = preferences[Keys.nutrition] ?: true,
            eveningNotifications = preferences[Keys.evening] ?: true,
            mealSections = preferences[Keys.mealSections]?.split(',')?.filter(String::isNotBlank)?.toSet()
                ?: setOf("BREAKFAST", "LUNCH", "DINNER", "SNACKS"),
            theme = preferences[Keys.theme] ?: "VOID",
            calorieTolerancePercent = preferences[Keys.tolerance] ?: 10,
            healthConnectEnabled = preferences[Keys.healthConnect] ?: false,
        )
    }

    suspend fun completeOnboarding() = context.ascendDataStore.edit { it[Keys.onboarding] = true }

    suspend fun setNotification(category: String, enabled: Boolean) = context.ascendDataStore.edit {
        when (category) {
            "morning" -> it[Keys.morning] = enabled
            "workout" -> it[Keys.workout] = enabled
            "nutrition" -> it[Keys.nutrition] = enabled
            "evening" -> it[Keys.evening] = enabled
        }
    }

    suspend fun setMealSections(sections: Set<String>) = context.ascendDataStore.edit {
        it[Keys.mealSections] = sections.joinToString(",")
    }

    suspend fun setCalorieTolerance(percent: Int) = context.ascendDataStore.edit {
        it[Keys.tolerance] = percent.coerceIn(5, 20)
    }

    suspend fun setHealthConnect(enabled: Boolean) = context.ascendDataStore.edit {
        it[Keys.healthConnect] = enabled
    }
}
