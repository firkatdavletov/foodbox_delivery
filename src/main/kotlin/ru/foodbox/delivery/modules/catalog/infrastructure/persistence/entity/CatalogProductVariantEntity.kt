package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catalog_product_variants")
class CatalogProductVariantEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "product_id", nullable = false)
    var productId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    var product: CatalogProductEntity? = null,

    @Column(name = "external_id", length = 128)
    var externalId: String? = null,

    @Column(nullable = false, unique = true, length = 128)
    var sku: String,

    @Column(length = 255)
    var title: String? = null,

    @Column(name = "price_minor")
    var priceMinor: Long? = null,

    @Column(name = "old_price_minor")
    var oldPriceMinor: Long? = null,

    @Column(name = "image_url", columnDefinition = "text")
    var imageUrl: String? = null,

    @Column(name = "sort_order", nullable = false, columnDefinition = "integer default 0")
    var sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
