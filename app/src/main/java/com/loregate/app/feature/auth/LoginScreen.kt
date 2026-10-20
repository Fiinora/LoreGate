package com.loregate.app.feature.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.loregate.app.R
import com.loregate.app.core.designsystem.BackgroundColor
import com.loregate.app.core.designsystem.BorderColor
import com.loregate.app.core.designsystem.GrayText
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.core.designsystem.OrangeSecondary
import com.loregate.app.core.designsystem.TextColor

@Composable
fun LoginRoute(
    onNavigateToHome: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val oneTapClient = remember { Identity.getSignInClient(context) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                viewModel.onEvent(LoginUiEvent.OnGoogleIdTokenReceived(idToken))
            } else {
                viewModel.onEvent(LoginUiEvent.OnGoogleSignInFailed("Google sign-in failed."))
            }
        } catch (e: Exception) {
            viewModel.onEvent(LoginUiEvent.OnGoogleSignInFailed(e.message ?: "An error occurred"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LoginUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                LoginUiEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onStartGoogleSignIn = {
            viewModel.onEvent(LoginUiEvent.OnGoogleSignInStarted)
            val signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(context.getString(R.string.web_client_id))
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
                        viewModel.onEvent(
                            LoginUiEvent.OnGoogleSignInFailed("Couldn't start sign in flow.")
                        )
                    }
                }
                .addOnFailureListener { e ->
                    viewModel.onEvent(
                        LoginUiEvent.OnGoogleSignInFailed(
                            e.localizedMessage ?: "Google sign-in failed"
                        )
                    )
                }
        }
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onStartGoogleSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.fixed_logo),
                contentDescription = "Logo LoreGate",
                modifier = Modifier.size(128.dp)
            )
            Text(
                text = "LoreGate",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (uiState.isSignUpMode) "Create your account" else "Selamat Datang!",
                fontSize = 16.sp,
                color = GrayText
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onEvent(LoginUiEvent.OnEmailChanged(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onEvent(LoginUiEvent.OnPasswordChanged(it)) },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (uiState.isPasswordHidden) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                trailingIcon = {
                    IconButton(onClick = { onEvent(LoginUiEvent.OnTogglePasswordVisibility) }) {
                        val icon = if (uiState.isPasswordHidden) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        }
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            val gradient = Brush.horizontalGradient(listOf(OrangePrimary, OrangeSecondary))
            Button(
                onClick = { onEvent(LoginUiEvent.OnSubmitEmailAuth) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradient, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isSignUpMode) "Sign Up" else "Login",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.6f), color = BorderColor)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onStartGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        color = TextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.isSignUpMode) "Sudah punya akun? " else "Belum punya akun? ",
                    color = GrayText
                )
                Text(
                    text = if (uiState.isSignUpMode) "Masuk" else "Daftar",
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) {
                        onEvent(LoginUiEvent.OnToggleSignUpMode)
                    }
                )
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(color = OrangePrimary)
        }
    }
}
