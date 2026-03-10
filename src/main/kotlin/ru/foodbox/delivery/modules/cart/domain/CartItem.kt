package ru.foodbox.delivery.modules.cart.domain

import ru.foodbox.delivery.services.model.UnitOfMeasure
import java.math.BigDecimal

data class CartItem(
    val productId: Long,
    val title: String,
    var quantity: Int,
    val price: BigDecimal,
    val countStep: Int,
    val unit: UnitOfMeasure
) {
    init {
        require(quantity > 0) { "Quantity must be greater than zero" }
    }

    fun changeQuantity(newQuantity: Int) {
        require(newQuantity > 0) { "Quantity must be greater than zero" }
        quantity = newQuantity
    }

    fun increase(delta: Int) {
        require(delta > 0) { "Delta must be greater than zero" }
        quantity += delta
    }
}