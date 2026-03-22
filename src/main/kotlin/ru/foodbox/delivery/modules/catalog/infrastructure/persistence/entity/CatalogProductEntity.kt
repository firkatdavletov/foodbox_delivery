package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catalog_products")
class CatalogProductEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "external_id", unique = true, length = 128)
    var externalId: String? = null,

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(nullable = false, unique = true, length = 255)
    var slug: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "price_minor", nullable = false)
    var priceMinor: Long,

    @Column(name = "old_price_minor")
    var oldPriceMinor: Long? = null,

    @Column(unique = true, length = 128)
    var sku: String? = null,

    @Column(length = 255)
    var brand: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var unit: ProductUnit,

    @Column(name = "count_step", nullable = false)
    var countStep: Int,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
