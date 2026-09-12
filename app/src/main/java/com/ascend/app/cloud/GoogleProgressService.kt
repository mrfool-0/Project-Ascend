package com.ascend.app.cloud

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import com.ascend.app.OnboardingProfile
import com.ascend.app.R
import com.ascend.app.core.database.DailySummaryEntity
import com.ascend.app.core.database.NutritionTargetEntity
import com.ascend.app.core.database.UserProfileEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleProgressService(private val context: Context) {
    // Optional in offline-only builds. The wildcard keep rule preserves it during shrinking.
    @get:android.annotation.SuppressLint("DiscouragedApi")
    private val webClientId: String?
        get() {
            val explicit = context.getString(R.string.google_oauth_web_client_id).takeUnless { it == "CONFIGURE_IN_FIREBASE" || it.isBlank() }
            val generatedId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            return explicit ?: generatedId.takeIf { it != 0 }?.let(context::getString)?.takeIf { it.endsWith(".apps.googleusercontent.com") }
        }
    val isConfigured: Boolean
        get() = webClientId != null &&
            FirebaseApp.initializeApp(context) != null

    suspend fun signInAndCreateBackup(activity: Activity, input: OnboardingProfile): Result<String> = runCatching {
        val user = authenticate(activity)
        FirebaseFirestore.getInstance().collection("players").document(user.uid)
            .collection("progress").document("onboarding")
            .set(input.toCloudMap() + mapOf("syncedAt" to FieldValue.serverTimestamp()))
            .awaitResult()
        user.email ?: error("Google account did not supply an email address.")
    }

    suspend fun authenticate(activity: Activity): com.google.firebase.auth.FirebaseUser {
        check(isConfigured) {
            "Google save needs the Firebase configuration file and OAuth web client ID. You can continue offline safely."
        }
        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(requireNotNull(webClientId))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
        val result = try {
            CredentialManager.create(context).getCredential(activity, request)
        } catch (error: NoCredentialException) {
            throw IllegalStateException("No eligible Google account was found. Add an account to this device and try again.", error)
        } catch (error: GetCredentialProviderConfigurationException) {
            throw IllegalStateException("Google sign-in needs an available Google Play services credential provider. Update Play services or use a Google-enabled device; your local progress is safe.", error)
        }
        val credential = result.credential
        check(credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google did not return a valid identity credential."
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val authResult = FirebaseAuth.getInstance()
            .signInWithCredential(GoogleAuthProvider.getCredential(googleCredential.idToken, null))
            .awaitResult()
        val user = authResult.user ?: error("Google sign-in completed without a user account.")
        check(!user.isAnonymous && user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            "ASCEND requires a verified Google-authenticated Firebase session for cloud progress."
        }
        return user
    }

    suspend fun syncProgress(
        profile: UserProfileEntity,
        target: NutritionTargetEntity,
        lifetimeXp: Int,
        summaries: List<DailySummaryEntity>,
    ) {
        if (!isConfigured) return
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
            ?.takeIf { !it.isAnonymous && it.providerData.any { provider -> provider.providerId == GoogleAuthProvider.PROVIDER_ID } }
            ?: return
        val snapshot = mapOf(
            "displayName" to profile.displayName,
            "objective" to profile.objective,
            "focusAreas" to profile.focusAreas.split(',').filter(String::isNotBlank),
            "workoutDays" to profile.workoutDays.split(',').filter(String::isNotBlank),
            "currentWeightKg" to profile.currentWeightKg,
            "targetWeightKg" to profile.targetWeightKg,
            "calorieTarget" to target.calories,
            "proteinTarget" to target.proteinGrams,
            "lifetimeXp" to lifetimeXp,
            "recentDailyProgress" to summaries.takeLast(31).map {
                mapOf(
                    "date" to it.localDate, "completion" to it.completionPercent,
                    "calories" to it.calories, "protein" to it.proteinGrams,
                    "water" to it.waterMl, "workout" to it.workoutCompleted,
                )
            },
            "syncedAt" to FieldValue.serverTimestamp(),
        )
        FirebaseFirestore.getInstance().collection("players").document(user.uid)
            .collection("progress").document("current").set(snapshot).awaitResult()
    }

    private fun OnboardingProfile.toCloudMap(): Map<String, Any> = mapOf(
        "displayName" to name,
        "birthDate" to birthDate.toString(),
        "heightCm" to heightCm,
        "weightKg" to weightKg,
        "targetWeightKg" to targetWeightKg,
        "objective" to objective.name,
        "activity" to activity.name,
        "experience" to experience.name,
        "equipment" to equipment.name,
        "diet" to diet.name,
        "focusAreas" to focusAreas.map { it.name },
        "injuries" to injuries.map { it.name },
        "injuryNotes" to injuryNotes,
        "workoutFrequency" to workoutFrequency,
        "trainingSplit" to trainingSplit.name,
        "workoutDays" to workoutDays.map { it.name },
        "futureVision" to futureVision,
        "coreReason" to coreReason,
        "minimumPromise" to minimumPromise,
    )

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(task.exception ?: IllegalStateException("Cloud operation failed"))
        }
    }
}
