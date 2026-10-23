package com.loregate.app.feature.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import coil3.compose.AsyncImage
import com.loregate.app.domain.model.HeritageStamp
import com.loregate.app.domain.model.HeritageTrail
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.loregate.app.core.designsystem.DarkSurfaceBrown
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.model.SiteType
import com.loregate.app.domain.model.Spear
import com.loregate.app.domain.usecase.SiteUnlockStatus

@Composable
fun MapTopHudOverlay(
    showBackButton: Boolean,
    collectedSpearCount: Int,
    spearTotalCount: Int,
    availableSpearCount: Int,
    nearbySpearCount: Int,
    unlockedSitesCount: Int,
    totalSitesCount: Int,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Tombol Kembali
        if (showBackButton) {
            Surface(
                onClick = onBackClick,
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = DarkSurfaceBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(44.dp))
        }

        // Center HUD: Ringkasan Progress Tombak & Situs
        val collected = collectedSpearCount.coerceAtLeast(0)
        val totalDisplay = maxOf(spearTotalCount, collected + availableSpearCount)
        val siteProgress = if (totalSitesCount > 0) {
            (unlockedSitesCount.toFloat() / totalSitesCount.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.94f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info Tombak
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFCA28),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Tombak $collected/$totalDisplay",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (nearbySpearCount > 0) {
                            Text(
                                text = "$nearbySpearCount di dekatmu!",
                                color = Color(0xFF4ADE80),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Jelajahi sekitar",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Divider Vertikal
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                // Info Situs & Progress Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (siteProgress >= 1f) Icons.Default.CheckCircle else Icons.Default.Flag,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Situs $unlockedSitesCount/$totalSitesCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { siteProgress },
                            modifier = Modifier
                                .width(60.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = OrangePrimary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Tombol Profil (Di pojok kanan atas, standar UI Map modern)
        Surface(
            onClick = onProfileClick,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profil Pengguna",
                    tint = OrangePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun BoxScope.MapSelectedSiteBannerCard(
    site: HistoricalSite,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .zIndex(15f),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(OrangePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = site.type.toIcon(),
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.nama,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSurfaceBrown
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = site.description.take(60) + "...",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MapFloatingControlButtons(
    is25DMode: Boolean,
    onToggle25DMode: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMyLocationClick: () -> Unit,
    onToggleTrailSelector: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tombol Daftar Jalur Wisata
        Surface(
            onClick = onToggleTrailSelector,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = "Pilih Jalur Wisata",
                    tint = OrangePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Toggle Mode 2.5D / 3D Tilt
        Surface(
            onClick = onToggle25DMode,
            shape = CircleShape,
            color = if (is25DMode) OrangePrimary else Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (is25DMode) Icons.Default.ViewInAr else Icons.Default.Map,
                    contentDescription = "Mode 2.5D",
                    tint = if (is25DMode) Color.White else DarkSurfaceBrown,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Kontrol Zoom (+ / -) dalam satu Pill Segmented modern
        Surface(
            shape = RoundedCornerShape(23.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(46.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Perbesar",
                        tint = DarkSurfaceBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )

                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Perkecil",
                        tint = DarkSurfaceBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Tombol My Location (Pusatkan ke User - Posisi paling bawah dekat jempol)
        Surface(
            onClick = onMyLocationClick,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Lokasi Saya",
                    tint = DarkSurfaceBrown,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun BoxScope.SiteDetailBottomSheet(
    site: HistoricalSite,
    unlockStatus: SiteUnlockStatus,
    userLocation: GeoPoint?,
    isRouteLoading: Boolean,
    onPrimaryActionClick: () -> Unit,
    onGetDirectionsClick: () -> Unit,
    onAddTipClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val distanceMeters = userLocation?.distanceToMeters(site.geoPoint)
    val isInRange = distanceMeters != null && distanceMeters <= site.radius
    val isLockedSeq = unlockStatus is SiteUnlockStatus.LockedBySequence
    val isActionEnabled = unlockStatus is SiteUnlockStatus.AlreadyUnlocked ||
        unlockStatus is SiteUnlockStatus.ReadyToUnlock

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .heightIn(max = 580.dp)
            .background(
                color = DarkSurfaceBrown.copy(alpha = 0.98f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .zIndex(25f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = site.type.toIcon(),
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.nama,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = site.type.displayLabel,
                        fontSize = 12.sp,
                        color = OrangePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = site.description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPrimaryActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLockedSeq) Color.Gray else OrangePrimary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isActionEnabled
            ) {
                val icon = when (unlockStatus) {
                    SiteUnlockStatus.AlreadyUnlocked -> Icons.AutoMirrored.Filled.MenuBook
                    is SiteUnlockStatus.ReadyToUnlock -> Icons.Default.LockOpen
                    is SiteUnlockStatus.LockedBySequence -> Icons.Default.Lock
                    is SiteUnlockStatus.OutOfRange -> Icons.AutoMirrored.Filled.DirectionsWalk
                }

                val text = when (unlockStatus) {
                    SiteUnlockStatus.AlreadyUnlocked -> "Baca Cerita Sejarah"
                    is SiteUnlockStatus.ReadyToUnlock -> "Buka Kunci Chapter (+50 XP)"
                    is SiteUnlockStatus.LockedBySequence -> {
                        "Selesaikan Chapter ${unlockStatus.previousChapterOrder} Terlebih Dahulu"
                    }
                    is SiteUnlockStatus.OutOfRange -> {
                        "Di Luar Jangkauan (${GeoPoint.formatDistance(unlockStatus.distanceMeters ?: 0f)})"
                    }
                }

                Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onGetDirectionsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OrangePrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                enabled = !isRouteLoading
            ) {
                if (isRouteLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OrangePrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menghitung Rute...")
                } else {
                    Icon(Icons.Default.Directions, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Petunjuk Arah Rute", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onAddTipClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Bagikan Tips / Cerita di Sini (+30 XP)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            if (distanceMeters != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Jarak: ${GeoPoint.formatDistance(distanceMeters)} / ${
                        GeoPoint.formatDistance(site.radius)
                    } jangkauan",
                    color = if (isInRange) Color(0xFF4ADE80) else OrangePrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BoxScope.SpearFocusedCard(
    spear: Spear,
    distanceMeters: Float?,
    isCollecting: Boolean,
    onCollectClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val inRange = distanceMeters != null && distanceMeters <= spear.proximityRadius
    val pulse = rememberInfiniteTransition(label = "spearCardPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
            .fillMaxWidth(0.9f)
            .scale(pulseScale)
            .zIndex(25f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, difficultyColor(spear.difficulty).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spear.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${difficultyLabel(spear.difficulty)} • XP: ${spear.xpReward}",
                        color = difficultyColor(spear.difficulty),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismissClick) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = spear.description.take(80) + if (spear.description.length > 80) "..." else "",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            val distanceText = distanceMeters?.let { GeoPoint.formatDistance(it) } ?: "..."
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (inRange) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.AutoMirrored.Filled.DirectionsWalk
                    },
                    contentDescription = null,
                    tint = if (inRange) Color(0xFF4CAF50) else Color(0xFFFFA500),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Jarak: $distanceText (Perlu: ${spear.proximityRadius.toInt()}m)",
                    color = if (inRange) Color(0xFF4CAF50) else Color(0xFFFFA500),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCollectClick,
                enabled = inRange && !isCollecting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (inRange) difficultyColor(spear.difficulty) else Color.Gray
                )
            ) {
                if (isCollecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    val remaining = ((distanceMeters ?: 0f) - spear.proximityRadius).coerceAtLeast(0f)
                    Text(
                        text = if (inRange) {
                            "AMBIL TOMBAK"
                        } else {
                            "DEKATI LAGI (${GeoPoint.formatDistance(remaining)})"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SpearCollectionDialog(
    spear: Spear?,
    onDismiss: () -> Unit
) {
    if (spear == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(visible = true, enter = scaleIn()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = difficultyColor(spear.difficulty),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚔️", fontSize = 48.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "🎉 Tombak Dikumpulkan!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = spear.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "📍 ${spear.locationName}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = spear.description,
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = OrangePrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "+${spear.xpReward} XP",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("Lanjut Petualangan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun SiteType.toIcon(): ImageVector = when (this) {
    SiteType.TEMPLE -> Icons.Default.Place
    SiteType.PALACE -> Icons.Default.Home
    SiteType.MUSEUM -> Icons.Default.LocationCity
    SiteType.MONUMENT -> Icons.Default.AccountBalance
    SiteType.BATTLEFIELD -> Icons.Default.Bolt
}

private fun difficultyColor(difficulty: String): Color = when (difficulty.uppercase()) {
    "HARD" -> Color(0xFFFF6B6B)
    "MEDIUM" -> Color(0xFFFFA500)
    else -> Color(0xFF4CAF50)
}

private fun difficultyLabel(difficulty: String): String = when (difficulty.uppercase()) {
    "HARD" -> "Hard"
    "MEDIUM" -> "Medium"
    else -> "Easy"
}

@Composable
fun MapActiveTrailHudCard(
    activeTrail: HeritageTrail,
    onExitTrail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(8.dp),
        border = BorderStroke(1.dp, Color(activeTrail.themeColorHex).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(activeTrail.themeColorHex).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(activeTrail.themeColorHex)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = Color(activeTrail.themeColorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JALUR AKTIF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(activeTrail.themeColorHex),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${activeTrail.siteIds.size} Situs",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Text(
                        text = activeTrail.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Surface(
                onClick = onExitTrail,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Keluar Jalur",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeritageTrailSelectorBottomSheet(
    trails: List<HeritageTrail>,
    onSelectTrail: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pilih Jalur Wisata Sejarah",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Navigasi otomatis menghubungkan situs berurutan",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                trails.forEach { trail ->
                    Card(
                        onClick = { onSelectTrail(trail.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(trail.themeColorHex).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = trail.badgeLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(trail.themeColorHex),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${trail.siteIds.size} Situs • ${trail.estimatedMinutes / 60} Jam",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = trail.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = trail.description,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = { onSelectTrail(trail.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Mulai", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StampCelebrationDialog(
    stamp: HeritageStamp?,
    onDismiss: () -> Unit
) {
    if (stamp == null) return

    val inkColor = Color(stamp.inkHexColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF6EE)), // Parchment
            elevation = CardDefaults.cardElevation(12.dp),
            border = BorderStroke(2.dp, inkColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 STEMPEL BARU DICAP!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = inkColor
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Inked Stamp Seal Effect
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(inkColor.copy(alpha = 0.08f), CircleShape)
                        .border(BorderStroke(3.dp, inkColor), CircleShape)
                        .padding(6.dp)
                        .border(BorderStroke(1.5.dp, inkColor.copy(alpha = 0.6f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stamp.sealEmoji, fontSize = 52.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stamp.culturalTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = inkColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Tercatat di Paspor Budaya atas kunjungan ke ${stamp.siteName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A4A4A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = inkColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${stamp.historicalQuote}\"",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = inkColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = inkColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Buka Babak Cerita", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MapOfflineStatusChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFF59E0B), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Mode Offline Aktif (Data Tersimpan)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

