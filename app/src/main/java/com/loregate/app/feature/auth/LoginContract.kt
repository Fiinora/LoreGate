package com.loregate.app.feature.auth

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordHidden: Boolean = true,
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface LoginUiEvent {
    data class OnEmailChanged(val email: String) : LoginUiEvent
    data class OnPasswordChanged(val password: String) : LoginUiEvent
    data object OnTogglePasswordVisibility : LoginUiEvent
    data object OnToggleSignUpMode : LoginUiEvent
    data object OnSubmitEmailAuth : LoginUiEvent
    data object OnGoogleSignInStarted : LoginUiEvent
    data class OnGoogleIdTokenReceived(val idToken: String) : LoginUiEvent
    data class OnGoogleSignInFailed(val message: String) : LoginUiEvent
}

sealed interface LoginUiEffect {
    data class ShowToast(val message: String) : LoginUiEffect
    data object NavigateToHome : LoginUiEffect
}
