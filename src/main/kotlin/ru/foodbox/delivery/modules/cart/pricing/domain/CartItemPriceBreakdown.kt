package ru.foodbox.delivery.modules.cart.pricing.domain

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope

data class CartItemPriceBreakdown(
    val basePriceMinor: Long,
    val perItemModifiersMinor: Long,
    val perLineModifiersMinor: Long,
    val unitPriceMinor: Long,
    val lineTotalMinor: Long,
)

fun calculateCartItemPrice(
    basePriceMinor: Long,
    lineQuantity: Int,
    modifiers: List<ModifierPricedSelection>,
): CartItemPriceBreakdown {
    require(basePriceMinor >= 0) { "basePriceMinor must be non-negative" }
    require(lineQuantity > 0) { "lineQuantity must be greater than zero" }

    val perItemModifiersMinor = modifiers
        .filter { it.applicationScope == ModifierApplicationScope.PER_ITEM }
        .sumOf { it.priceMinor * it.quantity }
    val perLineModifiersMinor = modifiers
        .filter { it.applicationScope == ModifierApplicationScope.PER_LINE }
        .sumOf { it.priceMinor * it.quantity }
    val unitPriceMinor = basePriceMinor + perItemModifiersMinor

    return CartItemPriceBreakdown(
        basePriceMinor = basePriceMinor,
        perItemModifiersMinor = perItemModifiersMinor,
        perLineModifiersMinor = perLineModifiersMinor,
        unitPriceMinor = unitPriceMinor,
        lineTotalMinor = unitPriceMinor * lineQuantity + perLineModifiersMinor,
    )
}
