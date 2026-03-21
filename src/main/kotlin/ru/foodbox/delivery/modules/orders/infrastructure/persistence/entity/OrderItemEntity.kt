package ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.util.UUID

@Entity
@Table(name = "order_items")
class OrderItemEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    var order: OrderEntity,

    @Column(name = "product_id", nullable = false)
    var productId: UUID,

    @Column(name = "variant_id")
    var variantId: UUID? = null,

    @Column(length = 255)
    var sku: String? = null,

    @Column(nullable = false, length = 255)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var unit: ProductUnit,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "price_minor", nullable = false)
    var priceMinor: Long,

    @Column(name = "total_minor", nullable = false)
    var totalMinor: Long,
)
