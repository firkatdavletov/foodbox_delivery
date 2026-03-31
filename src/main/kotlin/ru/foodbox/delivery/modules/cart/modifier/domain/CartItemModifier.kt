package ru.foodbox.delivery.modules.cart.modifier.domain

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.cart.pricing.domain.ModifierPricedSelection
import java.util.UUID

data class CartItemModifier(
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
    init {
        require(groupCodeSnapshot.isNotBlank()) { "groupCodeSnapshot must not be blank" }
        require(groupNameSnapshot.isNotBlank()) { "groupNameSnapshot must not be blank" }
        require(optionCodeSnapshot.isNotBlank()) { "optionCodeSnapshot must not be blank" }
        require(optionNameSnapshot.isNotBlank()) { "optionNameSnapshot must not be blank" }
        require(priceSnapshot >= 0) { "priceSnapshot must be non-negative" }
        require(quantity > 0) { "quantity must be greater than zero" }
    }

    override val applicationScope: ModifierApplicationScope
        get() = applicationScopeSnapshot

    override val priceMinor: Long
        get() = priceSnapshot
}
