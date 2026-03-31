package ru.foodbox.delivery.modules.cart.domain

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.cart.modifier.domain.CartItemModifier
import ru.foodbox.delivery.modules.cart.pricing.domain.calculateCartItemPrice
import java.time.Instant
import java.util.UUID

data class CartItem(
    val id: UUID = UUID.randomUUID(),
    val productId: UUID,
    val variantId: UUID?,
    val title: String,
    val unit: ProductUnit,
    val countStep: Int,
    var quantity: Int,
    val priceMinor: Long,
    val modifiers: List<CartItemModifier> = emptyList(),
    val createdAt: Instant = Instant.now(),
) {
    init {
        require(countStep > 0) { "countStep must be greater than zero" }
        require(quantity > 0) { "quantity must be greater than zero" }
        require(quantity % countStep == 0) { "quantity must match countStep" }
        require(priceMinor >= 0) { "priceMinor must be positive" }
    }

    fun changeQuantity(newQuantity: Int) {
        require(newQuantity > 0) { "quantity must be greater than zero" }
        require(newQuantity % countStep == 0) { "quantity must match countStep" }
        quantity = newQuantity
    }

    fun increase(delta: Int) {
        require(delta > 0) { "delta must be greater than zero" }
        changeQuantity(quantity + delta)
    }

    fun lineTotalMinor(): Long = calculateCartItemPrice(priceMinor, quantity, modifiers).lineTotalMinor

    fun hasSameConfiguration(productId: UUID, variantId: UUID?, modifiers: List<CartItemModifier>): Boolean {
        return this.productId == productId &&
            this.variantId == variantId &&
            modifierSignature(this.modifiers) == modifierSignature(modifiers)
    }

    private fun modifierSignature(modifiers: List<CartItemModifier>): List<String> {
        return modifiers.map {
            "${it.modifierGroupId}:${it.modifierOptionId}:${it.quantity}"
        }.sorted()
    }
}
