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
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogProductModifiersService
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierOptionRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ProductModifierGroupRepository
import ru.foodbox.delivery.modules.cart.modifier.application.CartItemModifierResolver
import ru.foodbox.delivery.modules.delivery.application.DeliveryAddressGeocoder
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
    fun `detects courier delivery draft by coordinates and uses geocoded address`() {
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
                    quantity = 2,
                    priceMinor = 1_000,
                )
            ),
            deliveryDraft = null,
            totalPriceMinor = 2_000,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val cartRepository = InMemoryCartRepository(cart)
        val deliveryService = CapturingDeliveryService(
            quoteToReturn = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = true,
                priceMinor = 500,
                currency = "RUB",
                zoneCode = "EKB",
                zoneName = "Yekaterinburg",
                estimatedDays = 1,
            )
        )
        val geocoder = StubDeliveryAddressGeocoder(
            address = DeliveryAddress(
                city = "Yekaterinburg",
                street = "Lenina",
                house = "1",
            )
        )
        val service = CartServiceImpl(
            cartRepository = cartRepository,
            productReadService = UnusedProductReadService(),
            cartMergePolicy = PassthroughCartMergePolicy(),
            cartItemModifierResolver = unusedCartItemModifierResolver(),
            deliveryService = deliveryService,
            deliveryAddressGeocoder = geocoder,
        )

        val draft = service.detectCourierDeliveryDraft(
            actor = actor,
            latitude = 56.8389,
            longitude = 60.6057,
        )

        assertEquals(56.8389, geocoder.lastLatitude)
        assertEquals(60.6057, geocoder.lastLongitude)
        assertEquals(DeliveryMethodType.COURIER, draft.deliveryMethod)
        assertEquals("Yekaterinburg", draft.deliveryAddress?.city)
        assertEquals("Lenina", draft.deliveryAddress?.street)
        assertEquals("1", draft.deliveryAddress?.house)
        assertEquals(56.8389, draft.deliveryAddress?.latitude)
        assertEquals(60.6057, draft.deliveryAddress?.longitude)
        assertEquals(2_000L, deliveryService.lastContext?.subtotalMinor)
        assertEquals(2, deliveryService.lastContext?.itemCount)
        assertEquals(500L, draft.quote?.priceMinor)
        assertEquals(2_500L, cartRepository.savedCart?.totalPriceMinor)
    }

    @Test
    fun `detects courier delivery draft for empty cart`() {
        val actor = CurrentActor.Guest("install-1")
        val cart = Cart(
            id = UUID.randomUUID(),
            owner = CartOwner(CartOwnerType.INSTALLATION, actor.installId),
            status = CartStatus.ACTIVE,
            items = mutableListOf(),
            deliveryDraft = null,
            totalPriceMinor = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val cartRepository = InMemoryCartRepository(cart)
        val deliveryService = CapturingDeliveryService(
            quoteToReturn = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = true,
                priceMinor = 500,
                currency = "RUB",
                zoneCode = "EKB",
                zoneName = "Yekaterinburg",
                estimatedDays = 1,
            )
        )
        val geocoder = StubDeliveryAddressGeocoder(
            address = DeliveryAddress(
                city = "Yekaterinburg",
                street = "Lenina",
                house = "1",
            )
        )
        val service = CartServiceImpl(
            cartRepository = cartRepository,
            productReadService = UnusedProductReadService(),
            cartMergePolicy = PassthroughCartMergePolicy(),
            cartItemModifierResolver = unusedCartItemModifierResolver(),
            deliveryService = deliveryService,
            deliveryAddressGeocoder = geocoder,
        )

        val draft = service.detectCourierDeliveryDraft(
            actor = actor,
            latitude = 56.8389,
            longitude = 60.6057,
        )

        assertEquals(0L, deliveryService.lastContext?.subtotalMinor)
        assertEquals(0, deliveryService.lastContext?.itemCount)
        assertEquals(500L, draft.quote?.priceMinor)
        assertEquals(500L, cartRepository.savedCart?.totalPriceMinor)
    }

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
            cartItemModifierResolver = unusedCartItemModifierResolver(),
            deliveryService = deliveryService,
            deliveryAddressGeocoder = StubDeliveryAddressGeocoder(),
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

    @Test
    fun `updates delivery draft for empty cart`() {
        val actor = CurrentActor.Guest("install-1")
        val cart = Cart(
            id = UUID.randomUUID(),
            owner = CartOwner(CartOwnerType.INSTALLATION, actor.installId),
            status = CartStatus.ACTIVE,
            items = mutableListOf(),
            deliveryDraft = null,
            totalPriceMinor = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
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
            cartItemModifierResolver = unusedCartItemModifierResolver(),
            deliveryService = deliveryService,
            deliveryAddressGeocoder = StubDeliveryAddressGeocoder(),
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

        assertEquals(0L, deliveryService.lastContext?.subtotalMinor)
        assertEquals(0, deliveryService.lastContext?.itemCount)
        assertEquals(600L, deliveryDraft.quote?.priceMinor)
        assertEquals(600L, cartRepository.savedCart?.totalPriceMinor)
    }

    @Test
    fun `refreshes expired delivery quote when getting cart`() {
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
                    calculatedAt = now.minusSeconds(1_800),
                    expiresAt = now.minusSeconds(60),
                ),
                createdAt = now.minusSeconds(1_800),
                updatedAt = now.minusSeconds(1_800),
            ),
            totalPriceMinor = 1_450,
            createdAt = now.minusSeconds(1_800),
            updatedAt = now.minusSeconds(1_800),
        )
        val cartRepository = InMemoryCartRepository(cart)
        val deliveryService = CapturingDeliveryService(
            quoteToReturn = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = true,
                priceMinor = 200,
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
            cartItemModifierResolver = unusedCartItemModifierResolver(),
            deliveryService = deliveryService,
            deliveryAddressGeocoder = StubDeliveryAddressGeocoder(),
        )

        val refreshedCart = service.getOrCreateActiveCart(actor)

        assertEquals(1_000L, deliveryService.lastContext?.subtotalMinor)
        assertEquals(1_200L, refreshedCart.totalPriceMinor)
        assertEquals(200L, refreshedCart.deliveryDraft?.quote?.priceMinor)
        assertEquals(1_200L, cartRepository.savedCart?.totalPriceMinor)
    }

    @Test
    fun `refreshes expired delivery quote for empty cart`() {
        val now = Instant.now()
        val actor = CurrentActor.Guest("install-1")
        val cart = Cart(
            id = UUID.randomUUID(),
            owner = CartOwner(CartOwnerType.INSTALLATION, actor.installId),
            status = CartStatus.ACTIVE,
            items = mutableListOf(),
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
                    calculatedAt = now.minusSeconds(1_800),
                    expiresAt = now.minusSeconds(60),
                ),
                createdAt = now.minusSeconds(1_800),
                updatedAt = now.minusSeconds(1_800),
            ),
            totalPriceMinor = 450,
            createdAt = now.minusSeconds(1_800),
            updatedAt = now.minusSeconds(1_800),
        )
        val cartRepository = InMemoryCartRepository(cart)
        val deliveryService = CapturingDeliveryService(
            quoteToReturn = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = true,
                priceMinor = 200,
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
            cartItemModifierResolver = unusedCartItemModifierResolver(),
            deliveryService = deliveryService,
            deliveryAddressGeocoder = StubDeliveryAddressGeocoder(),
        )

        val refreshedCart = service.getOrCreateActiveCart(actor)

        assertEquals(0L, deliveryService.lastContext?.subtotalMinor)
        assertEquals(0, deliveryService.lastContext?.itemCount)
        assertEquals(200L, refreshedCart.totalPriceMinor)
        assertEquals(200L, refreshedCart.deliveryDraft?.quote?.priceMinor)
        assertEquals(200L, cartRepository.savedCart?.totalPriceMinor)
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

    private class StubDeliveryAddressGeocoder(
        private val address: DeliveryAddress? = null,
    ) : DeliveryAddressGeocoder {
        var lastLatitude: Double? = null
        var lastLongitude: Double? = null

        override fun isConfigured(): Boolean = true

        override fun reverseGeocode(latitude: Double, longitude: Double): DeliveryAddress? {
            lastLatitude = latitude
            lastLongitude = longitude
            return address
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

    private fun unusedCartItemModifierResolver(): CartItemModifierResolver {
        return CartItemModifierResolver(
            catalogProductModifiersService = CatalogProductModifiersService(
                productModifierGroupRepository = UnusedProductModifierGroupRepository(),
                modifierGroupRepository = UnusedModifierGroupRepository(),
                modifierOptionRepository = UnusedModifierOptionRepository(),
            ),
        )
    }

    private class UnusedProductModifierGroupRepository : ProductModifierGroupRepository {
        override fun findAllByProductId(productId: UUID): List<ProductModifierGroup> = error("Not used")
        override fun findAllByProductIds(productIds: Collection<UUID>): List<ProductModifierGroup> = error("Not used")
        override fun deleteAllByProductId(productId: UUID) = error("Not used")
        override fun saveAll(productModifierGroups: List<ProductModifierGroup>): List<ProductModifierGroup> = error("Not used")
    }

    private class UnusedModifierGroupRepository : ModifierGroupRepository {
        override fun findAll(): List<ModifierGroup> = error("Not used")
        override fun findAllByIsActive(isActive: Boolean): List<ModifierGroup> = error("Not used")
        override fun findAllByCodes(codes: Collection<String>): List<ModifierGroup> = error("Not used")
        override fun findAllByIds(ids: Collection<UUID>): List<ModifierGroup> = error("Not used")
        override fun findById(id: UUID): ModifierGroup? = error("Not used")
        override fun findByCode(code: String): ModifierGroup? = error("Not used")
        override fun save(group: ModifierGroup): ModifierGroup = error("Not used")
    }

    private class UnusedModifierOptionRepository : ModifierOptionRepository {
        override fun findAllByGroupIds(groupIds: Collection<UUID>): List<ModifierOption> = error("Not used")
        override fun deleteAllByGroupId(groupId: UUID) = error("Not used")
        override fun saveAll(options: List<ModifierOption>): List<ModifierOption> = error("Not used")
    }
}
