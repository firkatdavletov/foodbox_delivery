package ru.foodbox.delivery.modules.cart.application.policy

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.cart.domain.Cart

@Component
class SumQuantityCartMergePolicy : CartMergePolicy {
    override fun merge(source: Cart, target: Cart): Cart {
        source.items.forEach { sourceItem ->
            val targetItem = target.items.firstOrNull { it.productId == sourceItem.productId }
            if (targetItem == null) {
                target.items += sourceItem.copy()
            } else {
                targetItem.increase(sourceItem.quantity)
            }
        }
        return target
    }
}