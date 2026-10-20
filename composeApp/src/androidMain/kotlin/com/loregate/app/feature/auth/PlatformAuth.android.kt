package com.loregate.app.feature.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.loregate.app.LoreGateApp
import com.loregate.app.R

@Composable
actual fun GoogleSignInProvider(
    onGoogleSignInStarted: () -> Unit,
    onGoogleIdTokenReceived: (String) -> Unit,
    onGoogleSignInFailed: (String) -> Unit,
    content: @Composable (startGoogleSignIn: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val oneTapClient = remember { Identity.getSignInClient(context) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                onGoogleIdTokenReceived(idToken)
            } else {
                onGoogleSignInFailed("Google sign-in failed: token null")
            }
        } catch (e: Exception) {
            onGoogleSignInFailed(e.message ?: "An error occurred during Google sign-in")
        }
    }

    content {
        onGoogleSignInStarted()
        val clientId = try {
            context.getString(R.string.web_client_id)
        } catch (e: Exception) {
            ""
        }
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    googleSignInLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    onGoogleSignInFailed("Couldn't start sign in flow.")
                }
            }
            .addOnFailureListener { e ->
                onGoogleSignInFailed(e.message ?: "Google One-Tap failed.")
            }
    }
}

actual fun showPlatformToast(message: String) {
    LoreGateApp.instance?.let { ctx ->
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
    }
}
