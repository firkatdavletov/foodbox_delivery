package ru.foodbox.delivery.data.entities

import jakarta.persistence.*
import ru.foodbox.delivery.data.DeliveryType
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class OrderEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserEntity?,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItemEntity> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    var deliveryType: DeliveryType,

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    var customerType: OrderCustomerType = OrderCustomerType.AUTHORIZED,

    @Column(name = "customer_name")
    var customerName: String? = null,

    @Column(name = "customer_phone")
    var customerPhone: String? = null,

    @Column(name = "customer_email")
    var customerEmail: String? = null,

    var deliveryAddress: String?,

    @Column(name = "delivery_time", nullable = true)
    var deliveryTime: LocalDateTime? = null,

    var comment: String? = null,

    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,

    @Column(name = "delivery_price", nullable = false)
    var deliveryPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "message_id", nullable = true)
    var messageId: Int? = null,
) : BaseAuditEntity<Long>() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PENDING
        private set

    fun addItem(block: OrderEntity.() -> OrderItemEntity) {
        items.add(block())
    }

    fun setItems(block: OrderEntity.() -> MutableSet<OrderItemEntity>) {
        items.clear()
        items.addAll(block())
    }

    fun cancel() {
        status = OrderStatus.CANCELLED
    }

    fun complete() {
        status = OrderStatus.COMPLETED
    }

    fun take() {
        status = OrderStatus.PROCESSING
    }

    fun pending() {
        status = OrderStatus.PENDING
    }
}
