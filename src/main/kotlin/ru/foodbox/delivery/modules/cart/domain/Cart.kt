package ru.foodbox.delivery.modules.cart.domain

import java.time.Instant
import java.util.UUID

data class Cart(
    val id: UUID,
    var owner: CartOwner,
    var status: CartStatus,
    val items: MutableList<CartItem>,
    var totalPriceMinor: Long,
    val createdAt: Instant,
    var updatedAt: Instant,
) {

    fun addItem(item: CartItem) {
        ensureActive()
        val existing = items.firstOrNull { it.productId == item.productId && it.variantId == item.variantId }
        if (existing == null) {
            items += item
        } else {
            existing.increase(item.quantity)
        }
        touch()
    }

    fun changeQuantity(productId: UUID, variantId: UUID?, quantity: Int) {
        ensureActive()
        val matchingItems = if (variantId == null) {
            items.filter { it.productId == productId }
        } else {
            items.filter { it.productId == productId && it.variantId == variantId }
        }

        if (matchingItems.isEmpty()) {
            throw IllegalArgumentException("Product not found in cart")
        }
        if (variantId == null && matchingItems.size > 1) {
            throw IllegalArgumentException("Multiple product variants found in cart. Specify variantId")
        }

        val item = matchingItems.first()

        item.changeQuantity(quantity)
        touch()
    }

    fun removeItem(productId: UUID, variantId: UUID?) {
        ensureActive()
        if (variantId == null) {
            items.removeIf { it.productId == productId }
        } else {
            items.removeIf { it.productId == productId && it.variantId == variantId }
        }
        touch()
    }

    fun clear() {
        ensureActive()
        items.clear()
        touch()
    }

    fun reassignOwner(newOwner: CartOwner) {
        ensureActive()
        owner = newOwner
        touch()
    }

    fun markMerged() {
        status = CartStatus.MERGED
        touch()
    }

    fun markOrdered() {
        status = CartStatus.ORDERED
        touch()
    }

    private fun ensureActive() {
        require(status == CartStatus.ACTIVE) { "Only active cart can be modified" }
    }

    private fun touch() {
        updatedAt = Instant.now()
        totalPriceMinor = items.sumOf { it.lineTotalMinor() }
    }
}
