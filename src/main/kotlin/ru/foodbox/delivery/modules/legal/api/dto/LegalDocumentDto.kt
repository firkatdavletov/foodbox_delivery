package ru.foodbox.delivery.modules.legal.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.foodbox.delivery.modules.legal.domain.LegalDocument
import java.time.Instant

data class LegalDocumentResponse(
    val type: String,
    val title: String,
    val subtitle: String?,
    val text: String,
    val updatedAt: Instant,
)

data class UpsertLegalDocumentRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,

    @field:Size(max = 512)
    val subtitle: String? = null,

    val text: String,
)

fun LegalDocument.toResponse(): LegalDocumentResponse {
    return LegalDocumentResponse(
        type = type.slug,
        title = title,
        subtitle = subtitle,
        text = text,
        updatedAt = updatedAt,
    )
}
