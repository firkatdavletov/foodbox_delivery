package ru.foodbox.delivery.data.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import ru.foodbox.delivery.data.DeliveryType
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class OrderEntity(
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItemEntity> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(name = "delivery_type", nullable = false)
    var deliveryType: DeliveryType,

    var deliveryAddress: String?,

    var comment: String? = null,

    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,

    @Column(name = "delivery_price", nullable = false)
    var deliveryPrice: Double = 0.0,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Double = 0.0,

    @Column(name = "message_id", nullable = true)
    var messageId: Int? = null,
) : BaseAuditEntity<Long>() {

    fun addItem(block: OrderEntity.() -> OrderItemEntity) {
        items.add(block())
    }

    fun setItems(block: OrderEntity.() -> MutableSet<OrderItemEntity>) {
        items.clear()
        items.addAll(block())
    }

    fun updateTotalPrice() {
        totalAmount = items.sumOf { it.price * it.quantity }
    }
}
