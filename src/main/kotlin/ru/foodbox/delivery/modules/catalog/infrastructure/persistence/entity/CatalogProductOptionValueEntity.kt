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
    name = "catalog_product_option_values",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_catalog_product_option_values_group_code",
            columnNames = ["option_group_id", "code"],
        )
    ],
)
class CatalogProductOptionValueEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "option_group_id", nullable = false)
    var optionGroupId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false, insertable = false, updatable = false)
    var optionGroup: CatalogProductOptionGroupEntity? = null,

    @Column(nullable = false, length = 64)
    var code: String,

    @Column(nullable = false, length = 128)
    var title: String,

    @Column(name = "sort_order", nullable = false, columnDefinition = "integer default 0")
    var sortOrder: Int = 0,
)
