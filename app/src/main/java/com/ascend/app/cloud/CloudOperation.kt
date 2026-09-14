package com.ascend.app.cloud

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Unlike a coroutine cancellation, a network deadline is a recoverable UI error. */
internal suspend fun <T> cloudDeadline(timeoutMs: Long, message: String, operation: suspend () -> T): T {
    // Box the value: Firebase Task<Void> legitimately succeeds with null.
    val result = withTimeoutOrNull(timeoutMs) { Result.success(operation()) }
        ?: throw IllegalStateException(message)
    return result.getOrThrow()
}

internal suspend fun <T> Task<T>.awaitCloudResult(timeoutMs: Long, message: String): T =
    cloudDeadline(timeoutMs, message) {
        suspendCancellableCoroutine { continuation ->
            // Do not depend on the Activity/main thread to deliver completion. Firebase Tasks
            // cannot always be cancelled; a late callback must never resume a cancelled waiter.
            addOnCompleteListener(Executor { it.run() }) { task ->
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(task.exception ?: IllegalStateException("Cloud operation cancelled."))
            }
        }
    }

data class CloudBackupResult(val saved: Boolean, val message: String)
data class GoogleLinkResult(val email: String, val backup: CloudBackupResult)

/** A failed backup must not turn a successful Google login into a failed login. */
internal suspend fun confirmBackup(write: suspend () -> Unit): CloudBackupResult = try {
    write()
    CloudBackupResult(true, "Google connected. Progress snapshot saved to Firebase.")
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    CloudBackupResult(false, failure.message ?: "Cloud backup was not confirmed. Your progress remains on this device.")
}
