package com.ascend.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ascend.app.cloud.awaitCloudResult
import com.ascend.app.cloud.cloudDeadline
import com.ascend.app.cloud.confirmBackup
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudOperationTest {
    @Test fun stalledFirebaseAuthEndsWithRecoverableError() = runBlocking {
        val source = TaskCompletionSource<String>()
        val result = runCatching { source.task.awaitCloudResult(40, "GOOGLE_AUTH_TIMEOUT") }
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("GOOGLE_AUTH_TIMEOUT", result.exceptionOrNull()?.message)
        // Firebase may complete after the UI timed out. It must not crash or resume twice.
        source.setResult("late-account")
    }

    @Test fun successfulVoidWriteIsNotMistakenForTimeout() = runBlocking {
        val source = TaskCompletionSource<Void?>()
        source.setResult(null)
        val result = confirmBackup { source.task.awaitCloudResult(500, "TIMEOUT") }
        assertTrue(result.saved)
    }

    @Test fun stalledBackupDoesNotBecomeFailedGoogleLogin() = runBlocking {
        val source = TaskCompletionSource<Void?>()
        val result = confirmBackup { source.task.awaitCloudResult(40, "CLOUD_SAVE_TIMEOUT") }
        assertFalse(result.saved)
        assertEquals("CLOUD_SAVE_TIMEOUT", result.message)
        source.setResult(null)
    }

    @Test fun rejectedBackupHasActionableErrorNotFalseSuccess() = runBlocking {
        val source = TaskCompletionSource<Void?>()
        source.setException(IllegalStateException("CLOUD_PERMISSION_DENIED"))
        val result = confirmBackup { source.task.awaitCloudResult(500, "TIMEOUT") }
        assertFalse(result.saved)
        assertEquals("CLOUD_PERMISSION_DENIED", result.message)
    }

    @Test fun externalCancellationIsNeverConvertedIntoBackupFailure() = runBlocking {
        val source = TaskCompletionSource<Void?>()
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            confirmBackup { source.task.awaitCloudResult(5_000, "TIMEOUT") }
        }
        request.cancel()
        request.join()
        assertTrue(request.isCancelled)
        source.setException(IllegalStateException("late failure"))
    }

    @Test fun deadlineRespectsParentCancellation() = runBlocking {
        val failure = runCatching {
            withTimeout(40) { cloudDeadline(5_000, "INNER_TIMEOUT") { awaitCancellation() } }
        }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }

    @Test fun completedAuthTaskReturnsItsActualIdentity() = runBlocking {
        val source = TaskCompletionSource<String>()
        source.setResult("fixture-uid")
        assertEquals("fixture-uid", source.task.awaitCloudResult(500, "TIMEOUT"))
    }
}
