package ru.foodbox.delivery.data.entities

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "cart_item")
class CartItemEntity(
    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    val cart: CartEntity,

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = true)
    var product: ProductEntity,

    var quantity: Int,
) : BaseAuditEntity<Long>()