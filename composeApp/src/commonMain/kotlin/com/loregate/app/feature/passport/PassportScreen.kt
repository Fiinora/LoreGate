package com.loregate.app.feature.passport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.core.designsystem.SurfaceColor
import com.loregate.app.core.designsystem.TextColorDark
import com.loregate.app.core.designsystem.TextColorGray
import com.loregate.app.domain.model.HeritageStamp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PassportRoute(
    onNavigateBack: () -> Unit,
    onNavigateToMapWithSite: (String) -> Unit,
    viewModel: PassportViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                PassportUiEffect.NavigateBack -> onNavigateBack()
                is PassportUiEffect.NavigateToMapWithSite -> onNavigateToMapWithSite(effect.siteId)
            }
        }
    }

    PassportScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportScreen(
    uiState: PassportUiState,
    onEvent: (PassportUiEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Paspor Jelajah Budaya",
                        fontWeight = FontWeight.Bold,
                        color = TextColorDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(PassportUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextColorDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        containerColor = SurfaceColor
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Passport Cover Header
            item(span = { GridItemSpan(2) }) {
                PassportCoverCard(
                    explorerName = uiState.explorerName,
                    explorerLevel = uiState.explorerLevel,
                    unlockedCount = uiState.unlockedCount,
                    totalCount = uiState.totalCount
                )
            }

            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        text = "Koleksi Stempel Situs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextColorDark
                    )
                    Text(
                        text = "Kunjungi situs fisik di dunia nyata untuk mengecap stempel resmi",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextColorGray
                    )
                }
            }

            itemsIndexed(uiState.stamps) { index, stamp ->
                // Apply a slight tilt for unlocked stamps like a real ink stamp imprint
                val rotationAngle = if (stamp.isUnlocked) {
                    if (index % 2 == 0) -3f else 2.5f
                } else 0f

                HeritageStampItemCard(
                    stamp = stamp,
                    rotationAngle = rotationAngle,
                    onClick = { onEvent(PassportUiEvent.OnStampClicked(stamp)) }
                )
            }
        }
    }

    uiState.selectedStamp?.let { stamp ->
        StampDetailDialog(
            stamp = stamp,
            onDismiss = { onEvent(PassportUiEvent.OnDismissStampDetail) },
            onNavigateToMap = { onEvent(PassportUiEvent.OnNavigateToSiteOnMap(stamp.siteId)) }
        )
    }
}

@Composable
private fun PassportCoverCard(
    explorerName: String,
    explorerLevel: Long,
    unlockedCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C221E)), // Deep Antique Leather
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.5f)) // Gold Foil Trim
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF3E2723), Color(0xFF1B1412))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PASPOR PENJELAJAH",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 2.sp,
                            color = Color(0xFFD4AF37), // Antique Gold
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "LoreGate Nusantara",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFD4AF37).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🏛️", fontSize = 22.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Nama Penjelajah",
                            fontSize = 11.sp,
                            color = Color(0xFFAAAAAA)
                        )
                        Text(
                            text = explorerName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Tingkat Pengelana",
                            fontSize = 11.sp,
                            color = Color(0xFFAAAAAA)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tingkat $explorerLevel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                val progress = if (totalCount > 0) (unlockedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Stempel Terkumpul",
                            fontSize = 12.sp,
                            color = Color(0xFFCCCCCC)
                        )
                        Text(
                            text = "$unlockedCount dari $totalCount Stempel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFD4AF37),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeritageStampItemCard(
    stamp: HeritageStamp,
    rotationAngle: Float,
    onClick: () -> Unit
) {
    val inkColor = Color(stamp.inkHexColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.88f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stamp.isUnlocked) Color(0xFFFAF6EE) else Color(0xFFF1ECE4) // Parchment effect
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (stamp.isUnlocked) 4.dp else 1.dp),
        border = BorderStroke(
            1.dp,
            if (stamp.isUnlocked) inkColor.copy(alpha = 0.3f) else Color(0xFFD7CCC8)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (stamp.isUnlocked) {
                // Inked Stamp Seal Effect
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(BorderStroke(2.dp, inkColor), shape = CircleShape)
                            .padding(4.dp)
                            .border(BorderStroke(1.dp, inkColor.copy(alpha = 0.5f)), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stamp.sealEmoji,
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stamp.siteName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = inkColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "RESMI • DICAP",
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = inkColor.copy(alpha = 0.8f)
                    )
                }
            } else {
                // Locked Silhouette Stamp
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color.Black.copy(alpha = 0.05f), shape = CircleShape)
                            .border(BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.4f)), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stamp.siteName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Belum Dikunjungi",
                        fontSize = 10.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StampDetailDialog(
    stamp: HeritageStamp,
    onDismiss: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    val inkColor = Color(stamp.inkHexColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.5.dp, if (stamp.isUnlocked) inkColor.copy(alpha = 0.4f) else Color(0xFFD7CCC8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seal Imprint
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(
                            BorderStroke(3.dp, if (stamp.isUnlocked) inkColor else Color.Gray),
                            shape = CircleShape
                        )
                        .padding(6.dp)
                        .border(
                            BorderStroke(1.5.dp, if (stamp.isUnlocked) inkColor.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.5f)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (stamp.isUnlocked) {
                        Text(text = stamp.sealEmoji, fontSize = 48.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stamp.culturalTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (stamp.isUnlocked) inkColor else TextColorDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${stamp.siteName} • ${stamp.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextColorGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Historical Quote box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (stamp.isUnlocked) inkColor.copy(alpha = 0.08f) else Color(0xFFEEEEEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${stamp.historicalQuote}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = if (stamp.isUnlocked) inkColor else Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stamp.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A4A4A),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (stamp.isUnlocked) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = inkColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = onNavigateToMap,
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lacak di Peta", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
