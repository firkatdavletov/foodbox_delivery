package ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "orders",
    indexes = [
        Index(name = "idx_orders_current_status_id", columnList = "current_status_id"),
    ],
)
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_status_id", nullable = false)
    var currentStatus: OrderStatusDefinitionEntity,

    @Column(columnDefinition = "text")
    var comment: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_code", length = 32)
    var paymentMethodCode: PaymentMethodCode? = null,

    @Column(name = "payment_method_name", length = 255)
    var paymentMethodName: String? = null,

    @Column(name = "subtotal_minor", nullable = false)
    var subtotalMinor: Long,

    @Column(name = "delivery_fee_minor", nullable = false)
    var deliveryFeeMinor: Long,

    @Column(name = "total_minor", nullable = false)
    var totalMinor: Long,

    @Column(name = "status_changed_at", nullable = false)
    var statusChangedAt: Instant,

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

    @OneToOne(
        mappedBy = "order",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var delivery: OrderDeliverySnapshotEntity? = null,
)
