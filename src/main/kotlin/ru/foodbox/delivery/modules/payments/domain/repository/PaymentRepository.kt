package ru.foodbox.delivery.modules.payments.domain.repository

import ru.foodbox.delivery.modules.payments.domain.Payment
import java.util.UUID

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(paymentId: UUID): Payment?
    fun findActiveByOrderId(orderId: UUID): Payment?
    fun findLatestByOrderId(orderId: UUID): Payment?
}
