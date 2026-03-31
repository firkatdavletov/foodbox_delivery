package ru.foodbox.delivery.modules.cart.pricing.application

import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.cart.domain.CartItem
import ru.foodbox.delivery.modules.cart.pricing.domain.CartItemPriceBreakdown
import ru.foodbox.delivery.modules.cart.pricing.domain.ModifierPricedSelection
import ru.foodbox.delivery.modules.cart.pricing.domain.calculateCartItemPrice

@Service
class CartItemPricingService {
    fun calculate(item: CartItem): CartItemPriceBreakdown {
        return calculateCartItemPrice(
            basePriceMinor = item.priceMinor,
            lineQuantity = item.quantity,
            modifiers = item.modifiers,
        )
    }

    fun calculate(
        basePriceMinor: Long,
        lineQuantity: Int,
        modifiers: List<ModifierPricedSelection>,
    ): CartItemPriceBreakdown {
        return calculateCartItemPrice(
            basePriceMinor = basePriceMinor,
            lineQuantity = lineQuantity,
            modifiers = modifiers,
        )
    }

    fun calculateCartTotal(items: Collection<CartItem>, deliveryFeeMinor: Long = 0L): Long {
        return items.sumOf { calculate(it).lineTotalMinor } + deliveryFeeMinor
    }
}
