package ru.foodbox.delivery.data.entities

import jakarta.persistence.*

@Entity
@Table(name = "cart_item")
class CartItemEntity(
    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    val cart: CartEntity,

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    var product: ProductEntity,

    var quantity: Int,
) : BaseAuditEntity<Long>()