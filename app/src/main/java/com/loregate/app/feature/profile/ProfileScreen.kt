package com.loregate.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.loregate.app.core.designsystem.CardColor
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.core.designsystem.SurfaceColor
import com.loregate.app.core.designsystem.TextColorDark
import com.loregate.app.core.designsystem.TextColorGray
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "Pengguna",
    val email: String = "user@email.com",
    val photoUrl: String = "https://api.dicebear.com/7.x/avataaars/svg?seed=Adventurer",
    val level: Long = 1L,
    val unlockedChaptersCount: Long = 0L,
    val showLogoutDialog: Boolean = false
)

sealed interface ProfileUiEvent {
    data object OnBackClicked : ProfileUiEvent
    data object OnStoryProgressClicked : ProfileUiEvent
    data object OnTravelHistoryClicked : ProfileUiEvent
    data object OnRequestLogout : ProfileUiEvent
    data object OnConfirmLogout : ProfileUiEvent
    data object OnDismissLogoutDialog : ProfileUiEvent
}

sealed interface ProfileUiEffect {
    data object NavigateBack : ProfileUiEffect
    data object NavigateToStoryProgress : ProfileUiEffect
    data object NavigateToStoryList : ProfileUiEffect
    data object NavigateToLogin : ProfileUiEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val showLogoutDialog = MutableStateFlow(false)

    private val userAppDataFlow = authRepository.currentUser.flatMapLatest { user ->
        if (user != null) userRepository.observeUserAppData(user.uid) else flowOf(null)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.currentUser,
        userAppDataFlow,
        showLogoutDialog
    ) { authUser, appData, showDialog ->
        ProfileUiState(
            displayName = authUser?.displayName?.ifBlank { null } ?: appData?.displayName?.ifBlank { null } ?: "Pengguna",
            email = authUser?.email ?: appData?.email ?: "user@email.com",
            photoUrl = authUser?.photoUrl ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=Adventurer",
            level = appData?.level ?: 1L,
            unlockedChaptersCount = appData?.unlockedChaptersCount ?: appData?.unlockedChapters?.size?.toLong() ?: 0L,
            showLogoutDialog = showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    private val _uiEffect = Channel<ProfileUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: ProfileUiEvent) {
        viewModelScope.launch {
            when (event) {
                ProfileUiEvent.OnBackClicked -> _uiEffect.send(ProfileUiEffect.NavigateBack)
                ProfileUiEvent.OnStoryProgressClicked -> _uiEffect.send(ProfileUiEffect.NavigateToStoryProgress)
                ProfileUiEvent.OnTravelHistoryClicked -> _uiEffect.send(ProfileUiEffect.NavigateToStoryList)
                ProfileUiEvent.OnRequestLogout -> showLogoutDialog.value = true
                ProfileUiEvent.OnDismissLogoutDialog -> showLogoutDialog.value = false
                ProfileUiEvent.OnConfirmLogout -> {
                    showLogoutDialog.value = false
                    authRepository.signOut()
                    _uiEffect.send(ProfileUiEffect.NavigateToLogin)
                }
            }
        }
    }
}

@Composable
fun ProfileRoute(
    onNavigateBack: () -> Unit,
    onNavigateToStoryProgress: () -> Unit,
    onNavigateToStoryList: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProfileUiEffect.NavigateBack -> onNavigateBack()
                ProfileUiEffect.NavigateToStoryProgress -> onNavigateToStoryProgress()
                ProfileUiEffect.NavigateToStoryList -> onNavigateToStoryList()
                ProfileUiEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit
) {
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(ProfileUiEvent.OnDismissLogoutDialog) },
            containerColor = Color.White,
            title = { Text("Log Out", fontWeight = FontWeight.Bold, color = TextColorDark) },
            text = { Text("Yakin ingin keluar dari akun ini?", color = TextColorGray) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ProfileUiEvent.OnConfirmLogout) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Keluar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(ProfileUiEvent.OnDismissLogoutDialog) },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextColorGray)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ProfileUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextColorDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        containerColor = SurfaceColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                modifier = Modifier.size(120.dp),
                color = CardColor
            ) {
                AsyncImage(
                    model = uiState.photoUrl,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = uiState.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextColorDark
            )
            Text(
                text = uiState.email,
                color = TextColorGray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge(label = "Level", value = "${uiState.level}")
                StatBadge(label = "Chapter Selesai", value = "${uiState.unlockedChaptersCount}")
            }

            Spacer(modifier = Modifier.height(48.dp))

            ProfileMenuCard(
                title = "Progress Cerita",
                subtitle = "Lihat cerita yang sudah terbuka",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = { onEvent(ProfileUiEvent.OnStoryProgressClicked) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileMenuCard(
                title = "Riwayat Perjalanan",
                subtitle = "Jejak lokasi yang pernah dikunjungi",
                icon = Icons.Default.History,
                onClick = { onEvent(ProfileUiEvent.OnTravelHistoryClicked) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileMenuCard(
                title = "Log Out",
                subtitle = "Keluar dari akun",
                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                iconTint = Color.Red,
                textColor = Color.Red,
                onClick = { onEvent(ProfileUiEvent.OnRequestLogout) }
            )
        }
    }
}

@Composable
fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = OrangePrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextColorGray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfileMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = OrangePrimary,
    textColor: Color = TextColorDark,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 16.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextColorGray
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextColorGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
