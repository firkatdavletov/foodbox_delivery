package ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "cart_items")
class CartItemEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    var cart: CartEntity,

    @Column(name = "product_id", nullable = false)
    var productId: UUID,

    @Column(name = "variant_id")
    var variantId: UUID? = null,

    @Column(nullable = false, length = 255)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var unit: ProductUnit,

    @Column(name = "count_step", nullable = false)
    var countStep: Int,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "price_minor", nullable = false)
    var priceMinor: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
