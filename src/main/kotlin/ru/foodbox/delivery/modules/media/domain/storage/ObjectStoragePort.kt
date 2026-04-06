package ru.foodbox.delivery.modules.media.domain.storage

import java.time.Duration
import java.time.Instant

data class CreateDirectUploadRequest(
    val objectKey: String,
    val contentType: String,
    val fileSize: Long,
    val expiresIn: Duration,
)

data class DirectUpload(
    val url: String,
    val method: String,
    val requiredHeaders: Map<String, String>,
    val expiresAt: Instant,
)

data class StoredObjectMetadata(
    val contentType: String?,
    val contentLength: Long,
)

interface ObjectStoragePort {
    fun bucket(): String
    fun createDirectUpload(request: CreateDirectUploadRequest): DirectUpload
    fun getObjectMetadata(objectKey: String): StoredObjectMetadata?
    fun getObjectBytes(objectKey: String): ByteArray
    fun putObject(objectKey: String, data: ByteArray, contentType: String)
    fun moveObject(sourceKey: String, destinationKey: String)
    fun buildPublicUrl(objectKey: String): String
}
