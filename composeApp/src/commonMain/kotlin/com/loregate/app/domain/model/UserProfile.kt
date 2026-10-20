package com.loregate.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null
)

@Serializable
data class UserAppData(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val unlockedChapters: List<String> = emptyList(),
    val unlockedChaptersCount: Long = 0,
    val totalChapters: Long = 50,
    val visitedSites: List<String> = emptyList(),
    val placesVisited: Long = 0,
    val experiencePoints: Long = 0,
    val level: Long = 1,
    val achievements: List<String> = emptyList(),
    val accountCreated: Long? = null,
    val lastLogin: Long? = null,
    val notificationsEnabled: Boolean = true,
    val language: String = "en"
)
