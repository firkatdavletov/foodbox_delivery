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
        val existing = items.firstOrNull { it.productId == item.productId }
        if (existing == null) {
            items += item
        } else {
            existing.increase(item.quantity)
        }
        touch()
    }

    fun changeQuantity(productId: UUID, quantity: Int) {
        ensureActive()
        val item = items.firstOrNull { it.productId == productId }
            ?: throw IllegalArgumentException("Product not found in cart")

        item.changeQuantity(quantity)
        touch()
    }

    fun removeItem(productId: UUID) {
        ensureActive()
        items.removeIf { it.productId == productId }
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
