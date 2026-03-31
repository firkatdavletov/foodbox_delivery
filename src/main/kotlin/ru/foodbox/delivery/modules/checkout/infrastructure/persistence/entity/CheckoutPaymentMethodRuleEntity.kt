package ru.foodbox.delivery.modules.checkout.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "checkout_payment_method_rules",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_checkout_payment_method_rules_delivery_method_payment_method",
            columnNames = ["delivery_method", "payment_method_code"],
        )
    ],
)
class CheckoutPaymentMethodRuleEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 32)
    var deliveryMethod: DeliveryMethodType,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_code", nullable = false, length = 32)
    var paymentMethodCode: PaymentMethodCode,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
