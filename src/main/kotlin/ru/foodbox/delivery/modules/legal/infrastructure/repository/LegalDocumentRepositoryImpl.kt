package ru.foodbox.delivery.modules.legal.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.legal.domain.LegalDocument
import ru.foodbox.delivery.modules.legal.domain.LegalDocumentType
import ru.foodbox.delivery.modules.legal.domain.repository.LegalDocumentRepository
import ru.foodbox.delivery.modules.legal.infrastructure.persistence.entity.LegalDocumentEntity
import ru.foodbox.delivery.modules.legal.infrastructure.persistence.jpa.LegalDocumentJpaRepository
import kotlin.jvm.optionals.getOrNull

@Repository
class LegalDocumentRepositoryImpl(
    private val jpaRepository: LegalDocumentJpaRepository,
) : LegalDocumentRepository {

    override fun findAll(): List<LegalDocument> {
        return jpaRepository.findAll().map { it.toDomain() }
    }

    override fun findByType(type: LegalDocumentType): LegalDocument? {
        return jpaRepository.findById(type).getOrNull()?.toDomain()
    }

    override fun save(document: LegalDocument): LegalDocument {
        val existing = jpaRepository.findById(document.type).getOrNull()
        val entity = existing ?: LegalDocumentEntity(
            type = document.type,
            title = document.title,
            subtitle = document.subtitle,
            content = document.text,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt,
        )

        entity.title = document.title
        entity.subtitle = document.subtitle
        entity.content = document.text
        entity.createdAt = document.createdAt
        entity.updatedAt = document.updatedAt

        return jpaRepository.save(entity).toDomain()
    }

    private fun LegalDocumentEntity.toDomain(): LegalDocument {
        return LegalDocument(
            type = type,
            title = title,
            subtitle = subtitle,
            text = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
