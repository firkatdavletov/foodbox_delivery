package ru.foodbox.delivery.modules.cart.domain

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CartDeliveryDraftTest {

    @Test
    fun `includes delivery quote price in total price`() {
        val now = Instant.now()
        val cart = Cart(
            id = UUID.randomUUID(),
            owner = CartOwner(CartOwnerType.INSTALLATION, "install-1"),
            status = CartStatus.ACTIVE,
            items = mutableListOf(
                CartItem(
                    productId = UUID.randomUUID(),
                    variantId = null,
                    title = "T-Shirt",
                    unit = ProductUnit.PIECE,
                    countStep = 1,
                    quantity = 1,
                    priceMinor = 199_900,
                )
            ),
            deliveryDraft = null,
            totalPriceMinor = 199_900,
            createdAt = now,
            updatedAt = now,
        )

        cart.upsertDeliveryDraft(
            CartDeliveryDraft(
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryAddress = null,
                pickupPointId = null,
                pickupPointExternalId = null,
                pickupPointName = null,
                pickupPointAddress = null,
                quote = CartDeliveryQuote(
                    available = true,
                    priceMinor = 29_900,
                    currency = "RUB",
                    zoneCode = "EKB",
                    zoneName = "Yekaterinburg",
                    estimatedDays = 1,
                    message = null,
                    calculatedAt = now,
                    expiresAt = now.plusSeconds(900),
                ),
                createdAt = now,
                updatedAt = now,
            )
        )

        assertEquals(229_800, cart.totalPriceMinor)
    }

    @Test
    fun `invalidates delivery quote when cart items change`() {
        val now = Instant.now()
        val cart = Cart(
            id = UUID.randomUUID(),
            owner = CartOwner(CartOwnerType.INSTALLATION, "install-1"),
            status = CartStatus.ACTIVE,
            items = mutableListOf(
                CartItem(
                    productId = UUID.randomUUID(),
                    variantId = null,
                    title = "T-Shirt",
                    unit = ProductUnit.PIECE,
                    countStep = 1,
                    quantity = 1,
                    priceMinor = 199_900,
                )
            ),
            deliveryDraft = CartDeliveryDraft(
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryAddress = null,
                pickupPointId = null,
                pickupPointExternalId = null,
                pickupPointName = null,
                pickupPointAddress = null,
                quote = CartDeliveryQuote(
                    available = true,
                    priceMinor = 29_900,
                    currency = "RUB",
                    zoneCode = "EKB",
                    zoneName = "Yekaterinburg",
                    estimatedDays = 1,
                    message = null,
                    calculatedAt = now,
                    expiresAt = now.plusSeconds(900),
                ),
                createdAt = now,
                updatedAt = now,
            ),
            totalPriceMinor = 199_900,
            createdAt = now,
            updatedAt = now,
        )

        cart.addItem(
            CartItem(
                productId = UUID.randomUUID(),
                variantId = null,
                title = "Jeans",
                unit = ProductUnit.PIECE,
                countStep = 1,
                quantity = 1,
                priceMinor = 299_900,
            )
        )

        assertNull(cart.deliveryDraft?.quote)
        assertEquals(499_800, cart.totalPriceMinor)
    }
}
