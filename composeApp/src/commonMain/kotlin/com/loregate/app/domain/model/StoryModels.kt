package com.loregate.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Story(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val synopsis: String = "",
    val imageUrl: String = "",
    val firstChapterId: String = "",
    val totalChapters: Int = 0,
    val unlockedCount: Int = 0,
    val progressPercentage: Int = 0,
    val isSecret: Boolean = false
)

@Serializable
data class Chapter(
    val id: String = "",
    val order: Int = 0,
    val title: String = "",
    val locationName: String = "",
    val isUnlocked: Boolean = false
)

@Serializable
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

@Serializable
data class ChapterSection(
    val title: String = "",
    val content: String = ""
)

@Serializable
data class KeyFigure(
    val name: String = "",
    val imageUrl: String = "",
    val role: String = ""
)

@Serializable
data class TimelineEvent(
    val date: String = "",
    val description: String = ""
)

@Serializable
data class Quote(
    val text: String = "",
    val author: String = ""
)
