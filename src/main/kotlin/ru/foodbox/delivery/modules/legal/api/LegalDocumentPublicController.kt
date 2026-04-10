package ru.foodbox.delivery.modules.legal.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.legal.api.dto.LegalDocumentResponse
import ru.foodbox.delivery.modules.legal.api.dto.toResponse
import ru.foodbox.delivery.modules.legal.application.LegalDocumentService
import ru.foodbox.delivery.modules.legal.domain.LegalDocumentType

@RestController
@RequestMapping("/api/v1/public/legal-documents")
class LegalDocumentPublicController(
    private val legalDocumentService: LegalDocumentService,
) {

    @GetMapping("/{type}")
    fun getDocument(
        @PathVariable type: String,
    ): LegalDocumentResponse {
        return legalDocumentService.getDocument(LegalDocumentType.fromSlug(type)).toResponse()
    }
}
