package ru.foodbox.delivery.data.entities

import jakarta.persistence.*
import ru.foodbox.delivery.services.model.UnitOfMeasure
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItemEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: OrderEntity,

    @Column(name = "product_id", nullable = false)
    var productId: Long,

    @Column(name = "image_url")
    var imageUrl: String?,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    var unit: UnitOfMeasure,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false, precision = 19, scale = 2)
    var price: BigDecimal,

    @Column(nullable = false, precision = 19, scale = 2)
    var totalPrice: BigDecimal,
) : BaseEntity<Long>()
