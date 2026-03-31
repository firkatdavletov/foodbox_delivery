package ru.foodbox.delivery.modules.checkout.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentMethodRule
import ru.foodbox.delivery.modules.checkout.domain.repository.CheckoutPaymentMethodRuleRepository
import ru.foodbox.delivery.modules.checkout.infrastructure.persistence.entity.CheckoutPaymentMethodRuleEntity
import ru.foodbox.delivery.modules.checkout.infrastructure.persistence.jpa.CheckoutPaymentMethodRuleJpaRepository
import java.time.Instant
import java.util.UUID

@Repository
class CheckoutPaymentMethodRuleRepositoryImpl(
    private val jpaRepository: CheckoutPaymentMethodRuleJpaRepository,
) : CheckoutPaymentMethodRuleRepository {

    override fun findAll(): List<CheckoutPaymentMethodRule> {
        return jpaRepository.findAllByOrderByDeliveryMethodAscSortOrderAscPaymentMethodCodeAsc()
            .groupBy(CheckoutPaymentMethodRuleEntity::deliveryMethod)
            .map { (deliveryMethod, entities) ->
                CheckoutPaymentMethodRule(
                    deliveryMethod = deliveryMethod,
                    paymentMethods = entities.map(CheckoutPaymentMethodRuleEntity::paymentMethodCode),
                )
            }
            .sortedBy { it.deliveryMethod.ordinal }
    }

    override fun replaceAll(rules: List<CheckoutPaymentMethodRule>) {
        jpaRepository.deleteAllInBatch()
        if (rules.isEmpty()) {
            return
        }

        val now = Instant.now()
        val entities = rules.flatMap { rule ->
            rule.paymentMethods.mapIndexed { index, paymentMethodCode ->
                CheckoutPaymentMethodRuleEntity(
                    id = UUID.randomUUID(),
                    deliveryMethod = rule.deliveryMethod,
                    paymentMethodCode = paymentMethodCode,
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        }
        jpaRepository.saveAll(entities)
    }
}
