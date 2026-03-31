package ru.foodbox.delivery.modules.cart.pricing

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.cart.pricing.application.CartItemPricingService
import ru.foodbox.delivery.modules.cart.pricing.domain.ModifierPricedSelection
import kotlin.test.assertEquals

class CartItemPricingServiceTest {

    private val pricingService = CartItemPricingService()

    @Test
    fun `calculates base per-item and per-line modifiers separately`() {
        val price = pricingService.calculate(
            basePriceMinor = 1_000,
            lineQuantity = 3,
            modifiers = listOf(
                testModifier(ModifierApplicationScope.PER_ITEM, modifierPriceMinor = 200, modifierQuantity = 1),
                testModifier(ModifierApplicationScope.PER_ITEM, modifierPriceMinor = 50, modifierQuantity = 2),
                testModifier(ModifierApplicationScope.PER_LINE, modifierPriceMinor = 300, modifierQuantity = 1),
            ),
        )

        assertEquals(300L, price.perItemModifiersMinor)
        assertEquals(300L, price.perLineModifiersMinor)
        assertEquals(1_300L, price.unitPriceMinor)
        assertEquals(4_200L, price.lineTotalMinor)
    }

    private fun testModifier(
        scope: ModifierApplicationScope,
        modifierPriceMinor: Long,
        modifierQuantity: Int,
    ): ModifierPricedSelection {
        return object : ModifierPricedSelection {
            override val applicationScope = scope
            override val priceMinor = modifierPriceMinor
            override val quantity = modifierQuantity
        }
    }
}
