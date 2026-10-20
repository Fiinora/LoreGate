package com.loregate.app.feature.auth

import androidx.compose.runtime.Composable
import platform.Foundation.NSLog

@Composable
actual fun GoogleSignInProvider(
    onGoogleSignInStarted: () -> Unit,
    onGoogleIdTokenReceived: (String) -> Unit,
    onGoogleSignInFailed: (String) -> Unit,
    content: @Composable (startGoogleSignIn: () -> Unit) -> Unit
) {
    content {
        onGoogleSignInStarted()
        onGoogleSignInFailed("Sign in with Google on iOS requires native GoogleSignIn configuration. Silakan gunakan Login dengan Email.")
    }
}

actual fun showPlatformToast(message: String) {
    NSLog("Toast: %s", message)
}
