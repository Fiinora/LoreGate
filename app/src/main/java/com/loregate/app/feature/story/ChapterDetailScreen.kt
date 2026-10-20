package com.loregate.app.feature.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.core.designsystem.PaperColor
import com.loregate.app.core.designsystem.ReaderTextColor
import com.loregate.app.domain.model.ChapterContent
import com.loregate.app.domain.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChapterDetailUiState(
    val isLoading: Boolean = true,
    val chapter: ChapterContent? = null,
    val errorMessage: String? = null
)

sealed interface ChapterDetailUiEvent {
    data class OnLoadChapter(val siteId: String) : ChapterDetailUiEvent
    data class OnContinueNextChapterClicked(val nextSiteId: String) : ChapterDetailUiEvent
    data object OnBackClicked : ChapterDetailUiEvent
}

sealed interface ChapterDetailUiEffect {
    data class NavigateToNextSiteOnMap(val nextSiteId: String) : ChapterDetailUiEffect
    data object NavigateBack : ChapterDetailUiEffect
}

@HiltViewModel
class ChapterDetailViewModel @Inject constructor(
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterDetailUiState())
    val uiState: StateFlow<ChapterDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ChapterDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: ChapterDetailUiEvent) {
        when (event) {
            is ChapterDetailUiEvent.OnLoadChapter -> loadChapter(event.siteId)
            is ChapterDetailUiEvent.OnContinueNextChapterClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(ChapterDetailUiEffect.NavigateToNextSiteOnMap(event.nextSiteId))
                }
            }
            ChapterDetailUiEvent.OnBackClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(ChapterDetailUiEffect.NavigateBack)
                }
            }
        }
    }

    private fun loadChapter(siteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            siteRepository.getChapterContent(siteId).fold(
                onSuccess = { content ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            chapter = content,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "An unknown error occurred."
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ChapterDetailRoute(
    siteId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNextSiteOnMap: (String) -> Unit,
    viewModel: ChapterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(siteId) {
        viewModel.onEvent(ChapterDetailUiEvent.OnLoadChapter(siteId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ChapterDetailUiEffect.NavigateBack -> onNavigateBack()
                is ChapterDetailUiEffect.NavigateToNextSiteOnMap -> {
                    onNavigateToNextSiteOnMap(effect.nextSiteId)
                }
            }
        }
    }

    ChapterDetailScreen(
        siteId = siteId,
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun ChapterDetailScreen(
    siteId: String,
    uiState: ChapterDetailUiState,
    onEvent: (ChapterDetailUiEvent) -> Unit
) {
    val scrollState = rememberScrollState()

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaperColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        }

        uiState.errorMessage != null || uiState.chapter == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaperColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Gagal memuat cerita",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = { onEvent(ChapterDetailUiEvent.OnLoadChapter(siteId)) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Coba Lagi")
                    }
                }
            }
        }

        else -> {
            val content = uiState.chapter
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaperColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        if (content.heroImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = content.heroImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, PaperColor),
                                        startY = 400f
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .offset(y = (-60).dp)
                    ) {
                        Text(
                            text = "CHAPTER ${content.site.chapterOrder}",
                            style = MaterialTheme.typography.labelLarge,
                            color = OrangePrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = content.site.nama,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = ReaderTextColor,
                            lineHeight = 40.sp
                        )

                        Surface(
                            color = Color(0xFFE7E5E4),
                            shape = CircleShape,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = content.site.type.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF44403C)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFD6D3D1), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(24.dp))

                        if (content.introduction.isNotEmpty()) {
                            Text(
                                text = content.introduction,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    lineHeight = 32.sp,
                                    fontSize = 18.sp
                                ),
                                color = ReaderTextColor
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        content.sections.forEach { section ->
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = section.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    lineHeight = 30.sp,
                                    fontSize = 16.sp
                                ),
                                color = ReaderTextColor
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        if (content.keyFigures.isNotEmpty()) {
                            Text(
                                text = "Tokoh Penting",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = ReaderTextColor
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            content.keyFigures.forEach { figure ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = figure.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = figure.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = figure.role,
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        if (!content.videoUrl.isNullOrEmpty()) {
                            Text(
                                text = "Arsip Video",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = ReaderTextColor
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        if (content.conclusion.isNotEmpty()) {
                            Text(
                                text = content.conclusion,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    lineHeight = 30.sp
                                ),
                                color = ReaderTextColor
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                        }

                        if (content.site.nextSiteId.isNotEmpty()) {
                            Button(
                                onClick = {
                                    onEvent(
                                        ChapterDetailUiEvent.OnContinueNextChapterClicked(
                                            content.site.nextSiteId
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(8.dp)
                            ) {
                                Text(
                                    text = "Continue to next chapter",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                            }
                        } else {
                            Button(
                                onClick = { onEvent(ChapterDetailUiEvent.OnBackClicked) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Finish Story")
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }

                IconButton(
                    onClick = { onEvent(ChapterDetailUiEvent.OnBackClicked) },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 10.dp, start = 16.dp)
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ReaderTextColor
                    )
                }
            }
        }
    }
}
