package ru.foodbox.delivery.modules.legal.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.legal.api.dto.LegalDocumentResponse
import ru.foodbox.delivery.modules.legal.api.dto.UpsertLegalDocumentRequest
import ru.foodbox.delivery.modules.legal.api.dto.toResponse
import ru.foodbox.delivery.modules.legal.application.LegalDocumentService
import ru.foodbox.delivery.modules.legal.domain.LegalDocumentType

@RestController
@RequestMapping("/api/v1/admin/legal-documents")
class LegalDocumentAdminController(
    private val legalDocumentService: LegalDocumentService,
) {

    @GetMapping
    fun getDocuments(): List<LegalDocumentResponse> {
        return legalDocumentService.getDocuments().map { it.toResponse() }
    }

    @GetMapping("/{type}")
    fun getDocument(
        @PathVariable type: String,
    ): LegalDocumentResponse {
        return legalDocumentService.getDocument(LegalDocumentType.fromSlug(type)).toResponse()
    }

    @PutMapping("/{type}")
    fun updateDocument(
        @PathVariable type: String,
        @Valid @RequestBody request: UpsertLegalDocumentRequest,
    ): LegalDocumentResponse {
        return legalDocumentService.updateDocument(
            type = LegalDocumentType.fromSlug(type),
            title = request.title,
            subtitle = request.subtitle,
            text = request.text,
        ).toResponse()
    }
}
