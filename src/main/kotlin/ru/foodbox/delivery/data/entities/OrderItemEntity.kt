package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_items")
class OrderItemEntity(
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    val order: OrderEntity,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    val price: Double,
) : BaseEntity<Long>()
