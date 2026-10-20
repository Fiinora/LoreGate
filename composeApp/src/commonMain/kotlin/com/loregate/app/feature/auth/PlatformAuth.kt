package com.loregate.app.feature.auth

import androidx.compose.runtime.Composable

@Composable
expect fun GoogleSignInProvider(
    onGoogleSignInStarted: () -> Unit,
    onGoogleIdTokenReceived: (String) -> Unit,
    onGoogleSignInFailed: (String) -> Unit,
    content: @Composable (startGoogleSignIn: () -> Unit) -> Unit
)

expect fun showPlatformToast(message: String)
