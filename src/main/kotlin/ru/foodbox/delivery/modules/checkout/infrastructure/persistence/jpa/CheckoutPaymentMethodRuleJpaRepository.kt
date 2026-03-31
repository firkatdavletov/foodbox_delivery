package ru.foodbox.delivery.modules.checkout.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.checkout.infrastructure.persistence.entity.CheckoutPaymentMethodRuleEntity
import java.util.UUID

interface CheckoutPaymentMethodRuleJpaRepository : JpaRepository<CheckoutPaymentMethodRuleEntity, UUID> {
    fun findAllByOrderByDeliveryMethodAscSortOrderAscPaymentMethodCodeAsc(): List<CheckoutPaymentMethodRuleEntity>
}
