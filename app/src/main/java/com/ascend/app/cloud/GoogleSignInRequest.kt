package com.ascend.app.cloud

import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption

/** Both explicit Google buttons must use the interactive button flow, not bottom-sheet discovery. */
internal object GoogleSignInRequest {
    fun create(webClientId: String): GetCredentialRequest {
        require(webClientId.isNotBlank() && webClientId.endsWith(".apps.googleusercontent.com")) {
            "Google sign-in is missing a valid Web OAuth client ID."
        }
        return GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
    }
}
