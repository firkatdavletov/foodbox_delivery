package ru.foodbox.delivery.modules.cart.domain

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.util.UUID

data class CartItem(
    val productId: UUID,
    val title: String,
    val unit: ProductUnit,
    val countStep: Int,
    var quantity: Int,
    val priceMinor: Long,
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

    fun lineTotalMinor(): Long = priceMinor * quantity
}
