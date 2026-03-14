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
    name = "catalog_product_variant_option_values",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_catalog_product_variant_option_values_variant_group",
            columnNames = ["variant_id", "option_group_id"],
        )
    ],
)
class CatalogProductVariantOptionValueEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "variant_id", nullable = false)
    var variantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false, insertable = false, updatable = false)
    var variant: CatalogProductVariantEntity? = null,

    @Column(name = "option_group_id", nullable = false)
    var optionGroupId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false, insertable = false, updatable = false)
    var optionGroup: CatalogProductOptionGroupEntity? = null,

    @Column(name = "option_value_id", nullable = false)
    var optionValueId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", nullable = false, insertable = false, updatable = false)
    var optionValue: CatalogProductOptionValueEntity? = null,
)
