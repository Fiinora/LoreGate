package com.loregate.app.feature.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.loregate.app.domain.model.Story
import com.loregate.app.domain.repository.SiteRepository
import com.loregate.app.domain.usecase.GetStoriesWithProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoryListUiState(
    val isLoading: Boolean = true,
    val isSeedingData: Boolean = false,
    val stories: List<Story> = emptyList(),
    val expandedStoryId: String? = null,
    val errorMessage: String? = null
) {
    val normalStories: List<Story>
        get() = stories.filter { !it.isSecret }
    val hiddenStories: List<Story>
        get() = stories.filter { !it.isSecret }
}

sealed interface StoryListUiEvent {
    data object OnRefresh : StoryListUiEvent
    data object OnSeedInitialDataClicked : StoryListUiEvent
    data class OnToggleExpandStory(val storyId: String) : StoryListUiEvent
    data class OnPlayStoryClicked(val storyId: String) : StoryListUiEvent
    data object OnBackClicked : StoryListUiEvent
}

sealed interface StoryListUiEffect {
    data class NavigateToGameChapter(val storyGroupId: String) : StoryListUiEffect
    data object NavigateBack : StoryListUiEffect
}

@HiltViewModel
class StoryListViewModel @Inject constructor(
    private val getStoriesWithProgressUseCase: GetStoriesWithProgressUseCase,
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryListUiState())
    val uiState: StateFlow<StoryListUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<StoryListUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        fetchStories()
    }

    fun onEvent(event: StoryListUiEvent) {
        when (event) {
            StoryListUiEvent.OnRefresh -> fetchStories()
            StoryListUiEvent.OnSeedInitialDataClicked -> seedInitialData()
            is StoryListUiEvent.OnToggleExpandStory -> {
                _uiState.update { state ->
                    state.copy(
                        expandedStoryId = if (state.expandedStoryId == event.storyId) null else event.storyId
                    )
                }
            }
            is StoryListUiEvent.OnPlayStoryClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(StoryListUiEffect.NavigateToGameChapter(event.storyId))
                }
            }
            StoryListUiEvent.OnBackClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(StoryListUiEffect.NavigateBack)
                }
            }
        }
    }

    private fun seedInitialData() {
        if (_uiState.value.isSeedingData) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSeedingData = true, errorMessage = null) }
            siteRepository.seedInitialData(null).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSeedingData = false) }
                    fetchStories()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSeedingData = false,
                            errorMessage = "Gagal melakukan seed data: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    private fun fetchStories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getStoriesWithProgressUseCase().fold(
                onSuccess = { stories ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            stories = stories,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Gagal memuat cerita"
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun StoryListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToGameChapter: (String) -> Unit,
    viewModel: StoryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                StoryListUiEffect.NavigateBack -> onNavigateBack()
                is StoryListUiEffect.NavigateToGameChapter -> {
                    onNavigateToGameChapter(effect.storyGroupId)
                }
            }
        }
    }

    StoryListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryListScreen(
    uiState: StoryListUiState,
    onEvent: (StoryListUiEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pilih Cerita",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(StoryListUiEvent.OnBackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF1F5F9))
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = OrangePrimary
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.stories.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum Ada Cerita di Firestore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Klik tombol di bawah untuk mengisi data awal (Cerita, Situs Sejarah, Chapter & Tombak) ke database Firestore kamu.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onEvent(StoryListUiEvent.OnSeedInitialDataClicked) },
                            enabled = !uiState.isSeedingData,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isSeedingData) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mengisi Data...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seed Data Awal ke Firestore")
                            }
                        }
                    }
                }
                else -> {
                    val normalStories = uiState.normalStories
                    val hiddenStories = uiState.hiddenStories

                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        if (normalStories.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Kisah Utama",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            items(normalStories, key = { it.id }) { story ->
                                StoryCard(
                                    story = story,
                                    isExpanded = uiState.expandedStoryId == story.id,
                                    onExpandToggle = {
                                        onEvent(StoryListUiEvent.OnToggleExpandStory(story.id))
                                    },
                                    onPlay = {
                                        onEvent(StoryListUiEvent.OnPlayStoryClicked(story.id))
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (hiddenStories.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Kisah Tersembunyi (Ditemukan!)",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            items(hiddenStories, key = { it.id }) { story ->
                                StoryCard(
                                    story = story,
                                    isExpanded = uiState.expandedStoryId == story.id,
                                    onExpandToggle = {
                                        onEvent(StoryListUiEvent.OnToggleExpandStory(story.id))
                                    },
                                    onPlay = {
                                        onEvent(StoryListUiEvent.OnPlayStoryClicked(story.id))
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryCard(
    story: Story,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
            ) {
                AsyncImage(
                    model = story.imageUrl.ifEmpty { "https://via.placeholder.com/600x400" },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${story.totalChapters} Chapters",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFCA28),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = story.synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mulai Cerita")
                    }
                }
            }
        }
    }
}

private val ProgressBackgroundGray = Color(0xFFF1F5F9)

@Composable
fun StoryProgressRoute(
    onNavigateBack: () -> Unit,
    onNavigateToGameChapter: (String) -> Unit,
    viewModel: StoryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                StoryListUiEffect.NavigateBack -> onNavigateBack()
                is StoryListUiEffect.NavigateToGameChapter -> {
                    onNavigateToGameChapter(effect.storyGroupId)
                }
            }
        }
    }

    StoryProgressScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryProgressScreen(
    uiState: StoryListUiState,
    onEvent: (StoryListUiEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pencapaian Cerita", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(StoryListUiEvent.OnBackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProgressBackgroundGray)
            )
        },
        containerColor = ProgressBackgroundGray
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = OrangePrimary
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.stories.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum Ada Cerita di Firestore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Klik tombol di bawah untuk mengisi data awal (Cerita, Situs Sejarah, Chapter & Tombak) ke database Firestore kamu.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onEvent(StoryListUiEvent.OnSeedInitialDataClicked) },
                            enabled = !uiState.isSeedingData,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isSeedingData) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mengisi Data...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seed Data Awal ke Firestore")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.stories, key = { it.id }) { story ->
                            ProgressCard(
                                story = story,
                                onClick = {
                                    onEvent(StoryListUiEvent.OnPlayStoryClicked(story.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressCard(
    story: Story,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${story.unlockedCount} dari ${story.totalChapters} Chapter Selesai",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (story.progressPercentage == 100) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Completed",
                        tint = Color(0xFFFFCA28),
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Continue",
                        tint = OrangePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { story.progressPercentage / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (story.progressPercentage == 100) Color(0xFF10B981) else OrangePrimary,
                    trackColor = Color(0xFFF1F5F9)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${story.progressPercentage}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (story.progressPercentage == 100) Color(0xFF10B981) else OrangePrimary
                )
            }
        }
    }
}
