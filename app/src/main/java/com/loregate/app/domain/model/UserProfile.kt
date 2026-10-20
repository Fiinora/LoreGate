package com.loregate.app.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

@Keep
@IgnoreExtraProperties
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
    @ServerTimestamp val accountCreated: Date? = null,
    @ServerTimestamp val lastLogin: Date? = null,
    val notificationsEnabled: Boolean = true,
    val language: String = "en"
) {
    constructor() : this(
        uid = "",
        displayName = "",
        email = "",
        photoUrl = "",
        unlockedChapters = emptyList(),
        unlockedChaptersCount = 0,
        totalChapters = 50,
        visitedSites = emptyList(),
        placesVisited = 0,
        experiencePoints = 0,
        level = 1,
        achievements = emptyList(),
        accountCreated = null,
        lastLogin = null,
        notificationsEnabled = true,
        language = "en"
    )
}
