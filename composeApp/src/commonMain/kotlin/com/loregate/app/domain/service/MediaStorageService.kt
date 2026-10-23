package com.loregate.app.domain.service

data class UploadedMedia(
    val url: String,
    val thumbnailUrl: String,
    val publicId: String? = null
)

interface MediaStorageService {
    suspend fun uploadImage(
        bytes: ByteArray,
        folder: String = "community_spots",
        fileName: String? = null
    ): Result<UploadedMedia>

    suspend fun deleteImage(publicIdOrUrl: String): Result<Unit>
}
