package ru.foodbox.delivery.modules.orders.application

import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.catalog.application.ProductReadService
import ru.foodbox.delivery.modules.catalog.domain.ProductSnapshot
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.cart.application.CartService
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemCommand
import ru.foodbox.delivery.modules.cart.application.command.ChangeCartItemQuantityCommand
import ru.foodbox.delivery.modules.cart.application.command.UpdateCartDeliveryCommand
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryDraft
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryQuote
import ru.foodbox.delivery.modules.cart.domain.CartItem
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.modifier.domain.CartItemModifier
import ru.foodbox.delivery.modules.cart.pricing.application.CartItemPricingService
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.checkout.application.CheckoutOptionsQuery
import ru.foodbox.delivery.modules.checkout.application.CheckoutService
import ru.foodbox.delivery.modules.checkout.domain.CheckoutDeliveryOption
import ru.foodbox.delivery.modules.delivery.application.DeliveryOrderRequestConfirmation
import ru.foodbox.delivery.modules.delivery.application.DeliveryOrderRequestService
import ru.foodbox.delivery.modules.delivery.application.DeliveryService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import ru.foodbox.delivery.modules.orders.application.command.CheckoutCommand
import ru.foodbox.delivery.modules.orders.application.event.OrderCreatedEvent
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.domain.OrderPaymentSnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistory
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusDefinitionRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusHistoryRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusTransitionRepository
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo
import ru.foodbox.delivery.modules.user.domain.User
import ru.foodbox.delivery.modules.user.domain.repository.UserRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OrderServiceImplTest {

    @Test
    fun `checkout revalidates cart prices and delivery before creating order`() {
        val productId = UUID.randomUUID()
        val cart = activeCart(
            items = mutableListOf(
                CartItem(
                    productId = productId,
                    variantId = null,
                    title = "Stale title",
                    unit = ProductUnit.PIECE,
                    countStep = 1,
                    quantity = 2,
                    priceMinor = 9_900,
                )
            ),
            deliveryDraft = courierDraft(),
        )
        val orderRepository = InMemoryOrderRepository()
        val deliveryService = RecordingDeliveryService(
            quote = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = true,
                priceMinor = 3_500,
                currency = "RUB",
                zoneCode = "city",
                zoneName = "City",
                estimatedDays = 1,
                estimatesMinutes = 60,
            ),
        )
        val cartService = StubCartService(cart)
        val deliveryOrderRequestService = RecordingDeliveryOrderRequestService()
        val eventPublisher = RecordingApplicationEventPublisher()
        val orderStatusService = createOrderStatusService(orderRepository, eventPublisher)
        val service = OrderServiceImpl(
            orderRepository = orderRepository,
            cartService = cartService,
            productReadService = StubProductReadService(
                snapshots = mapOf(
                    productId to ProductSnapshot(
                        id = productId,
                        variantId = null,
                        sku = "SKU-COURIER-1",
                        title = "Fresh title",
                        unit = ProductUnit.PIECE,
                        countStep = 1,
                        priceMinor = 12_500,
                    )
                )
            ),
            userRepository = StubUserRepository(),
            deliveryService = deliveryService,
            cartItemPricingService = CartItemPricingService(),
            checkoutService = StubCheckoutService(
                options = listOf(
                    checkoutDeliveryOption(
                        deliveryMethod = DeliveryMethodType.COURIER,
                        paymentMethods = listOf(PaymentMethodCode.CARD_ON_DELIVERY),
                    )
                )
            ),
            deliveryOrderRequestService = deliveryOrderRequestService,
            applicationEventPublisher = eventPublisher,
            orderStatusService = orderStatusService,
        )

        val order = service.checkout(
            actor = CurrentActor.Guest("install-1"),
            command = CheckoutCommand(
                paymentMethodCode = PaymentMethodCode.CARD_ON_DELIVERY,
                customerName = "Guest",
                customerPhone = "+79990000000",
                customerEmail = "guest@example.com",
                deliveryAddress = null,
                comment = "Comment",
            ),
        )

        val savedOrder = orderRepository.savedOrders.single()
        assertEquals(25_000L, savedOrder.subtotalMinor)
        assertEquals(3_500L, savedOrder.deliveryFeeMinor)
        assertEquals(28_500L, savedOrder.totalMinor)
        assertEquals("Fresh title", savedOrder.items.single().title)
        assertEquals("SKU-COURIER-1", savedOrder.items.single().sku)
        assertEquals(12_500L, savedOrder.items.single().priceMinor)
        assertEquals("PENDING", savedOrder.currentStatus.code)
        assertEquals(
            OrderPaymentSnapshot(
                methodCode = PaymentMethodCode.CARD_ON_DELIVERY,
                methodName = PaymentMethodCode.CARD_ON_DELIVERY.displayName,
            ),
            savedOrder.payment,
        )
        assertEquals(25_000L, deliveryService.contexts.single().subtotalMinor)
        assertEquals(2, deliveryService.contexts.single().itemCount)
        assertNotNull(deliveryOrderRequestService.lastOrder)
        assertEquals(DeliveryMethodType.COURIER, deliveryOrderRequestService.lastOrder?.delivery?.method)
        assertEquals(60, savedOrder.delivery.estimatesMinutes)
        assertEquals(cart.id, cartService.markedOrderedCartId)
        assertEquals(1, eventPublisher.events.size)
        assertEquals(order.id, (eventPublisher.events.single() as OrderCreatedEvent).order.id)
    }

    @Test
    fun `checkout confirms yandex offer and updates delivery price and status`() {
        val productId = UUID.randomUUID()
        val cart = activeCart(
            items = mutableListOf(
                CartItem(
                    productId = productId,
                    variantId = null,
                    title = "Cart title",
                    unit = ProductUnit.PIECE,
                    countStep = 1,
                    quantity = 1,
                    priceMinor = 10_000,
                )
            ),
            deliveryDraft = yandexDraft(),
        )
        val orderRepository = InMemoryOrderRepository()
        val deliveryService = RecordingDeliveryService(
            quote = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.YANDEX_PICKUP_POINT,
                available = true,
                priceMinor = 4_000,
                currency = "RUB",
                estimatedDays = 2,
                pickupPointExternalId = "pickup-point-1",
                pickupPointName = "Yandex Point 1",
                pickupPointAddress = "Lenina, 1",
            ),
        )
        val deliveryOrderRequestService = RecordingDeliveryOrderRequestService(
            confirmation = DeliveryOrderRequestConfirmation(
                externalOfferId = "offer-1",
                externalRequestId = "request-1",
                deliveryFeeMinor = 5_500,
                currency = "RUB",
            ),
        )
        val cartService = StubCartService(cart)
        val eventPublisher = RecordingApplicationEventPublisher()
        val orderStatusService = createOrderStatusService(orderRepository, eventPublisher)
        val service = OrderServiceImpl(
            orderRepository = orderRepository,
            cartService = cartService,
            productReadService = StubProductReadService(
                snapshots = mapOf(
                    productId to ProductSnapshot(
                        id = productId,
                        variantId = null,
                        sku = "SKU-YANDEX-1",
                        title = "Fresh Yandex title",
                        unit = ProductUnit.PIECE,
                        countStep = 1,
                        priceMinor = 18_000,
                    )
                )
            ),
            userRepository = StubUserRepository(),
            deliveryService = deliveryService,
            cartItemPricingService = CartItemPricingService(),
            checkoutService = StubCheckoutService(
                options = listOf(
                    checkoutDeliveryOption(
                        deliveryMethod = DeliveryMethodType.YANDEX_PICKUP_POINT,
                        paymentMethods = listOf(PaymentMethodCode.CARD_ON_DELIVERY),
                    )
                )
            ),
            deliveryOrderRequestService = deliveryOrderRequestService,
            applicationEventPublisher = eventPublisher,
            orderStatusService = orderStatusService,
        )

        val order = service.checkout(
            actor = CurrentActor.Guest("install-1"),
            command = CheckoutCommand(
                paymentMethodCode = PaymentMethodCode.CARD_ON_DELIVERY,
                customerName = "Guest",
                customerPhone = "+79990000000",
                customerEmail = "guest@example.com",
                deliveryAddress = null,
                comment = null,
            ),
        )

        assertEquals(3, orderRepository.savedOrders.size)
        val firstSave = orderRepository.savedOrders[0]
        val secondSave = orderRepository.savedOrders[1]
        val thirdSave = orderRepository.savedOrders[2]
        assertEquals("PENDING", firstSave.currentStatus.code)
        assertEquals(4_000L, firstSave.deliveryFeeMinor)
        assertEquals(22_000L, firstSave.totalMinor)
        assertEquals("PENDING", secondSave.currentStatus.code)
        assertEquals(5_500L, secondSave.deliveryFeeMinor)
        assertEquals(23_500L, secondSave.totalMinor)
        assertNotNull(deliveryOrderRequestService.lastOrder)
        assertEquals(firstSave.id, deliveryOrderRequestService.lastOrder?.id)
        assertEquals("SKU-YANDEX-1", deliveryOrderRequestService.lastOrder?.items?.single()?.sku)
        assertEquals(
            OrderPaymentSnapshot(
                methodCode = PaymentMethodCode.CARD_ON_DELIVERY,
                methodName = PaymentMethodCode.CARD_ON_DELIVERY.displayName,
            ),
            deliveryOrderRequestService.lastOrder?.payment,
        )
        assertEquals("CONFIRMED", thirdSave.currentStatus.code)
        assertEquals(5_500L, thirdSave.deliveryFeeMinor)
        assertEquals(23_500L, thirdSave.totalMinor)
        assertEquals("RUB", thirdSave.delivery.currency)
        assertEquals("CONFIRMED", order.currentStatus.code)
        assertEquals(cart.id, cartService.markedOrderedCartId)
        assertEquals(2, eventPublisher.events.size)
    }

    @Test
    fun `checkout copies cart item modifiers and includes them into subtotal`() {
        val productId = UUID.randomUUID()
        val cart = activeCart(
            items = mutableListOf(
                CartItem(
                    productId = productId,
                    variantId = null,
                    title = "Coffee",
                    unit = ProductUnit.PIECE,
                    countStep = 1,
                    quantity = 2,
                    priceMinor = 9_500,
                    modifiers = listOf(
                        CartItemModifier(
                            modifierGroupId = UUID.randomUUID(),
                            modifierOptionId = UUID.randomUUID(),
                            groupCodeSnapshot = "extra_shot",
                            groupNameSnapshot = "Extra shot",
                            optionCodeSnapshot = "one_more",
                            optionNameSnapshot = "One more shot",
                            applicationScopeSnapshot = ModifierApplicationScope.PER_ITEM,
                            priceSnapshot = 500,
                            quantity = 1,
                        ),
                        CartItemModifier(
                            modifierGroupId = UUID.randomUUID(),
                            modifierOptionId = UUID.randomUUID(),
                            groupCodeSnapshot = "gift",
                            groupNameSnapshot = "Gift",
                            optionCodeSnapshot = "card",
                            optionNameSnapshot = "Card",
                            applicationScopeSnapshot = ModifierApplicationScope.PER_LINE,
                            priceSnapshot = 300,
                            quantity = 1,
                        ),
                    ),
                )
            ),
            deliveryDraft = courierDraft(),
        )
        val orderRepository = InMemoryOrderRepository()
        val deliveryService = RecordingDeliveryService(
            quote = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = true,
                priceMinor = 1_000,
                currency = "RUB",
                zoneCode = "city",
                zoneName = "City",
                estimatedDays = 1,
            ),
        )
        val eventPublisher = RecordingApplicationEventPublisher()
        val orderStatusService = createOrderStatusService(orderRepository, eventPublisher)
        val service = OrderServiceImpl(
            orderRepository = orderRepository,
            cartService = StubCartService(cart),
            productReadService = StubProductReadService(
                snapshots = mapOf(
                    productId to ProductSnapshot(
                        id = productId,
                        variantId = null,
                        sku = "SKU-COFFEE-1",
                        title = "Coffee",
                        unit = ProductUnit.PIECE,
                        countStep = 1,
                        priceMinor = 10_000,
                    )
                ),
            ),
            userRepository = StubUserRepository(),
            deliveryService = deliveryService,
            cartItemPricingService = CartItemPricingService(),
            checkoutService = StubCheckoutService(
                options = listOf(
                    checkoutDeliveryOption(
                        deliveryMethod = DeliveryMethodType.COURIER,
                        paymentMethods = listOf(PaymentMethodCode.CARD_ON_DELIVERY),
                    )
                )
            ),
            deliveryOrderRequestService = RecordingDeliveryOrderRequestService(),
            applicationEventPublisher = eventPublisher,
            orderStatusService = orderStatusService,
        )

        val order = service.checkout(
            actor = CurrentActor.Guest("install-1"),
            command = CheckoutCommand(
                paymentMethodCode = PaymentMethodCode.CARD_ON_DELIVERY,
                customerName = "Guest",
                customerPhone = "+79990000000",
                customerEmail = "guest@example.com",
                deliveryAddress = null,
                comment = null,
            ),
        )

        assertEquals(21_300L, order.subtotalMinor)
        assertEquals(22_300L, order.totalMinor)
        assertEquals(2, order.items.single().modifiers.size)
        assertEquals("extra_shot", order.items.single().modifiers.first().groupCodeSnapshot)
        assertEquals("card", order.items.single().modifiers.last().optionCodeSnapshot)
    }

    @Test
    fun `checkout saves delivery address details into cart draft and order`() {
        val productId = UUID.randomUUID()
        val cart = activeCart(
            items = mutableListOf(
                CartItem(
                    productId = productId,
                    variantId = null,
                    title = "Pizza",
                    unit = ProductUnit.PIECE,
                    countStep = 1,
                    quantity = 1,
                    priceMinor = 15_000,
                )
            ),
            deliveryDraft = courierDraft().copy(
                deliveryAddress = DeliveryAddress(
                    city = "Yekaterinburg",
                    street = "Lenina",
                    house = "1",
                    apartment = "12",
                ),
            ),
        )
        val orderRepository = InMemoryOrderRepository()
        val deliveryService = RecordingDeliveryService(
            quote = DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = true,
                priceMinor = 1_500,
                currency = "RUB",
                zoneCode = "city",
                zoneName = "City",
                estimatedDays = 1,
                estimatesMinutes = 45,
            ),
        )
        val cartService = StubCartService(cart)
        val eventPublisher = RecordingApplicationEventPublisher()
        val orderStatusService = createOrderStatusService(orderRepository, eventPublisher)
        val service = OrderServiceImpl(
            orderRepository = orderRepository,
            cartService = cartService,
            productReadService = StubProductReadService(
                snapshots = mapOf(
                    productId to ProductSnapshot(
                        id = productId,
                        variantId = null,
                        sku = "SKU-PIZZA-1",
                        title = "Pizza",
                        unit = ProductUnit.PIECE,
                        countStep = 1,
                        priceMinor = 15_000,
                    )
                )
            ),
            userRepository = StubUserRepository(),
            deliveryService = deliveryService,
            cartItemPricingService = CartItemPricingService(),
            checkoutService = StubCheckoutService(
                options = listOf(
                    checkoutDeliveryOption(
                        deliveryMethod = DeliveryMethodType.COURIER,
                        paymentMethods = listOf(PaymentMethodCode.CARD_ON_DELIVERY),
                    )
                )
            ),
            deliveryOrderRequestService = RecordingDeliveryOrderRequestService(),
            applicationEventPublisher = eventPublisher,
            orderStatusService = orderStatusService,
        )

        val order = service.checkout(
            actor = CurrentActor.Guest("install-1"),
            command = CheckoutCommand(
                paymentMethodCode = PaymentMethodCode.CARD_ON_DELIVERY,
                customerName = "Guest",
                customerPhone = "+79990000000",
                customerEmail = "guest@example.com",
                deliveryAddress = DeliveryAddress(
                    entrance = "2",
                    floor = "4",
                    intercom = "45K",
                    comment = "Call before arrival",
                ),
                comment = null,
            ),
        )

        assertEquals(1, cartService.updatedDeliveryCommands.size)
        assertEquals("2", cartService.updatedDeliveryCommands.single().deliveryAddress?.entrance)
        assertEquals("4", cartService.updatedDeliveryCommands.single().deliveryAddress?.floor)
        assertEquals("45K", cartService.updatedDeliveryCommands.single().deliveryAddress?.intercom)
        assertEquals("Call before arrival", cartService.updatedDeliveryCommands.single().deliveryAddress?.comment)
        assertEquals("2", cart.deliveryDraft?.deliveryAddress?.entrance)
        assertEquals("4", cart.deliveryDraft?.deliveryAddress?.floor)
        assertEquals("45K", cart.deliveryDraft?.deliveryAddress?.intercom)
        assertEquals("Call before arrival", cart.deliveryDraft?.deliveryAddress?.comment)
        assertEquals("2", deliveryService.contexts.single().deliveryAddress?.entrance)
        assertEquals("4", deliveryService.contexts.single().deliveryAddress?.floor)
        assertEquals("45K", deliveryService.contexts.single().deliveryAddress?.intercom)
        assertEquals("Call before arrival", deliveryService.contexts.single().deliveryAddress?.comment)
        assertEquals("2", order.delivery.address?.entrance)
        assertEquals("4", order.delivery.address?.floor)
        assertEquals("45K", order.delivery.address?.intercom)
        assertEquals("Call before arrival", order.delivery.address?.comment)
    }

    private fun activeCart(
        items: MutableList<CartItem>,
        deliveryDraft: CartDeliveryDraft,
    ): Cart {
        val now = Instant.now()
        return Cart(
            id = UUID.randomUUID(),
            owner = CartOwner(
                type = CartOwnerType.INSTALLATION,
                value = "install-1",
            ),
            status = CartStatus.ACTIVE,
            items = items,
            deliveryDraft = deliveryDraft,
            totalPriceMinor = items.sumOf { it.lineTotalMinor() } + (deliveryDraft.quote?.priceMinor ?: 0L),
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun courierDraft(): CartDeliveryDraft {
        val now = Instant.now()
        return CartDeliveryDraft(
            deliveryMethod = DeliveryMethodType.COURIER,
            deliveryAddress = null,
            pickupPointId = null,
            pickupPointExternalId = null,
            pickupPointName = null,
            pickupPointAddress = null,
            quote = CartDeliveryQuote(
                available = true,
                priceMinor = 1_000,
                currency = "RUB",
                zoneCode = "stale",
                zoneName = "Stale",
                estimatedDays = 3,
                message = null,
                calculatedAt = now,
                expiresAt = now.plusSeconds(3_600),
            ),
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun yandexDraft(): CartDeliveryDraft {
        val now = Instant.now()
        return CartDeliveryDraft(
            deliveryMethod = DeliveryMethodType.YANDEX_PICKUP_POINT,
            deliveryAddress = null,
            pickupPointId = UUID.randomUUID(),
            pickupPointExternalId = "pickup-point-1",
            pickupPointName = "Yandex Point 1",
            pickupPointAddress = "Lenina, 1",
            quote = CartDeliveryQuote(
                available = true,
                priceMinor = 3_000,
                currency = "RUB",
                zoneCode = null,
                zoneName = null,
                estimatedDays = 2,
                message = null,
                calculatedAt = now,
                expiresAt = now.plusSeconds(3_600),
            ),
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun checkoutDeliveryOption(
        deliveryMethod: DeliveryMethodType,
        paymentMethods: List<PaymentMethodCode>,
    ): CheckoutDeliveryOption {
        return CheckoutDeliveryOption(
            deliveryMethod = DeliveryMethodSetting.defaultFor(deliveryMethod).copy(isActive = true),
            paymentMethods = paymentMethods.map(::paymentMethodInfo),
        )
    }

    private fun paymentMethodInfo(code: PaymentMethodCode): PaymentMethodInfo {
        return PaymentMethodInfo(
            code = code,
            name = code.displayName,
            description = code.description,
            isOnline = code.isOnline,
            isActive = true,
        )
    }

    private class StubCartService(
        private val cart: Cart,
    ) : CartService {
        var markedOrderedCartId: UUID? = null
        val updatedDeliveryCommands = mutableListOf<UpdateCartDeliveryCommand>()

        override fun getOrCreateActiveCart(actor: CurrentActor): Cart = cart

        override fun addItem(actor: CurrentActor, command: AddCartItemCommand): Cart {
            throw UnsupportedOperationException("Not used in order service tests")
        }

        override fun changeQuantity(actor: CurrentActor, command: ChangeCartItemQuantityCommand): Cart {
            throw UnsupportedOperationException("Not used in order service tests")
        }

        override fun removeItem(actor: CurrentActor, itemId: UUID): Cart {
            throw UnsupportedOperationException("Not used in order service tests")
        }

        override fun clear(actor: CurrentActor): Cart {
            throw UnsupportedOperationException("Not used in order service tests")
        }

        override fun getDeliveryDraft(actor: CurrentActor): CartDeliveryDraft? = cart.deliveryDraft

        override fun detectCourierDeliveryDraft(actor: CurrentActor, latitude: Double, longitude: Double): CartDeliveryDraft {
            throw UnsupportedOperationException("Not used in order service tests")
        }

        override fun updateDeliveryDraft(actor: CurrentActor, command: UpdateCartDeliveryCommand): CartDeliveryDraft {
            updatedDeliveryCommands += command
            val existingDraft = cart.deliveryDraft
                ?: throw IllegalStateException("Cart delivery draft is not selected")
            val updatedDraft = existingDraft.copy(
                deliveryMethod = command.deliveryMethod,
                deliveryAddress = command.deliveryAddress,
                pickupPointId = command.pickupPointId,
                pickupPointExternalId = command.pickupPointExternalId,
                updatedAt = Instant.now(),
            )
            cart.upsertDeliveryDraft(updatedDraft)
            return updatedDraft
        }

        override fun mergeGuestCartIntoUser(userId: UUID, installId: String): Cart {
            throw UnsupportedOperationException("Not used in order service tests")
        }

        override fun markOrdered(cartId: UUID) {
            markedOrderedCartId = cartId
        }
    }

    private class StubProductReadService(
        private val snapshots: Map<UUID, ProductSnapshot>,
    ) : ProductReadService {
        override fun getActiveProductSnapshot(productId: UUID, variantId: UUID?): ProductSnapshot? {
            return snapshots[productId]
        }
    }

    private class StubUserRepository : UserRepository {
        override fun create(user: User): User {
            throw UnsupportedOperationException("Not used in order service tests")
        }

        override fun findById(id: UUID): User? = null
    }

    private class RecordingDeliveryService(
        private val quote: DeliveryQuote,
    ) : DeliveryService {
        val contexts = mutableListOf<DeliveryQuoteContext>()

        override fun getAvailableMethodSettings(): List<DeliveryMethodSetting> = emptyList()

        override fun getActivePickupPoints(): List<PickupPoint> = emptyList()

        override fun detectYandexLocations(query: String): List<YandexDeliveryLocationVariant> = emptyList()

        override fun getYandexPickupPoints(geoId: Long): List<YandexPickupPointOption> = emptyList()

        override fun getYandexPickupPoint(pickupPointId: String): YandexPickupPointOption? = null

        override fun calculateQuote(context: DeliveryQuoteContext): DeliveryQuote {
            contexts += context
            return quote
        }
    }

    private class StubCheckoutService(
        private val options: List<CheckoutDeliveryOption>,
    ) : CheckoutService {
        val queries = mutableListOf<CheckoutOptionsQuery>()

        override fun getAvailableOptions(query: CheckoutOptionsQuery): List<CheckoutDeliveryOption> {
            queries += query
            return options
        }
    }

    private class RecordingDeliveryOrderRequestService(
        private val confirmation: DeliveryOrderRequestConfirmation? = null,
    ) : DeliveryOrderRequestService {
        var lastOrder: Order? = null

        override fun createAndConfirm(order: Order): DeliveryOrderRequestConfirmation? {
            lastOrder = order.copyOrder()
            return confirmation
        }
    }

    private class RecordingApplicationEventPublisher : ApplicationEventPublisher {
        val events = mutableListOf<Any>()

        override fun publishEvent(event: Any) {
            events += event
        }
    }

    private class InMemoryOrderRepository : OrderRepository {
        val savedOrders = mutableListOf<Order>()
        private val storedOrders = linkedMapOf<UUID, Order>()

        override fun save(order: Order): Order {
            val snapshot = order.copyOrder()
            savedOrders += snapshot
            storedOrders[order.id] = snapshot
            return order
        }

        override fun findById(orderId: UUID): Order? = storedOrders[orderId]?.copyOrder()

        override fun findAllByCurrentStatusStateTypes(stateTypes: Set<OrderStateType>): List<Order> {
            return storedOrders.values.filter { it.currentStatus.stateType in stateTypes }.map(Order::copyOrder)
        }

        override fun findByOrderNumber(orderNumber: String): Order? {
            return storedOrders.values.firstOrNull { it.orderNumber == orderNumber }?.copyOrder()
        }

        override fun findByUserId(userId: UUID): List<Order> {
            return storedOrders.values.filter { it.userId == userId }.map(Order::copyOrder)
        }

        override fun findByGuestInstallId(installId: String): List<Order> {
            return storedOrders.values.filter { it.guestInstallId == installId }.map(Order::copyOrder)
        }

        override fun existsByCurrentStatusId(statusId: UUID): Boolean {
            return storedOrders.values.any { it.currentStatus.id == statusId }
        }
    }

    class InMemoryOrderStatusDefinitionRepository : OrderStatusDefinitionRepository {
        private val definitions = linkedMapOf<UUID, OrderStatusDefinition>()

        override fun findAll(includeInactive: Boolean): List<OrderStatusDefinition> {
            return definitions.values.filter { includeInactive || it.isActive }
        }

        override fun findById(id: UUID): OrderStatusDefinition? = definitions[id]

        override fun findByCode(code: String): OrderStatusDefinition? {
            return definitions.values.firstOrNull { it.code == code.trim().uppercase() }
        }

        override fun findByStateType(stateType: OrderStateType): List<OrderStatusDefinition> {
            return definitions.values.filter { it.stateType == stateType }
        }

        override fun findInitial(): OrderStatusDefinition? {
            return definitions.values.firstOrNull { it.isInitial && it.isActive }
        }

        override fun save(status: OrderStatusDefinition): OrderStatusDefinition {
            definitions[status.id] = status
            return status
        }
    }

    class InMemoryOrderStatusTransitionRepository : OrderStatusTransitionRepository {
        private val transitions = linkedMapOf<UUID, OrderStatusTransition>()

        override fun findAll(includeInactive: Boolean): List<OrderStatusTransition> {
            return transitions.values.filter { includeInactive || it.isActive }
        }

        override fun findById(id: UUID): OrderStatusTransition? = transitions[id]

        override fun findAllByFromStatusId(fromStatusId: UUID, includeInactive: Boolean): List<OrderStatusTransition> {
            return transitions.values.filter { it.fromStatus.id == fromStatusId && (includeInactive || it.isActive) }
        }

        override fun findAllByStatusId(statusId: UUID, includeInactive: Boolean): List<OrderStatusTransition> {
            return transitions.values.filter {
                (it.fromStatus.id == statusId || it.toStatus.id == statusId) &&
                    (includeInactive || it.isActive)
            }
        }

        override fun findTransition(fromStatusId: UUID, toStatusId: UUID): OrderStatusTransition? {
            return transitions.values.firstOrNull { it.fromStatus.id == fromStatusId && it.toStatus.id == toStatusId }
        }

        override fun save(transition: OrderStatusTransition): OrderStatusTransition {
            transitions[transition.id] = transition
            return transition
        }

        override fun deleteById(id: UUID) {
            transitions.remove(id)
        }
    }

    class InMemoryOrderStatusHistoryRepository : OrderStatusHistoryRepository {
        private val histories = mutableListOf<OrderStatusHistory>()

        override fun save(history: OrderStatusHistory): OrderStatusHistory {
            histories += history
            return history
        }

        override fun findAllByOrderId(orderId: UUID): List<OrderStatusHistory> {
            return histories.filter { it.orderId == orderId }.sortedBy(OrderStatusHistory::changedAt)
        }

        override fun existsByOrderId(orderId: UUID): Boolean {
            return histories.any { it.orderId == orderId }
        }
    }
}

private fun createOrderStatusService(
    orderRepository: OrderRepository,
    eventPublisher: ApplicationEventPublisher,
): OrderStatusService {
    val definitionRepository = OrderServiceImplTest.InMemoryOrderStatusDefinitionRepository()
    val transitionRepository = OrderServiceImplTest.InMemoryOrderStatusTransitionRepository()
    val historyRepository = OrderServiceImplTest.InMemoryOrderStatusHistoryRepository()
    OrderStatusWorkflowDefaults.statuses.forEach(definitionRepository::save)
    OrderStatusWorkflowDefaults.transitions.forEach(transitionRepository::save)
    return OrderStatusServiceImpl(
        orderRepository = orderRepository,
        orderStatusDefinitionRepository = definitionRepository,
        orderStatusTransitionRepository = transitionRepository,
        orderStatusHistoryRepository = historyRepository,
        applicationEventPublisher = eventPublisher,
    )
}

private fun Order.copyOrder(): Order {
    return copy(
        delivery = delivery.copy(
            address = delivery.address?.copy(),
        ),
        items = items.map { item ->
            item.copy(modifiers = item.modifiers.map { it.copy() })
        },
        payment = payment?.copy(),
        statusHistory = statusHistory.toList(),
    )
}
