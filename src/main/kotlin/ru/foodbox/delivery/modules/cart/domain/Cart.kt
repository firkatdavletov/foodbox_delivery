package ru.foodbox.delivery.modules.cart.domain

import java.time.Instant
import java.util.UUID

data class Cart(
    val id: UUID,
    var owner: CartOwner,
    var status: CartStatus,
    val items: MutableList<CartItem>,
    var deliveryDraft: CartDeliveryDraft?,
    var totalPriceMinor: Long,
    val createdAt: Instant,
    var updatedAt: Instant,
) {

    fun itemsSubtotalMinor(): Long = items.sumOf { it.lineTotalMinor() }

    fun addItem(item: CartItem) {
        ensureActive()
        val existing = items.firstOrNull { it.productId == item.productId && it.variantId == item.variantId }
        if (existing == null) {
            items += item
        } else {
            existing.increase(item.quantity)
        }
        touch(recalculateTotal = true, invalidateDeliveryQuote = true)
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
        touch(recalculateTotal = true, invalidateDeliveryQuote = true)
    }

    fun removeItem(productId: UUID, variantId: UUID?) {
        ensureActive()
        if (variantId == null) {
            items.removeIf { it.productId == productId }
        } else {
            items.removeIf { it.productId == productId && it.variantId == variantId }
        }
        touch(recalculateTotal = true, invalidateDeliveryQuote = true)
    }

    fun clear() {
        ensureActive()
        items.clear()
        touch(recalculateTotal = true, invalidateDeliveryQuote = true)
    }

    fun upsertDeliveryDraft(draft: CartDeliveryDraft) {
        ensureActive()
        deliveryDraft = draft
        touch(recalculateTotal = true, invalidateDeliveryQuote = false)
    }

    fun clearDeliveryDraft() {
        ensureActive()
        deliveryDraft = null
        touch(recalculateTotal = true, invalidateDeliveryQuote = false)
    }

    fun reassignOwner(newOwner: CartOwner) {
        ensureActive()
        owner = newOwner
        touch(recalculateTotal = false, invalidateDeliveryQuote = false)
    }

    fun markMerged() {
        status = CartStatus.MERGED
        touch(recalculateTotal = false, invalidateDeliveryQuote = false)
    }

    fun markOrdered() {
        status = CartStatus.ORDERED
        touch(recalculateTotal = false, invalidateDeliveryQuote = false)
    }

    private fun ensureActive() {
        require(status == CartStatus.ACTIVE) { "Only active cart can be modified" }
    }

    fun recalculateTotalPrice(now: Instant = Instant.now()) {
        totalPriceMinor = itemsSubtotalMinor() + deliveryFeeMinor(now)
    }

    private fun touch(recalculateTotal: Boolean, invalidateDeliveryQuote: Boolean) {
        updatedAt = Instant.now()
        if (invalidateDeliveryQuote) {
            deliveryDraft = deliveryDraft?.invalidateQuote(updatedAt)
        }
        if (recalculateTotal) {
            recalculateTotalPrice(updatedAt)
        }
    }

    private fun deliveryFeeMinor(now: Instant): Long {
        return deliveryDraft?.quote
            ?.takeIf { it.available && !it.isExpired(now) }
            ?.priceMinor
            ?: 0L
    }
}
