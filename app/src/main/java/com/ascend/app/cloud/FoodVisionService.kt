package com.ascend.app.cloud

import android.content.Context
import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import org.json.JSONObject
import kotlin.math.roundToInt

data class FoodVisionResult(
    val name: String,
    val servingGrams: Double,
    val servingDescription: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val saturatedFat: Double,
    val sodiumMg: Double,
    val confidence: Double,
    val notes: String,
)

class FoodVisionService(private val context: Context) {
    val isConfigured: Boolean
        get() = FirebaseApp.initializeApp(context) != null

    suspend fun analyze(bitmap: Bitmap): Result<FoodVisionResult> = runCatching {
        check(isConfigured) {
            "Food Vision needs Firebase AI Logic. Add app/google-services.json and enable the Gemini Developer API in Firebase."
        }
        val schema = Schema.obj(
            mapOf(
                "food_name" to Schema.string("Short name for the complete visible meal or food"),
                "serving_grams" to Schema.double("Estimated grams in the entire visible edible portion", minimum = 1.0, maximum = 5000.0),
                "serving_description" to Schema.string("Human description such as one bowl or two slices"),
                "calories_kcal" to Schema.double("Estimated calories for the entire visible portion", minimum = 0.0, maximum = 10000.0),
                "protein_g" to Schema.double("Estimated protein grams", minimum = 0.0, maximum = 1000.0),
                "carbohydrate_g" to Schema.double("Estimated carbohydrate grams", minimum = 0.0, maximum = 2000.0),
                "fat_g" to Schema.double("Estimated fat grams", minimum = 0.0, maximum = 1000.0),
                "fiber_g" to Schema.double("Estimated fiber grams", minimum = 0.0, maximum = 200.0),
                "sugar_g" to Schema.double("Estimated total sugar grams", minimum = 0.0, maximum = 1000.0),
                "saturated_fat_g" to Schema.double("Estimated saturated fat grams", minimum = 0.0, maximum = 500.0),
                "sodium_mg" to Schema.double("Estimated sodium milligrams", minimum = 0.0, maximum = 50000.0),
                "confidence" to Schema.double("Confidence from 0 to 1 based on food and portion visibility", minimum = 0.0, maximum = 1.0),
                "notes" to Schema.string("One short uncertainty note mentioning hidden ingredients or portion ambiguity"),
            ),
        )
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.7-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                maxOutputTokens = 500
            },
        )
        val image = bitmap.downscaled(maxSide = 1536)
        val prompt = content {
            image(image)
            text(
                """
                Analyze only the food visibly present in this image. Identify the complete dish or meal,
                estimate the entire visible edible portion, and estimate its nutrition. Account for likely
                cooking oil, sauces, and mixed ingredients, but do not invent items that are not reasonably
                visible. If no food is visible, set food_name to NO_FOOD. Return nutrition for the full visible
                portion, not per 100 grams. This is an approximate logging aid, never a medical measurement.
                """.trimIndent(),
            )
        }
        val responseText = model.generateContent(prompt).text ?: error("Food Vision returned no estimate.")
        parse(responseText)
    }

    private fun parse(json: String): FoodVisionResult {
        val value = JSONObject(json)
        val name = value.getString("food_name").trim()
        check(name.isNotBlank() && name != "NO_FOOD") { "No recognizable food was found. Try a brighter, closer photo of the full plate." }
        return FoodVisionResult(
            name = name.take(80),
            servingGrams = value.getDouble("serving_grams").bounded(1.0, 5000.0),
            servingDescription = value.getString("serving_description").trim().take(100),
            calories = value.getDouble("calories_kcal").bounded(0.0, 10000.0),
            protein = value.getDouble("protein_g").bounded(0.0, 1000.0),
            carbs = value.getDouble("carbohydrate_g").bounded(0.0, 2000.0),
            fat = value.getDouble("fat_g").bounded(0.0, 1000.0),
            fiber = value.getDouble("fiber_g").bounded(0.0, 200.0),
            sugar = value.getDouble("sugar_g").bounded(0.0, 1000.0),
            saturatedFat = value.getDouble("saturated_fat_g").bounded(0.0, 500.0),
            sodiumMg = value.getDouble("sodium_mg").bounded(0.0, 50000.0),
            confidence = value.getDouble("confidence").bounded(0.0, 1.0),
            notes = value.getString("notes").trim().take(180),
        )
    }

    private fun Bitmap.downscaled(maxSide: Int): Bitmap {
        val largest = maxOf(width, height)
        if (largest <= maxSide) return this
        val ratio = maxSide.toDouble() / largest
        return Bitmap.createScaledBitmap(this, (width * ratio).roundToInt(), (height * ratio).roundToInt(), true)
    }

    private fun Double.bounded(min: Double, max: Double): Double = coerceIn(min, max)
}
