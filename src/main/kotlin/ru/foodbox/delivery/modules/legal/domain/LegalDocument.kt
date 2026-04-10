package ru.foodbox.delivery.modules.legal.domain

import java.time.Instant

data class LegalDocument(
    val type: LegalDocumentType,
    val title: String,
    val subtitle: String?,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
