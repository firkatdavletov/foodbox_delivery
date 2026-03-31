package ru.foodbox.delivery.modules.cart.application.policy

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.cart.domain.Cart
import java.time.Instant

@Component
class SumQuantityCartMergePolicy : CartMergePolicy {
    override fun merge(source: Cart, target: Cart): Cart {
        source.items.forEach { sourceItem ->
            val existing = target.items.firstOrNull {
                it.hasSameConfiguration(
                    productId = sourceItem.productId,
                    variantId = sourceItem.variantId,
                    modifiers = sourceItem.modifiers,
                )
            }
            if (existing == null) {
                target.items += sourceItem.copy()
            } else {
                existing.increase(sourceItem.quantity)
            }
        }
        val now = Instant.now()
        target.deliveryDraft = when {
            target.deliveryDraft == null -> source.deliveryDraft?.invalidateQuote(now)
            else -> target.deliveryDraft?.invalidateQuote(now)
        }
        target.recalculateTotalPrice(now)
        target.updatedAt = now
        return target
    }
}
