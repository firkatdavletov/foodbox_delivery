package ru.foodbox.delivery.modules.legal.domain.repository

import ru.foodbox.delivery.modules.legal.domain.LegalDocument
import ru.foodbox.delivery.modules.legal.domain.LegalDocumentType

interface LegalDocumentRepository {
    fun findAll(): List<LegalDocument>
    fun findByType(type: LegalDocumentType): LegalDocument?
    fun save(document: LegalDocument): LegalDocument
}
