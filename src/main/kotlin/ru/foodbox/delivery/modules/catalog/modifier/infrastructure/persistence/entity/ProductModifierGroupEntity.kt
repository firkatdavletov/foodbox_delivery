package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductEntity
import java.util.UUID

@Entity
@Table(name = "product_modifier_groups")
class ProductModifierGroupEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "product_id", nullable = false)
    var productId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    var product: CatalogProductEntity? = null,

    @Column(name = "modifier_group_id", nullable = false)
    var modifierGroupId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_group_id", nullable = false, insertable = false, updatable = false)
    var modifierGroup: ModifierGroupEntity? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,
)
