package ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import java.util.UUID

@Entity
@Table(name = "order_item_modifiers")
class OrderItemModifierEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    var orderItem: OrderItemEntity,

    @Column(name = "modifier_group_id", nullable = false)
    var modifierGroupId: UUID,

    @Column(name = "modifier_option_id", nullable = false)
    var modifierOptionId: UUID,

    @Column(name = "group_code_snapshot", nullable = false, length = 128)
    var groupCodeSnapshot: String,

    @Column(name = "group_name_snapshot", nullable = false, length = 255)
    var groupNameSnapshot: String,

    @Column(name = "option_code_snapshot", nullable = false, length = 128)
    var optionCodeSnapshot: String,

    @Column(name = "option_name_snapshot", nullable = false, length = 255)
    var optionNameSnapshot: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "application_scope_snapshot", nullable = false, length = 32)
    var applicationScopeSnapshot: ModifierApplicationScope,

    @Column(name = "price_snapshot", nullable = false)
    var priceSnapshot: Long,

    @Column(nullable = false)
    var quantity: Int,
)
