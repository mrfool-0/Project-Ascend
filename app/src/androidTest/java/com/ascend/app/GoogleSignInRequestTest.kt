package com.ascend.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ascend.app.cloud.GoogleSignInRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleSignInRequestTest {
    @Test fun explicitButtonUsesExactlyOneInteractiveGoogleOption() {
        val client = "test-web-client.apps.googleusercontent.com"
        val request = GoogleSignInRequest.create(client)
        assertEquals(1, request.credentialOptions.size)
        val option = request.credentialOptions.single()
        assertTrue(option is GetSignInWithGoogleOption)
        assertEquals(client, (option as GetSignInWithGoogleOption).serverClientId)
    }

    @Test fun unconfiguredBuildCannotLaunchAnInvalidOAuthRequest() {
        listOf("", "CONFIGURE_IN_FIREBASE", "not-a-client").forEach {
            assertTrue(runCatching { GoogleSignInRequest.create(it) }.isFailure)
        }
    }
}
