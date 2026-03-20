package ru.foodbox.delivery.modules.cart.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.catalog.application.ProductReadService
import ru.foodbox.delivery.modules.catalog.domain.ProductSnapshot
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.cart.application.command.UpdateCartDeliveryCommand
import ru.foodbox.delivery.modules.cart.application.policy.CartMergePolicy
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryDraft
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryQuote
import ru.foodbox.delivery.modules.cart.domain.CartItem
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.domain.repository.CartRepository
import ru.foodbox.delivery.modules.delivery.application.DeliveryService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class CartServiceImplTest {

    @Test
    fun `uses items subtotal when recalculating delivery quote`() {
        val now = Instant.now()
        val actor = CurrentActor.Guest("install-1")
        val cart = Cart(
            id = UUID.randomUUID(),
            owner = CartOwner(CartOwnerType.INSTALLATION, actor.installId),
            status = CartStatus.ACTIVE,
            items = mutableListOf(
                CartItem(
                    productId = UUID.randomUUID(),
                    variantId = null,
                    title = "T-Shirt",
                    unit = ProductUnit.PIECE,
                    countStep = 1,
                    quantity = 1,
                    priceMinor = 1_000,
                )
            ),
            deliveryDraft = CartDeliveryDraft(
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryAddress = DeliveryAddress(
                    city = "Yekaterinburg",
                    street = "Lenina",
                    house = "1",
                ),
                pickupPointId = null,
                pickupPointExternalId = null,
                pickupPointName = null,
                pickupPointAddress = null,
                quote = CartDeliveryQuote(
                    available = true,
                    priceMinor = 450,
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
            totalPriceMinor = 1_450,
            createdAt = now,
            updatedAt = now,
        )
        val cartRepository = InMemoryCartRepository(cart)
        val deliveryService = CapturingDeliveryService(
            quoteToReturn = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = true,
                priceMinor = 600,
                currency = "RUB",
                zoneCode = "EKB",
                zoneName = "Yekaterinburg",
                estimatedDays = 1,
            )
        )
        val service = CartServiceImpl(
            cartRepository = cartRepository,
            productReadService = UnusedProductReadService(),
            cartMergePolicy = PassthroughCartMergePolicy(),
            deliveryService = deliveryService,
        )

        val deliveryDraft = service.updateDeliveryDraft(
            actor = actor,
            command = UpdateCartDeliveryCommand(
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryAddress = DeliveryAddress(
                    city = "Yekaterinburg",
                    street = "Lenina",
                    house = "1",
                ),
                pickupPointId = null,
                pickupPointExternalId = null,
            ),
        )

        assertEquals(1_000L, deliveryService.lastContext?.subtotalMinor)
        assertEquals(1_600L, cartRepository.savedCart?.totalPriceMinor)
        assertEquals(600L, deliveryDraft.quote?.priceMinor)
    }

    private class InMemoryCartRepository(
        private var activeCart: Cart?,
    ) : CartRepository {
        var savedCart: Cart? = null

        override fun findById(cartId: UUID): Cart? = activeCart?.takeIf { it.id == cartId }

        override fun findActiveByOwner(owner: CartOwner): Cart? {
            return activeCart?.takeIf { it.owner == owner && it.status == CartStatus.ACTIVE }
        }

        override fun save(cart: Cart): Cart {
            activeCart = cart
            savedCart = cart
            return cart
        }
    }

    private class CapturingDeliveryService(
        private val quoteToReturn: DeliveryQuote,
    ) : DeliveryService {
        var lastContext: DeliveryQuoteContext? = null

        override fun getAvailableMethods(): List<DeliveryMethodType> = emptyList()

        override fun getActivePickupPoints(): List<PickupPoint> = emptyList()

        override fun detectYandexLocations(query: String): List<YandexDeliveryLocationVariant> = emptyList()

        override fun getYandexPickupPoints(geoId: Long): List<YandexPickupPointOption> = emptyList()

        override fun getYandexPickupPoint(pickupPointId: String): YandexPickupPointOption? = null

        override fun calculateQuote(context: DeliveryQuoteContext): DeliveryQuote {
            lastContext = context
            return quoteToReturn
        }
    }

    private class UnusedProductReadService : ProductReadService {
        override fun getActiveProductSnapshot(productId: UUID, variantId: UUID?): ProductSnapshot? {
            error("ProductReadService is not used in this test")
        }
    }

    private class PassthroughCartMergePolicy : CartMergePolicy {
        override fun merge(source: Cart, target: Cart): Cart = target
    }
}
