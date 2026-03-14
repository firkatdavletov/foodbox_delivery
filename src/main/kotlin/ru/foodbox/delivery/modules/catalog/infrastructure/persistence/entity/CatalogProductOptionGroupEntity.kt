package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "catalog_product_option_groups",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_catalog_product_option_groups_product_code",
            columnNames = ["product_id", "code"],
        )
    ],
)
class CatalogProductOptionGroupEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "product_id", nullable = false)
    var productId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    var product: CatalogProductEntity? = null,

    @Column(nullable = false, length = 64)
    var code: String,

    @Column(nullable = false, length = 128)
    var title: String,

    @Column(name = "sort_order", nullable = false, columnDefinition = "integer default 0")
    var sortOrder: Int = 0,
)
