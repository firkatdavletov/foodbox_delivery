package ru.foodbox.delivery.data.entities

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

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