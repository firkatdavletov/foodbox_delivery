package ru.foodbox.delivery.modules.cart.application.policy

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.cart.domain.Cart

@Component
class SumQuantityCartMergePolicy : CartMergePolicy {
    override fun merge(source: Cart, target: Cart): Cart {
        source.items.forEach { sourceItem ->
            val existing = target.items.firstOrNull { it.productId == sourceItem.productId }
            if (existing == null) {
                target.items += sourceItem.copy()
            } else {
                existing.increase(sourceItem.quantity)
            }
        }
        target.totalPriceMinor = target.items.sumOf { it.lineTotalMinor() }
        return target
    }
}
