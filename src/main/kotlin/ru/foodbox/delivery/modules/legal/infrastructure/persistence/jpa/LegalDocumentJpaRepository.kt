package ru.foodbox.delivery.modules.legal.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.legal.domain.LegalDocumentType
import ru.foodbox.delivery.modules.legal.infrastructure.persistence.entity.LegalDocumentEntity

interface LegalDocumentJpaRepository : JpaRepository<LegalDocumentEntity, LegalDocumentType>
