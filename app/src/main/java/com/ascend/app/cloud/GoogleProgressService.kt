package com.ascend.app.cloud

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.ascend.app.OnboardingProfile
import com.ascend.app.R
import com.ascend.app.core.database.DailySummaryEntity
import com.ascend.app.core.database.NutritionTargetEntity
import com.ascend.app.core.database.UserProfileEntity
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

enum class GoogleSignInStage(val label: String) {
    ACCOUNT("Choose your Google account…"),
    AUTHENTICATION("Verifying Google sign-in…"),
    BACKUP("Google connected · Saving snapshot…"),
}

data class CloudBackupStatus(val email: String? = null, val saved: Boolean = false, val message: String = "No cloud backup confirmed.")

class GoogleProgressService(private val context: Context) {
    private val store = context.getSharedPreferences("cloud_backup_status", Context.MODE_PRIVATE)
    private val _stage = MutableStateFlow<GoogleSignInStage?>(null)
    val stage = _stage.asStateFlow()
    private val _backupStatus = MutableStateFlow(CloudBackupStatus(store.getString("email", null), store.getBoolean("saved", false), store.getString("message", null) ?: "No cloud backup confirmed."))
    val backupStatus = _backupStatus.asStateFlow()
    private val writeGeneration = AtomicLong()

    private fun reportStage(value: GoogleSignInStage?) {
        _stage.value = value
        // Stage names only: never log tokens, email, profile fields or Firebase exception bodies.
        Log.i("AscendAuth", "stage=${value?.name ?: "IDLE"}")
    }

    private fun recordBackup(email: String, saved: Boolean, message: String) {
        _backupStatus.value = CloudBackupStatus(email, saved, message)
        store.edit { putString("email", email); putBoolean("saved", saved); putString("message", message) }
    }

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

    suspend fun signInAndCreateBackup(activity: Activity, input: OnboardingProfile): Result<GoogleLinkResult> {
        return try {
            val user = authenticate(activity)
            val email = user.email ?: error("Google account did not supply an email address.")
            reportStage(GoogleSignInStage.BACKUP)
            val backup = confirmBackup {
                writeSnapshot(user.uid, email, "onboarding", input.toCloudMap(), awaitConfirmation = true)
            }
            Result.success(GoogleLinkResult(email, backup))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(failure)
        } finally {
            reportStage(null)
        }
    }

    suspend fun authenticate(activity: Activity): com.google.firebase.auth.FirebaseUser = try {
        authenticateInternal(activity)
    } finally { reportStage(null) }

    private suspend fun authenticateInternal(activity: Activity): com.google.firebase.auth.FirebaseUser {
        check(isConfigured) {
            "Google save needs the Firebase configuration file and OAuth web client ID. You can continue offline safely."
        }
        val request = GoogleSignInRequest.create(requireNotNull(webClientId))
        reportStage(GoogleSignInStage.ACCOUNT)
        val result = try {
            cloudDeadline(90_000, "Google account selection timed out (GOOGLE_PICKER_TIMEOUT). Retry when ready; your setup is unchanged.") {
                CredentialManager.create(context).getCredential(activity, request)
            }
        } catch (error: NoCredentialException) {
            throw IllegalStateException("Google could not start account selection (GOOGLE_NO_CREDENTIAL). Retry with updated Google Play services. If it continues, this build's package/signing certificate and OAuth configuration need verification; your local progress is safe.", error)
        } catch (error: GetCredentialCancellationException) {
            throw IllegalStateException("Google sign-in was canceled. Tap Sign in with Google when you're ready; your progress is unchanged.", error)
        } catch (error: GetCredentialProviderConfigurationException) {
            throw IllegalStateException("Google sign-in needs an available Google Play services credential provider. Update Play services or use a Google-enabled device; your local progress is safe.", error)
        } catch (error: GetCredentialException) {
            throw IllegalStateException("Google account selection failed (GOOGLE_PROVIDER_ERROR). Retry once. If it persists, share this code so the app's OAuth setup can be checked.", error)
        }
        val credential = result.credential
        check(credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google did not return a valid identity credential."
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        reportStage(GoogleSignInStage.AUTHENTICATION)
        val authResult = FirebaseAuth.getInstance()
            .signInWithCredential(GoogleAuthProvider.getCredential(googleCredential.idToken, null))
            .awaitCloudResult(20_000, "Firebase did not confirm Google sign-in within 20 seconds (GOOGLE_AUTH_TIMEOUT). Check your connection and retry. Your local progress is safe.")
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
        awaitConfirmation: Boolean = true,
    ) {
        if (profile.googleAccountEmail.isNullOrBlank()) return
        check(isConfigured) { "Cloud backup is not configured for this build (CLOUD_CONFIG_MISSING)." }
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
            ?.takeIf { !it.isAnonymous && it.email.equals(profile.googleAccountEmail, ignoreCase = true) && it.providerData.any { provider -> provider.providerId == GoogleAuthProvider.PROVIDER_ID } }
            ?: error("Reconnect Google to resume cloud backup (GOOGLE_SESSION_REQUIRED). Your local progress is safe.")
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
        if (awaitConfirmation) reportStage(GoogleSignInStage.BACKUP)
        try {
            writeSnapshot(user.uid, requireNotNull(user.email), "current", snapshot, awaitConfirmation)
        } finally {
            if (awaitConfirmation) reportStage(null)
        }
    }

    private suspend fun writeSnapshot(uid: String, email: String, document: String, snapshot: Map<String, Any>, awaitConfirmation: Boolean) {
        val generation = writeGeneration.incrementAndGet()
        val pending = "Google connected. Cloud backup not yet confirmed. Progress is saved on this device; queued uploads can sync when Firebase becomes available."
        recordBackup(email, false, pending)
        Log.i("AscendAuth", "backup=QUEUED document=$document")
        val task = FirebaseFirestore.getInstance().collection("players").document(uid)
            .collection("progress").document(document).set(snapshot + mapOf("syncedAt" to FieldValue.serverTimestamp()))
        task.addOnCompleteListener { completed ->
            if (generation != writeGeneration.get()) return@addOnCompleteListener
            recordBackup(email, completed.isSuccessful, if (completed.isSuccessful) "Progress snapshot saved to Firebase." else backupFailure(completed.exception))
            Log.i("AscendAuth", "backup=${if (completed.isSuccessful) "CONFIRMED" else "FAILED"} document=$document")
        }
        // Local actions enqueue a write and return immediately, even when Firebase is offline.
        if (!awaitConfirmation) return
        try {
            task.awaitCloudResult(12_000, "$pending (CLOUD_SAVE_TIMEOUT)")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val message = if (failure.message?.contains("CLOUD_SAVE_TIMEOUT") == true) failure.message!! else backupFailure(failure)
            if (generation == writeGeneration.get()) recordBackup(email, false, message)
            Log.w("AscendAuth", "backup=UNCONFIRMED document=$document")
            throw IllegalStateException(message, failure)
        }
    }

    private fun backupFailure(failure: Exception?): String {
        val code = (failure as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name ?: "FAILED"
        return "Google connected, but Firebase could not save the snapshot (CLOUD_$code). Your progress is still on this device. Retry from Profile → Account & privacy."
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

}
