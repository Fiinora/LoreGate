package com.loregate.app.domain.repository

import com.loregate.app.domain.model.HeritageStamp
import kotlinx.coroutines.flow.Flow

interface PassportRepository {
    fun observeUserStamps(uid: String?): Flow<List<HeritageStamp>>
    suspend fun getUserStamps(uid: String?): Result<List<HeritageStamp>>
    suspend fun getStampBySiteId(siteId: String, isUnlocked: Boolean = false): HeritageStamp?
}
