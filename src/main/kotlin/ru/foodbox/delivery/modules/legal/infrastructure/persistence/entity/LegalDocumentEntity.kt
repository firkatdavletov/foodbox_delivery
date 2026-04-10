package ru.foodbox.delivery.modules.legal.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.legal.domain.LegalDocumentType
import java.time.Instant

@Entity
@Table(name = "legal_documents")
class LegalDocumentEntity(
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    var type: LegalDocumentType,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "subtitle", length = 512)
    var subtitle: String? = null,

    @Column(name = "content", nullable = false, columnDefinition = "text")
    var content: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
