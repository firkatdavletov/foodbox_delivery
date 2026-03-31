package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import java.util.UUID

@Entity
@Table(name = "modifier_options")
class ModifierOptionEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "group_id", nullable = false)
    var groupId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, insertable = false, updatable = false)
    var group: ModifierGroupEntity? = null,

    @Column(nullable = false, length = 128)
    var code: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false, length = 32)
    var priceType: ModifierPriceType,

    @Column(nullable = false)
    var price: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "application_scope", nullable = false, length = 32)
    var applicationScope: ModifierApplicationScope,

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,
)
