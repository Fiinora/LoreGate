package com.loregate.app.feature.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.loregate.app.core.designsystem.DarkSurfaceBrown
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.domain.model.CommunitySpot
import com.loregate.app.domain.model.GeoPoint

@Composable
fun BoxScope.CommunitySpotBottomSheet(
    spot: CommunitySpot,
    currentUserId: String?,
    userLocation: GeoPoint?,
    isRouteLoading: Boolean,
    onToggleHelpfulClick: () -> Unit,
    onGetDirectionsClick: () -> Unit,
    onReportClick: (reason: String) -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val hasLiked = currentUserId != null && spot.helpfulUserIds.contains(currentUserId)
    val isOwner = currentUserId != null && currentUserId == spot.authorId
    val categoryColor = runCatching { Color(android.graphics.Color.parseColor(spot.category.hexColor)) }
        .getOrElse { OrangePrimary }
    val distanceMeters = userLocation?.distanceToMeters(spot.geoPoint)

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
        // Drag handle & close button
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
            // Optional Header Image
            if (spot.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2D241E))
                ) {
                    AsyncImage(
                        model = spot.imageUrl,
                        contentDescription = spot.title,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Badges row: Category + Verified On-Site
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = categoryColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, categoryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = spot.category.emoji, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = spot.category.displayLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }
                }

                if (spot.isVerifiedOnSite) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Terverifikasi di Lokasi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = spot.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Author and distance row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spot.authorName.take(1).uppercase(),
                        color = OrangePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Oleh ${spot.authorName}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                if (distanceMeters != null) {
                    Text(
                        text = " • ${GeoPoint.formatDistance(distanceMeters)}",
                        fontSize = 12.sp,
                        color = OrangePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Description / Tips body
            Text(
                text = spot.description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Helpful Button
                Button(
                    onClick = onToggleHelpfulClick,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasLiked) OrangePrimary else Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Bermanfaat",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (spot.helpfulCount > 0) "Bermanfaat (${spot.helpfulCount})" else "Bermanfaat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Route Navigation Button
                Button(
                    onClick = onGetDirectionsClick,
                    modifier = Modifier.weight(1.2f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isRouteLoading
                ) {
                    if (isRouteLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = "Petunjuk Arah",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Petunjuk Arah",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Owner delete or user report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isOwner) {
                    TextButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Spot Saya", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                } else {
                    TextButton(onClick = { showReportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Laporkan",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Laporkan Temuan Ini?") },
            text = { Text("Laporan dari komunitas membantu menjaga LoreGate tetap akurat dan aman. Temuan yang menerima 3 laporan akan otomatis disembunyikan.") },
            confirmButton = {
                Button(
                    onClick = {
                        showReportDialog = false
                        onReportClick("Konten tidak sesuai / spam")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Laporkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Penemuan Anda?") },
            text = { Text("Temuan dan foto yang terkait akan dihapus secara permanen dari peta publik.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
