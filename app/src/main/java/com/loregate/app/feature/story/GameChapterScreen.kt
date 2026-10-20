package com.loregate.app.feature.story

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.domain.model.Chapter
import com.loregate.app.domain.usecase.GetStoryTimelineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameChapterUiState(
    val isLoading: Boolean = true,
    val storyTitle: String = "",
    val storyImage: String = "",
    val chapters: List<Chapter> = emptyList(),
    val errorMessage: String? = null
)

sealed interface GameChapterUiEvent {
    data class OnLoadStory(val storyGroupId: String) : GameChapterUiEvent
    data class OnGoToLocationClicked(val siteId: String) : GameChapterUiEvent
    data class OnReadChapterClicked(val siteId: String) : GameChapterUiEvent
    data object OnBackClicked : GameChapterUiEvent
}

sealed interface GameChapterUiEffect {
    data class NavigateToMapWithTarget(val siteId: String) : GameChapterUiEffect
    data class NavigateToChapterDetail(val siteId: String) : GameChapterUiEffect
    data object NavigateBack : GameChapterUiEffect
}

@HiltViewModel
class GameChapterViewModel @Inject constructor(
    private val getStoryTimelineUseCase: GetStoryTimelineUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameChapterUiState())
    val uiState: StateFlow<GameChapterUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<GameChapterUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: GameChapterUiEvent) {
        when (event) {
            is GameChapterUiEvent.OnLoadStory -> loadStoryChapters(event.storyGroupId)
            is GameChapterUiEvent.OnGoToLocationClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(GameChapterUiEffect.NavigateToMapWithTarget(event.siteId))
                }
            }
            is GameChapterUiEvent.OnReadChapterClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(GameChapterUiEffect.NavigateToChapterDetail(event.siteId))
                }
            }
            GameChapterUiEvent.OnBackClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(GameChapterUiEffect.NavigateBack)
                }
            }
        }
    }

    private fun loadStoryChapters(storyGroupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getStoryTimelineUseCase(storyGroupId).fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            storyTitle = data.storyTitle,
                            storyImage = data.storyImage,
                            chapters = data.chapters,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load chapters"
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun GameChapterRoute(
    storyGroupId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMapWithTarget: (String) -> Unit,
    onNavigateToChapterDetail: (String) -> Unit,
    viewModel: GameChapterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(storyGroupId) {
        viewModel.onEvent(GameChapterUiEvent.OnLoadStory(storyGroupId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                GameChapterUiEffect.NavigateBack -> onNavigateBack()
                is GameChapterUiEffect.NavigateToMapWithTarget -> {
                    onNavigateToMapWithTarget(effect.siteId)
                }
                is GameChapterUiEffect.NavigateToChapterDetail -> {
                    onNavigateToChapterDetail(effect.siteId)
                }
            }
        }
    }

    GameChapterScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun GameChapterScreen(
    uiState: GameChapterUiState,
    onEvent: (GameChapterUiEvent) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        }
        uiState.errorMessage != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.errorMessage}", color = Color.Red)
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .height(220.dp)
                        .fillMaxWidth()
                ) {
                    AsyncImage(
                        model = uiState.storyImage.ifEmpty { "https://via.placeholder.com/600x400" },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )

                    IconButton(
                        onClick = { onEvent(GameChapterUiEvent.OnBackClicked) },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 8.dp, start = 16.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = uiState.storyTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    itemsIndexed(uiState.chapters, key = { _, ch -> ch.id }) { index, chapter ->
                        TimelineItem(
                            chapter = chapter,
                            index = index,
                            total = uiState.chapters.size,
                            isUnlocked = chapter.isUnlocked,
                            onUnlockClick = {
                                onEvent(GameChapterUiEvent.OnGoToLocationClicked(chapter.id))
                            },
                            onReadClick = {
                                onEvent(GameChapterUiEvent.OnReadChapterClicked(chapter.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    chapter: Chapter,
    index: Int,
    total: Int,
    isUnlocked: Boolean,
    onUnlockClick: () -> Unit,
    onReadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (index == 0) Color.Transparent else Color.LightGray)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, if (!isUnlocked) Color.Gray else OrangePrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (!isUnlocked) Icons.Outlined.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (!isUnlocked) Color.Gray else OrangePrimary
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (index == total - 1) Color.Transparent else Color.LightGray)
            )
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp, start = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(if (!isUnlocked) 0.dp else 4.dp),
            border = if (!isUnlocked) BorderStroke(1.dp, Color.LightGray) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Chapter ${chapter.order}: ${chapter.title}",
                    fontWeight = FontWeight.Bold,
                    color = if (!isUnlocked) Color.Gray else Color.Black
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(" ${chapter.locationName}", fontSize = 12.sp, color = Color.Gray)
                }

                if (!isUnlocked) {
                    Button(
                        onClick = onUnlockClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pergi ke Lokasi")
                    }
                } else {
                    Button(
                        onClick = onReadClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Baca Cerita")
                    }
                }
            }
        }
    }
}
