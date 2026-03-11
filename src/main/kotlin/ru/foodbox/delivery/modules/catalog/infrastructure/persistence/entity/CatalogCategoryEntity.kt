package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catalog_categories")
class CatalogCategoryEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "external_id", unique = true, length = 128)
    var externalId: String? = null,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(nullable = false, unique = true, length = 255)
    var slug: String,

    @Column(name = "parent_id")
    var parentId: UUID? = null,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "image_url")
    var imageUrl: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
