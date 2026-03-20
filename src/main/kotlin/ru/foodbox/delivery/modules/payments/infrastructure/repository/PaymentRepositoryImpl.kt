package ru.foodbox.delivery.modules.payments.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.payments.domain.Payment
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import ru.foodbox.delivery.modules.payments.domain.repository.PaymentRepository
import ru.foodbox.delivery.modules.payments.infrastructure.persistence.entity.PaymentEntity
import ru.foodbox.delivery.modules.payments.infrastructure.persistence.jpa.PaymentJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class PaymentRepositoryImpl(
    private val jpaRepository: PaymentJpaRepository,
) : PaymentRepository {

    private val activeStatuses = PaymentStatus.entries.filterNot { it.isTerminal() }

    override fun save(payment: Payment): Payment {
        val existing = jpaRepository.findById(payment.id).getOrNull()
        val entity = existing ?: PaymentEntity(
            id = payment.id,
            orderId = payment.orderId,
            paymentMethodCode = payment.paymentMethodCode,
            paymentMethodName = payment.paymentMethodName,
            status = payment.status,
            amountMinor = payment.amountMinor,
            currency = payment.currency,
            providerCode = payment.providerCode,
            externalPaymentId = payment.externalPaymentId,
            confirmationUrl = payment.confirmationUrl,
            details = payment.details,
            createdAt = payment.createdAt,
            updatedAt = payment.updatedAt,
            paidAt = payment.paidAt,
        )

        entity.orderId = payment.orderId
        entity.paymentMethodCode = payment.paymentMethodCode
        entity.paymentMethodName = payment.paymentMethodName
        entity.status = payment.status
        entity.amountMinor = payment.amountMinor
        entity.currency = payment.currency
        entity.providerCode = payment.providerCode
        entity.externalPaymentId = payment.externalPaymentId
        entity.confirmationUrl = payment.confirmationUrl
        entity.details = payment.details
        entity.updatedAt = payment.updatedAt
        entity.paidAt = payment.paidAt

        return toDomain(jpaRepository.save(entity))
    }

    override fun findById(paymentId: UUID): Payment? {
        return jpaRepository.findById(paymentId).getOrNull()?.let(::toDomain)
    }

    override fun findActiveByOrderId(orderId: UUID): Payment? {
        return jpaRepository.findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(orderId, activeStatuses)?.let(::toDomain)
    }

    override fun findLatestByOrderId(orderId: UUID): Payment? {
        return jpaRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)?.let(::toDomain)
    }

    private fun toDomain(entity: PaymentEntity): Payment {
        return Payment(
            id = entity.id,
            orderId = entity.orderId,
            paymentMethodCode = entity.paymentMethodCode,
            paymentMethodName = entity.paymentMethodName,
            status = entity.status,
            amountMinor = entity.amountMinor,
            currency = entity.currency,
            providerCode = entity.providerCode,
            externalPaymentId = entity.externalPaymentId,
            confirmationUrl = entity.confirmationUrl,
            details = entity.details,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            paidAt = entity.paidAt,
        )
    }
}
