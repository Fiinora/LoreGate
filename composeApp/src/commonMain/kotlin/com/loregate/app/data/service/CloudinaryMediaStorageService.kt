package com.loregate.app.data.service

import com.loregate.app.domain.service.MediaStorageService
import com.loregate.app.domain.service.UploadedMedia
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CloudinaryMediaStorageService(
    private val config: MediaStorageConfig,
    private val httpClient: HttpClient = HttpClient()
) : MediaStorageService {

    override suspend fun uploadImage(
        bytes: ByteArray,
        folder: String,
        fileName: String?
    ): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        runCatching {
            val cloudName = config.cloudName.trim()
            val uploadPreset = config.uploadPreset.trim()

            if (cloudName.isBlank() || uploadPreset.isBlank()) {
                throw IllegalStateException(
                    "Cloudinary belum dikonfigurasi. Pastikan CLOUDINARY_CLOUD_NAME dan CLOUDINARY_UPLOAD_PRESET telah diisi di local.properties."
                )
            }

            val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

            val response = httpClient.post(uploadUrl) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("upload_preset", uploadPreset)
                            if (folder.isNotBlank()) {
                                append("folder", folder)
                            }
                            append(
                                key = "file",
                                value = bytes,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, "image/jpeg")
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=\"${fileName ?: "photo.jpg"}\""
                                    )
                                }
                            )
                        }
                    )
                )
            }.bodyAsText()

            val json = Json.parseToJsonElement(response).jsonObject
            val errorObj = json["error"]?.jsonObject
            if (errorObj != null) {
                val errorMsg = errorObj["message"]?.jsonPrimitive?.content ?: "Gagal upload ke Cloudinary"
                throw RuntimeException("Cloudinary error: $errorMsg")
            }

            val secureUrl = json["secure_url"]?.jsonPrimitive?.content
                ?: json["url"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("Cloudinary response tidak memuat secure_url.")

            val publicId = json["public_id"]?.jsonPrimitive?.content

            // Otomatis buat thumbnail 150x150 dengan face crop jika foto potret/landscape
            val thumbnailUrl = if (secureUrl.contains("/upload/")) {
                secureUrl.replace("/upload/", "/upload/c_thumb,w_150,h_150,g_auto/")
            } else {
                secureUrl
            }

            UploadedMedia(
                url = secureUrl,
                thumbnailUrl = thumbnailUrl,
                publicId = publicId
            )
        }
    }

    override suspend fun deleteImage(publicIdOrUrl: String): Result<Unit> = runCatching {
        // Cloudinary client-side deletion using unsigned credentials is intentionally restricted for security.
        // No-op for client-side test flow.
    }
}
