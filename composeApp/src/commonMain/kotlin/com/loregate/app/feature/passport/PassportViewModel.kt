package com.loregate.app.feature.passport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loregate.app.domain.model.HeritageStamp
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.PassportRepository
import com.loregate.app.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PassportViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val passportRepository: PassportRepository
) : ViewModel() {

    private val selectedStamp = MutableStateFlow<HeritageStamp?>(null)

    private val userProfileFlow = authRepository.currentUser.flatMapLatest { user ->
        if (user != null) userRepository.observeUserAppData(user.uid) else flowOf(null)
    }

    private val stampsFlow = authRepository.currentUser.flatMapLatest { user ->
        passportRepository.observeUserStamps(user?.uid)
    }

    val uiState: StateFlow<PassportUiState> = combine(
        authRepository.currentUser,
        userProfileFlow,
        stampsFlow,
        selectedStamp
    ) { authUser, appData, stamps, selected ->
        val name = authUser?.displayName?.ifBlank { null }
            ?: appData?.displayName?.ifBlank { null }
            ?: "Pengelana Waktu"
        val level = appData?.level ?: 1L
        val unlockedCount = stamps.count { it.isUnlocked }

        PassportUiState(
            isLoading = false,
            explorerName = name,
            explorerLevel = level,
            stamps = stamps,
            selectedStamp = selected,
            unlockedCount = unlockedCount,
            totalCount = stamps.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PassportUiState(isLoading = true)
    )

    private val _uiEffect = Channel<PassportUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: PassportUiEvent) {
        viewModelScope.launch {
            when (event) {
                is PassportUiEvent.OnStampClicked -> {
                    selectedStamp.value = event.stamp
                }
                PassportUiEvent.OnDismissStampDetail -> {
                    selectedStamp.value = null
                }
                is PassportUiEvent.OnNavigateToSiteOnMap -> {
                    selectedStamp.value = null
                    _uiEffect.send(PassportUiEffect.NavigateToMapWithSite(event.siteId))
                }
                PassportUiEvent.OnBackClicked -> {
                    _uiEffect.send(PassportUiEffect.NavigateBack)
                }
            }
        }
    }
}
