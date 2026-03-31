package ru.foodbox.delivery.modules.orders.modifier.domain

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.cart.pricing.domain.ModifierPricedSelection
import java.util.UUID

data class OrderItemModifier(
    val modifierGroupId: UUID,
    val modifierOptionId: UUID,
    val groupCodeSnapshot: String,
    val groupNameSnapshot: String,
    val optionCodeSnapshot: String,
    val optionNameSnapshot: String,
    val applicationScopeSnapshot: ModifierApplicationScope,
    val priceSnapshot: Long,
    override val quantity: Int,
) : ModifierPricedSelection {
    override val applicationScope: ModifierApplicationScope
        get() = applicationScopeSnapshot

    override val priceMinor: Long
        get() = priceSnapshot
}
