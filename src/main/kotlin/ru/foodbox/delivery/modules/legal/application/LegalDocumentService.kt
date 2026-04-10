package ru.foodbox.delivery.modules.legal.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.legal.domain.LegalDocument
import ru.foodbox.delivery.modules.legal.domain.LegalDocumentType
import ru.foodbox.delivery.modules.legal.domain.repository.LegalDocumentRepository
import java.time.Clock

@Service
class LegalDocumentService(
    private val legalDocumentRepository: LegalDocumentRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getDocuments(): List<LegalDocument> {
        val documentsByType = legalDocumentRepository.findAll().associateBy(LegalDocument::type)
        return LegalDocumentType.entries.map { type ->
            documentsByType[type] ?: defaultDocument(type)
        }
    }

    @Transactional(readOnly = true)
    fun getDocument(type: LegalDocumentType): LegalDocument {
        return legalDocumentRepository.findByType(type) ?: defaultDocument(type)
    }

    @Transactional
    fun updateDocument(
        type: LegalDocumentType,
        title: String,
        subtitle: String?,
        text: String,
    ): LegalDocument {
        val existing = legalDocumentRepository.findByType(type)
        val now = clock.instant()

        return legalDocumentRepository.save(
            LegalDocument(
                type = type,
                title = title.trim(),
                subtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() },
                text = text,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        )
    }

    private fun defaultDocument(type: LegalDocumentType): LegalDocument {
        val now = clock.instant()
        return LegalDocument(
            type = type,
            title = type.defaultTitle,
            subtitle = null,
            text = "",
            createdAt = now,
            updatedAt = now,
        )
    }
}
