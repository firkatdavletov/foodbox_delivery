package ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliveryType
import ru.foodbox.delivery.modules.orders.domain.OrderStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "orders")
class OrderEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    var orderNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 32)
    var customerType: OrderCustomerType,

    @Column(name = "user_id")
    var userId: UUID? = null,

    @Column(name = "guest_install_id", length = 255)
    var guestInstallId: String? = null,

    @Column(name = "customer_name", length = 255)
    var customerName: String? = null,

    @Column(name = "customer_phone", length = 32)
    var customerPhone: String? = null,

    @Column(name = "customer_email", length = 255)
    var customerEmail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: OrderStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 32)
    var deliveryType: OrderDeliveryType,

    @Column(name = "delivery_address", columnDefinition = "text")
    var deliveryAddress: String? = null,

    @Column(columnDefinition = "text")
    var comment: String? = null,

    @Column(name = "subtotal_minor", nullable = false)
    var subtotalMinor: Long,

    @Column(name = "delivery_fee_minor", nullable = false)
    var deliveryFeeMinor: Long,

    @Column(name = "total_minor", nullable = false)
    var totalMinor: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @OneToMany(
        mappedBy = "order",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var items: MutableList<OrderItemEntity> = mutableListOf(),
)
