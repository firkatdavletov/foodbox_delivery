package ru.foodbox.delivery.modules.cart.pricing.domain

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope

interface ModifierPricedSelection {
    val applicationScope: ModifierApplicationScope
    val priceMinor: Long
    val quantity: Int
}
