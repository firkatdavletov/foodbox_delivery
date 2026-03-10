package ru.foodbox.delivery.modules.cart.infrastructure.persistance.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.data.entities.BaseAuditEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "cart_item")
class CartItemEntity(
    @Id
    @Column(nullable = false)
    val id: UUID,

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    val cart: CartEntity,

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = true)
    var product: ProductEntity,

    var quantity: Int,

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),
)