package com.loregate.app.data.repository

import com.loregate.app.domain.model.HeritageStamp
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.PassportRepository
import com.loregate.app.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPassportRepository(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : PassportRepository {

    private val defaultStampsCatalog = HeritageStamp.buildDefaultStamps()

    override fun observeUserStamps(uid: String?): Flow<List<HeritageStamp>> {
        val targetUidFlow = if (uid != null) {
            flowOf(uid)
        } else {
            authRepository.currentUser.map { it?.uid }
        }

        return targetUidFlow.distinctUntilChanged().flatMapLatest { currentUid ->
            if (currentUid.isNullOrBlank()) {
                flowOf(defaultStampsCatalog)
            } else {
                userRepository.observeUserAppData(currentUid).map { appData ->
                    val unlockedIds = (appData?.unlockedChapters.orEmpty() + appData?.visitedSites.orEmpty()).toSet()
                    mapCatalogWithUnlocked(unlockedIds)
                }
            }
        }
    }

    override suspend fun getUserStamps(uid: String?): Result<List<HeritageStamp>> = runCatching {
        val targetUid = uid ?: authRepository.getCurrentUser()?.uid
        if (targetUid.isNullOrBlank()) {
            defaultStampsCatalog
        } else {
            val appData = userRepository.getUserAppData(targetUid).getOrNull()
            val unlockedIds = (appData?.unlockedChapters.orEmpty() + appData?.visitedSites.orEmpty()).toSet()
            mapCatalogWithUnlocked(unlockedIds)
        }
    }

    override suspend fun getStampBySiteId(siteId: String, isUnlocked: Boolean): HeritageStamp? {
        val stamp = defaultStampsCatalog.find { it.siteId == siteId } ?: return null
        return stamp.copy(
            isUnlocked = isUnlocked,
            unlockedDate = if (isUnlocked) "Baru saja diperoleh" else null
        )
    }

    private fun mapCatalogWithUnlocked(unlockedIds: Set<String>): List<HeritageStamp> {
        return defaultStampsCatalog.map { stamp ->
            val unlocked = unlockedIds.contains(stamp.siteId)
            stamp.copy(
                isUnlocked = unlocked,
                unlockedDate = if (unlocked) "Tercatat di Paspor" else null
            )
        }
    }
}
