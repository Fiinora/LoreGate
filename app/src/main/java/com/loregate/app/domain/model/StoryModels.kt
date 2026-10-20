package com.loregate.app.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

data class Story(
    val id: String,
    val title: String,
    val location: String,
    val synopsis: String,
    val imageUrl: String,
    val firstChapterId: String,
    val totalChapters: Int,
    val unlockedCount: Int,
    val progressPercentage: Int,
    val isSecret: Boolean = false
)

data class Chapter(
    val id: String,
    val order: Int,
    val title: String,
    val locationName: String,
    val isUnlocked: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class ChapterContent(
    val site: HistoricalSite = HistoricalSite(),
    val heroImageUrl: String = "",
    val introduction: String = "",
    val sections: List<ChapterSection> = emptyList(),
    val keyFigures: List<KeyFigure> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val quote: Quote? = null,
    val videoUrl: String? = null,
    val conclusion: String = ""
)

@Keep
@IgnoreExtraProperties
data class ChapterSection(
    val title: String = "",
    val content: String = ""
)

@Keep
@IgnoreExtraProperties
data class KeyFigure(
    val name: String = "",
    val imageUrl: String = "",
    val role: String = ""
)

@Keep
@IgnoreExtraProperties
data class TimelineEvent(
    val date: String = "",
    val description: String = ""
)

@Keep
@IgnoreExtraProperties
data class Quote(
    val text: String = "",
    val author: String = ""
)
