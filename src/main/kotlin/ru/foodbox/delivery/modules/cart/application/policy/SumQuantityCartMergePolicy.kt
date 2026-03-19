package ru.foodbox.delivery.modules.cart.application.policy

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.cart.domain.Cart
import java.time.Instant

@Component
class SumQuantityCartMergePolicy : CartMergePolicy {
    override fun merge(source: Cart, target: Cart): Cart {
        source.items.forEach { sourceItem ->
            val existing = target.items.firstOrNull {
                it.productId == sourceItem.productId && it.variantId == sourceItem.variantId
            }
            if (existing == null) {
                target.items += sourceItem.copy()
            } else {
                existing.increase(sourceItem.quantity)
            }
        }
        val now = Instant.now()
        target.totalPriceMinor = target.items.sumOf { it.lineTotalMinor() }
        target.deliveryDraft = when {
            target.deliveryDraft == null -> source.deliveryDraft?.invalidateQuote(now)
            else -> target.deliveryDraft?.invalidateQuote(now)
        }
        target.updatedAt = now
        return target
    }
}
