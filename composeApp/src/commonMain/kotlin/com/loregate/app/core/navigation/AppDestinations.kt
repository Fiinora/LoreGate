package com.loregate.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

@Serializable
data object HomeRoute

@Serializable
data class MapRoute(
    val targetSiteId: String? = null,
    val initialTrailId: String? = null
)

@Serializable
data object ProfileRoute

@Serializable
data object PassportRoute

@Serializable
data object StoryListRoute

@Serializable
data object StoryProgressRoute

@Serializable
data class GameChapterRoute(val storyGroupId: String)

@Serializable
data class ChapterDetailRoute(val siteId: String)
