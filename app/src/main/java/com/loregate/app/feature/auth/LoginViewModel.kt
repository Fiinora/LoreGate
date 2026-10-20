package com.loregate.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loregate.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<LoginUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.OnEmailChanged -> {
                _uiState.update { it.copy(email = event.email) }
            }
            is LoginUiEvent.OnPasswordChanged -> {
                _uiState.update { it.copy(password = event.password) }
            }
            LoginUiEvent.OnTogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordHidden = !it.isPasswordHidden) }
            }
            LoginUiEvent.OnToggleSignUpMode -> {
                _uiState.update { it.copy(isSignUpMode = !it.isSignUpMode) }
            }
            LoginUiEvent.OnSubmitEmailAuth -> submitEmailAuth()
            LoginUiEvent.OnGoogleSignInStarted -> {
                _uiState.update { it.copy(isLoading = true) }
            }
            is LoginUiEvent.OnGoogleIdTokenReceived -> signInWithGoogle(event.idToken)
            is LoginUiEvent.OnGoogleSignInFailed -> {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginUiEffect.ShowToast(event.message))
            }
        }
    }

    private fun submitEmailAuth() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            sendEffect(LoginUiEffect.ShowToast("Please fill in all fields"))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (current.isSignUpMode) {
                authRepository.signUpWithEmail(current.email.trim(), current.password)
            } else {
                authRepository.signInWithEmail(current.email.trim(), current.password)
            }

            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = {
                    val welcomeMessage = if (current.isSignUpMode) "Akun Dibuat!" else "Selamat Datang!"
                    sendEffect(LoginUiEffect.ShowToast(welcomeMessage))
                    sendEffect(LoginUiEffect.NavigateToHome)
                },
                onFailure = { error ->
                    sendEffect(
                        LoginUiEffect.ShowToast(
                            error.message ?: if (current.isSignUpMode) "Sign up failed" else "Sign in failed"
                        )
                    )
                }
            )
        }
    }

    private fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.signInWithGoogleIdToken(idToken)
            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = {
                    sendEffect(LoginUiEffect.ShowToast("Selamat Datang!"))
                    sendEffect(LoginUiEffect.NavigateToHome)
                },
                onFailure = { error ->
                    sendEffect(LoginUiEffect.ShowToast(error.message ?: "Google sign in failed"))
                }
            )
        }
    }

    private fun sendEffect(effect: LoginUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
