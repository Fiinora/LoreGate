package com.loregate.app.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.loregate.app.R
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val userPhotoUrl: String? = null
)

sealed interface HomeUiEvent {
    data object OnStoryListClicked : HomeUiEvent
    data object OnFreeRoamMapClicked : HomeUiEvent
    data object OnProfileClicked : HomeUiEvent
}

sealed interface HomeUiEffect {
    data object NavigateToStoryList : HomeUiEffect
    data object NavigateToMap : HomeUiEffect
    data object NavigateToProfile : HomeUiEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = authRepository.currentUser
        .map { user -> HomeUiState(userPhotoUrl = user?.photoUrl) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(userPhotoUrl = authRepository.getCurrentUser()?.photoUrl)
        )

    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: HomeUiEvent) {
        viewModelScope.launch {
            when (event) {
                HomeUiEvent.OnStoryListClicked -> _uiEffect.send(HomeUiEffect.NavigateToStoryList)
                HomeUiEvent.OnFreeRoamMapClicked -> _uiEffect.send(HomeUiEffect.NavigateToMap)
                HomeUiEvent.OnProfileClicked -> _uiEffect.send(HomeUiEffect.NavigateToProfile)
            }
        }
    }
}

@Composable
fun HomeRoute(
    onNavigateToStoryList: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HomeUiEffect.NavigateToStoryList -> onNavigateToStoryList()
                HomeUiEffect.NavigateToMap -> onNavigateToMap()
                HomeUiEffect.NavigateToProfile -> onNavigateToProfile()
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit
) {
    val bgImages = remember {
        listOf(
            R.drawable.candi_singosari,
            R.drawable.ken_arok_bg,
            R.drawable.candi_jago
        )
    }
    var currentBgIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentBgIndex = (currentBgIndex + 1) % bgImages.size
        }
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .background(Color(0xFFFDFBF6))
                    .windowInsetsPadding(WindowInsets.navigationBars),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { onEvent(HomeUiEvent.OnProfileClicked) },
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    border = BorderStroke(4.dp, OrangePrimary),
                    modifier = Modifier.size(72.dp)
                ) {
                    AsyncImage(
                        model = uiState.userPhotoUrl
                            ?: "https://assets.pikiran-rakyat.com/crop/160x209:1440x832/1200x675/photo/2024/09/21/1845156795.png",
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFDFBF6))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(OrangePrimary)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LoreGate",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Jelajah Sejarah Nyata",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFE0B2)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    onClick = { onEvent(HomeUiEvent.OnStoryListClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Crossfade(
                            targetState = bgImages[currentBgIndex],
                            animationSpec = tween(1000),
                            label = "bgAnim"
                        ) { imgRes ->
                            Image(
                                painter = painterResource(id = imgRes),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        )
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFFFCA28),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Kumpulan Cerita",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mulai Petualanganmu",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    onClick = { onEvent(HomeUiEvent.OnFreeRoamMapClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=1000",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                        )
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Free Roam",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Jelajah Tanpa Misi",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}
