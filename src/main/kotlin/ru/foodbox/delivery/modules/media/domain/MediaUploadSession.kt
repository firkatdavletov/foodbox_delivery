package ru.foodbox.delivery.modules.media.domain

import java.time.Instant

data class MediaUploadSession(
    val mediaImage: MediaImage,
    val uploadUrl: String,
    val uploadMethod: String,
    val requiredHeaders: Map<String, String>,
    val expiresAt: Instant,
)
