package com.loregate.app.domain.usecase

import com.loregate.app.domain.model.Chapter
import com.loregate.app.domain.model.Story
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.SiteRepository
import com.loregate.app.domain.repository.UserRepository
import javax.inject.Inject

data class StoryTimelineData(
    val storyTitle: String,
    val storyImage: String,
    val chapters: List<Chapter>,
    val unlockedChapterIds: Set<String>
)

class GetStoriesWithProgressUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val siteRepository: SiteRepository
) {
    suspend operator fun invoke(): Result<List<Story>> = runCatching {
        val currentUser = authRepository.getCurrentUser()
        val unlockedIds = if (currentUser != null) {
            userRepository.getUserAppData(currentUser.uid).getOrNull()?.unlockedChapters?.toSet().orEmpty()
        } else {
            emptySet()
        }

        val allSites = siteRepository.getHistoricalSites().getOrThrow()
        val groupedSites = allSites
            .filter { it.storyGroup.isNotEmpty() }
            .groupBy { it.storyGroup }

        groupedSites.mapNotNull { (groupId, sitesInGroup) ->
            val firstChapter = sitesInGroup.minByOrNull { it.chapterOrder } ?: sitesInGroup.first()
            val total = sitesInGroup.size
            val unlockedCount = sitesInGroup.count { unlockedIds.contains(it.id) }
            val percentage = if (total > 0) (unlockedCount * 100 / total) else 0

            val isHiddenStory = firstChapter.isHidden
            val isFirstChapterUnlocked = unlockedIds.contains(firstChapter.id)

            if (isHiddenStory && !isFirstChapterUnlocked) {
                return@mapNotNull null
            }

            Story(
                id = groupId,
                title = groupId,
                location = firstChapter.location.ifEmpty { "Jawa Timur" },
                synopsis = firstChapter.description,
                imageUrl = firstChapter.imageUrl,
                firstChapterId = firstChapter.id,
                totalChapters = total,
                unlockedCount = unlockedCount,
                progressPercentage = percentage,
                isSecret = isHiddenStory
            )
        }
    }
}

class GetStoryTimelineUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val siteRepository: SiteRepository
) {
    suspend operator fun invoke(storyGroupId: String): Result<StoryTimelineData> = runCatching {
        val currentUser = authRepository.getCurrentUser()
            ?: throw IllegalStateException("User not logged in")

        val unlockedIds = userRepository.getUserAppData(currentUser.uid)
            .getOrNull()?.unlockedChapters?.toSet().orEmpty()

        val sites = siteRepository.getSitesByStoryGroup(storyGroupId).getOrThrow()
        if (sites.isEmpty()) {
            throw NoSuchElementException("No chapters found for this story")
        }

        val sortedSites = sites.sortedBy { it.chapterOrder }
        val firstSite = sortedSites.first()

        val chapters = sortedSites.map { site ->
            Chapter(
                id = site.id,
                order = site.chapterOrder,
                title = site.nama,
                locationName = site.location.ifEmpty { site.nama },
                isUnlocked = unlockedIds.contains(site.id)
            )
        }

        StoryTimelineData(
            storyTitle = storyGroupId,
            storyImage = firstSite.imageUrl,
            chapters = chapters,
            unlockedChapterIds = unlockedIds
        )
    }
}
