package ru.foodbox.delivery.modules.cart.domain

import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.model.DeliveryInfo
import ru.foodbox.delivery.services.model.UnitOfMeasure
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class Cart(
    val id: UUID,
    var owner: CartOwner,
    var status: CartStatus,
    val items: MutableList<CartItem> = mutableListOf(),
    var totalPrice: BigDecimal = BigDecimal(0),
    val createdAt: LocalDateTime,
    var updatedAt: LocalDateTime,
) {
    fun addItem(
        productId: Long,
        title: String,
        countStep: Int,
        unit: UnitOfMeasure,
        quantity: Int,
        priceSnapshot: BigDecimal
    ) {
        require(status == CartStatus.ACTIVE) { "Only active cart can be modified" }
        require(quantity > 0) { "Quantity must be greater than zero" }

        val existing = items.firstOrNull { it.productId == productId }
        if (existing != null) {
            existing.increase(quantity)
        } else {
            items += CartItem(
                productId = productId,
                title = title,
                quantity = quantity,
                price = priceSnapshot,
                unit = unit,
                countStep = countStep
            )
        }
        updatedAt = LocalDateTime.now()
        updateTotalPrice()
    }

    fun changeQuantity(productId: Long, quantity: Int) {
        require(status == CartStatus.ACTIVE) { "Only active cart can be modified" }

        val item = items.firstOrNull { it.productId == productId }
            ?: throw IllegalArgumentException("Product is not in cart")

        item.changeQuantity(quantity)
        updatedAt = LocalDateTime.now()
        updateTotalPrice()
    }

    fun removeItem(productId: Long) {
        require(status == CartStatus.ACTIVE) { "Only active cart can be modified" }
        items.removeIf { it.productId == productId }
        updatedAt = LocalDateTime.now()
        updateTotalPrice()
    }

    fun clear() {
        require(status == CartStatus.ACTIVE) { "Only active cart can be modified" }
        items.clear()
        updatedAt = LocalDateTime.now()
        updateTotalPrice()
    }

    fun reassignOwner(newOwner: CartOwner) {
        require(status == CartStatus.ACTIVE) { "Only active cart can be reassigned" }
        owner = newOwner
        updatedAt = LocalDateTime.now()
    }

    fun markMerged() {
        status = CartStatus.MERGED
        updatedAt = LocalDateTime.now()
    }

    private fun updateTotalPrice() {
        totalPrice = items.sumOf { it.price }
    }
}