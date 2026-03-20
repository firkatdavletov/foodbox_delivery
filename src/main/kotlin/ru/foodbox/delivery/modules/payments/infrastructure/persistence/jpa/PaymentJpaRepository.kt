package ru.foodbox.delivery.modules.payments.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import ru.foodbox.delivery.modules.payments.infrastructure.persistence.entity.PaymentEntity
import java.util.UUID

interface PaymentJpaRepository : JpaRepository<PaymentEntity, UUID> {
    fun findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
        orderId: UUID,
        statuses: Collection<PaymentStatus>,
    ): PaymentEntity?

    fun findFirstByOrderIdOrderByCreatedAtDesc(orderId: UUID): PaymentEntity?
}
