package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "catalog_category_images",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_catalog_category_images_owner_image", columnNames = ["category_id", "image_id"]),
    ],
    indexes = [
        Index(name = "idx_catalog_category_images_category", columnList = "category_id"),
        Index(name = "idx_catalog_category_images_image", columnList = "image_id"),
    ],
)
class CatalogCategoryImageEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID,

    @Column(name = "image_id", nullable = false)
    var imageId: UUID,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
