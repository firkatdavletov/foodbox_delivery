package ru.foodbox.delivery.modules.payments.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payments_order_id", columnList = "order_id"),
        Index(name = "idx_payments_order_id_status", columnList = "order_id,status"),
    ],
)
class PaymentEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "order_id", nullable = false)
    var orderId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_code", nullable = false, length = 32)
    var paymentMethodCode: PaymentMethodCode,

    @Column(name = "payment_method_name", nullable = false, length = 255)
    var paymentMethodName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: PaymentStatus,

    @Column(name = "amount_minor", nullable = false)
    var amountMinor: Long,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,

    @Column(name = "provider_code", length = 64)
    var providerCode: String? = null,

    @Column(name = "external_payment_id", length = 255)
    var externalPaymentId: String? = null,

    @Column(name = "confirmation_url", length = 1000)
    var confirmationUrl: String? = null,

    @Column(columnDefinition = "text")
    var details: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column(name = "paid_at")
    var paidAt: Instant? = null,
)
