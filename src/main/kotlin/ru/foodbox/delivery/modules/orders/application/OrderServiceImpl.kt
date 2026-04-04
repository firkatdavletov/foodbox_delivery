package ru.foodbox.delivery.modules.orders.application

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.catalog.application.ProductReadService
import ru.foodbox.delivery.modules.cart.application.CartService
import ru.foodbox.delivery.modules.cart.pricing.application.CartItemPricingService
import ru.foodbox.delivery.modules.checkout.application.CheckoutOptionsQuery
import ru.foodbox.delivery.modules.checkout.application.CheckoutService
import ru.foodbox.delivery.modules.delivery.application.DeliveryOrderRequestService
import ru.foodbox.delivery.modules.delivery.application.DeliveryService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.DeliveryValidationException
import ru.foodbox.delivery.modules.orders.application.command.CheckoutCommand
import ru.foodbox.delivery.modules.orders.application.command.ChangeOrderStatusCommand
import ru.foodbox.delivery.modules.orders.application.command.GuestCheckoutCommand
import ru.foodbox.delivery.modules.orders.application.event.OrderCreatedEvent
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderPaymentSnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.modifier.domain.OrderItemModifier
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.user.domain.repository.UserRepository
import java.time.Instant
import java.util.UUID

@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val cartService: CartService,
    private val productReadService: ProductReadService,
    private val userRepository: UserRepository,
    private val deliveryService: DeliveryService,
    private val cartItemPricingService: CartItemPricingService,
    private val checkoutService: CheckoutService,
    private val deliveryOrderRequestService: DeliveryOrderRequestService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val orderStatusService: OrderStatusService,
) : OrderService {

    private val phoneRegex = Regex("^\\+?[1-9]\\d{9,14}$")

    @Transactional
    override fun checkout(actor: CurrentActor, command: CheckoutCommand): Order {
        val cart = cartService.getOrCreateActiveCart(actor)
        if (cart.items.isEmpty()) {
            throw IllegalArgumentException("Cart is empty")
        }

        val deliveryDraft = cart.deliveryDraft
            ?: throw DeliveryValidationException("Cart delivery draft is not selected")

        val user = (actor as? CurrentActor.User)?.let { userRepository.findById(it.userId) }

        val customerName = command.customerName?.trim()?.takeIf { it.isNotBlank() }
            ?: user?.name

        val customerPhone = command.customerPhone?.trim()?.takeIf { it.isNotBlank() }
            ?: user?.phone

        val customerEmail = command.customerEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: user?.email

        val normalizedPhone = customerPhone?.let(::normalizePhone)
        val now = Instant.now()
        val initialStatus = orderStatusService.getInitialStatus()
        validatePaymentMethod(
            deliveryMethod = deliveryDraft.deliveryMethod,
            pickupPointExternalId = deliveryDraft.pickupPointExternalId,
            paymentMethodCode = command.paymentMethodCode,
        )
        val items = resolveCartItems(cart)
        val subtotal = items.sumOf { it.totalMinor }
        val deliveryQuote = requireAvailableQuote(
            deliveryService.calculateQuote(
                DeliveryQuoteContext(
                    cartId = cart.id,
                    subtotalMinor = subtotal,
                    itemCount = cart.items.sumOf { it.quantity },
                    deliveryMethod = deliveryDraft.deliveryMethod,
                    deliveryAddress = deliveryDraft.deliveryAddress,
                    pickupPointId = deliveryDraft.pickupPointId,
                    pickupPointExternalId = deliveryDraft.pickupPointExternalId,
                )
            )
        )
        val deliverySnapshot = buildDeliverySnapshot(
            method = deliveryDraft.deliveryMethod,
            address = deliveryDraft.deliveryAddress,
            quote = deliveryQuote,
        )

        val order = Order(
            id = UUID.randomUUID(),
            orderNumber = generateOrderNumber(),
            customerType = if (actor is CurrentActor.User) OrderCustomerType.USER else OrderCustomerType.GUEST,
            userId = (actor as? CurrentActor.User)?.userId,
            guestInstallId = (actor as? CurrentActor.Guest)?.installId,
            customerName = customerName,
            customerPhone = normalizedPhone,
            customerEmail = customerEmail,
            currentStatus = initialStatus,
            delivery = deliverySnapshot,
            comment = command.comment?.trim()?.takeIf { it.isNotBlank() },
            items = items,
            subtotalMinor = subtotal,
            deliveryFeeMinor = deliverySnapshot.priceMinor,
            totalMinor = subtotal + deliverySnapshot.priceMinor,
            statusChangedAt = now,
            createdAt = now,
            updatedAt = now,
            payment = OrderPaymentSnapshot(
                methodCode = command.paymentMethodCode,
                methodName = command.paymentMethodCode.displayName,
            ),
        )

        var saved = orderRepository.save(order)
        orderStatusService.recordInitialStatus(saved, OrderStatusChangeActor.system())
        deliveryOrderRequestService.createAndConfirm(saved)?.let { confirmation ->
            saved.updateDeliveryPricing(
                priceMinor = confirmation.deliveryFeeMinor,
                currency = confirmation.currency,
            )
            saved = orderRepository.save(saved)
            saved = orderStatusService.changeStatus(
                orderId = saved.id,
                command = ChangeOrderStatusCommand(
                    targetStateType = OrderStateType.CONFIRMED,
                ),
                actor = OrderStatusChangeActor.system(),
            )
        }
        cartService.markOrdered(cart.id)
        applicationEventPublisher.publishEvent(OrderCreatedEvent(saved))
        return saved
    }

    @Transactional
    override fun guestCheckout(command: GuestCheckoutCommand, installId: String?): Order {
        if (command.items.isEmpty()) {
            throw IllegalArgumentException("Items must not be empty")
        }

        val normalizedPhone = normalizePhone(command.customerPhone)
        val now = Instant.now()
        val initialStatus = orderStatusService.getInitialStatus()

        validatePaymentMethod(
            deliveryMethod = command.deliveryMethod,
            pickupPointExternalId = command.pickupPointExternalId,
            paymentMethodCode = command.paymentMethodCode,
        )
        val orderItems = command.items.map { item ->
            val product = productReadService.getActiveProductSnapshot(
                productId = item.productId,
                variantId = item.variantId,
            )
                ?: throw NotFoundException("Product not found")

            require(item.quantity > 0) { "quantity must be greater than zero" }
            require(item.quantity % product.countStep == 0) { "quantity must match countStep" }

            OrderItem(
                id = UUID.randomUUID(),
                productId = product.id,
                variantId = product.variantId,
                sku = product.sku,
                title = product.title,
                unit = product.unit,
                quantity = item.quantity,
                priceMinor = product.priceMinor,
                totalMinor = cartItemPricingService.calculate(
                    basePriceMinor = product.priceMinor,
                    lineQuantity = item.quantity,
                    modifiers = emptyList(),
                ).lineTotalMinor,
            )
        }

        val subtotal = orderItems.sumOf { it.totalMinor }
        val deliveryQuote = requireAvailableQuote(
            deliveryService.calculateQuote(
                DeliveryQuoteContext(
                    subtotalMinor = subtotal,
                    itemCount = orderItems.sumOf { it.quantity },
                    deliveryMethod = command.deliveryMethod,
                    deliveryAddress = command.deliveryAddress,
                    pickupPointId = command.pickupPointId,
                    pickupPointExternalId = command.pickupPointExternalId,
                )
            )
        )
        val deliverySnapshot = buildDeliverySnapshot(
            method = command.deliveryMethod,
            address = command.deliveryAddress,
            quote = deliveryQuote,
        )

        val order = Order(
            id = UUID.randomUUID(),
            orderNumber = generateOrderNumber(),
            customerType = OrderCustomerType.GUEST,
            userId = null,
            guestInstallId = installId,
            customerName = command.customerName.trim(),
            customerPhone = normalizedPhone,
            customerEmail = command.customerEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            currentStatus = initialStatus,
            delivery = deliverySnapshot,
            comment = command.comment?.trim()?.takeIf { it.isNotBlank() },
            items = orderItems,
            subtotalMinor = subtotal,
            deliveryFeeMinor = deliverySnapshot.priceMinor,
            totalMinor = subtotal + deliverySnapshot.priceMinor,
            statusChangedAt = now,
            createdAt = now,
            updatedAt = now,
            payment = OrderPaymentSnapshot(
                methodCode = command.paymentMethodCode,
                methodName = command.paymentMethodCode.displayName,
            ),
        )

        var saved = orderRepository.save(order)
        orderStatusService.recordInitialStatus(saved, OrderStatusChangeActor.system())
        deliveryOrderRequestService.createAndConfirm(saved)?.let { confirmation ->
            saved.updateDeliveryPricing(
                priceMinor = confirmation.deliveryFeeMinor,
                currency = confirmation.currency,
            )
            saved = orderRepository.save(saved)
            saved = orderStatusService.changeStatus(
                orderId = saved.id,
                command = ChangeOrderStatusCommand(
                    targetStateType = OrderStateType.CONFIRMED,
                ),
                actor = OrderStatusChangeActor.system(),
            )
        }
        applicationEventPublisher.publishEvent(OrderCreatedEvent(saved))
        return saved
    }

    override fun getOrder(actor: CurrentActor, orderId: UUID): Order {
        val order = orderRepository.findById(orderId)
            ?: throw NotFoundException("Order not found")

        if (!canAccessOrder(actor, order)) {
            throw ForbiddenException("You do not have access to this order")
        }

        return order
    }

    override fun getMyOrders(actor: CurrentActor): List<Order> {
        return when (actor) {
            is CurrentActor.User -> orderRepository.findByUserId(actor.userId)
            is CurrentActor.Guest -> orderRepository.findByGuestInstallId(actor.installId)
        }
    }

    override fun getCurrentOrders(actor: CurrentActor): List<Order> {
        return getMyOrders(actor).filterNot { it.currentStatus.isFinal }
    }

    override fun getAdminOrders(): List<Order> {
        return orderRepository.findAllByCurrentStatusStateTypes(
            stateTypes = setOf(
                OrderStateType.CREATED,
                OrderStateType.AWAITING_CONFIRMATION,
                OrderStateType.CONFIRMED,
                OrderStateType.PREPARING,
                OrderStateType.READY_FOR_PICKUP,
                OrderStateType.OUT_FOR_DELIVERY,
                OrderStateType.ON_HOLD,
            ),
        )
    }

    override fun getAdminOrderByNumber(orderNumber: String): Order {
        val normalizedOrderNumber = orderNumber.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("orderNumber must not be blank")

        return orderRepository.findByOrderNumber(normalizedOrderNumber)
            ?: throw NotFoundException("Order not found")
    }

    private fun canAccessOrder(actor: CurrentActor, order: Order): Boolean {
        return when (actor) {
            is CurrentActor.User -> order.userId == actor.userId
            is CurrentActor.Guest -> order.guestInstallId == actor.installId
        }
    }

    private fun buildDeliverySnapshot(
        method: DeliveryMethodType,
        address: DeliveryAddress?,
        quote: DeliveryQuote,
    ): OrderDeliverySnapshot {
        val priceMinor = quote.priceMinor
            ?: throw DeliveryValidationException("Delivery price is unavailable")

        return OrderDeliverySnapshot(
            method = method,
            methodName = method.displayName,
            priceMinor = priceMinor,
            currency = quote.currency,
            zoneCode = quote.zoneCode,
            zoneName = quote.zoneName,
            estimatedDays = quote.estimatedDays,
            estimatesMinutes = quote.estimatesMinutes,
            pickupPointId = quote.pickupPointId,
            pickupPointExternalId = quote.pickupPointExternalId,
            pickupPointName = quote.pickupPointName,
            pickupPointAddress = quote.pickupPointAddress,
            address = if (method == DeliveryMethodType.COURIER) address?.normalized() else null,
        )
    }

    private fun requireAvailableQuote(quote: DeliveryQuote): DeliveryQuote {
        if (!quote.available) {
            throw DeliveryValidationException(quote.message ?: "Delivery is unavailable")
        }
        return quote
    }

    private fun normalizePhone(phone: String): String {
        val normalized = phone.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (!phoneRegex.matches(normalized)) {
            throw IllegalArgumentException("Invalid phone format")
        }

        return normalized
    }

    private fun generateOrderNumber(): String {
        return UUID.randomUUID().toString().take(6).uppercase()
    }

    private fun validatePaymentMethod(
        deliveryMethod: DeliveryMethodType,
        pickupPointExternalId: String?,
        paymentMethodCode: PaymentMethodCode,
    ) {
        val availablePaymentMethods = checkoutService.getAvailableOptions(
            CheckoutOptionsQuery(
                pickupPointId = pickupPointExternalId,
            )
        ).firstOrNull { it.deliveryMethod == deliveryMethod }
            ?.paymentMethods
            ?.map { it.code }
            ?.toSet()
            ?: throw DeliveryValidationException("Selected delivery method is unavailable")

        if (paymentMethodCode !in availablePaymentMethods) {
            throw DeliveryValidationException("Selected payment method is unavailable for delivery")
        }
    }

    private fun resolveCartItems(cart: ru.foodbox.delivery.modules.cart.domain.Cart): List<OrderItem> {
        return cart.items.map { item ->
            val product = productReadService.getActiveProductSnapshot(
                productId = item.productId,
                variantId = item.variantId,
            ) ?: throw NotFoundException("Product not found")

            require(item.quantity > 0) { "quantity must be greater than zero" }
            require(item.quantity % product.countStep == 0) { "quantity must match countStep" }
            val modifiers = item.modifiers.map { modifier ->
                OrderItemModifier(
                    modifierGroupId = modifier.modifierGroupId,
                    modifierOptionId = modifier.modifierOptionId,
                    groupCodeSnapshot = modifier.groupCodeSnapshot,
                    groupNameSnapshot = modifier.groupNameSnapshot,
                    optionCodeSnapshot = modifier.optionCodeSnapshot,
                    optionNameSnapshot = modifier.optionNameSnapshot,
                    applicationScopeSnapshot = modifier.applicationScopeSnapshot,
                    priceSnapshot = modifier.priceSnapshot,
                    quantity = modifier.quantity,
                )
            }
            val total = cartItemPricingService.calculate(
                basePriceMinor = product.priceMinor,
                lineQuantity = item.quantity,
                modifiers = modifiers,
            )

            OrderItem(
                id = UUID.randomUUID(),
                productId = product.id,
                variantId = product.variantId,
                sku = product.sku,
                title = product.title,
                unit = product.unit,
                quantity = item.quantity,
                priceMinor = product.priceMinor,
                totalMinor = total.lineTotalMinor,
                modifiers = modifiers,
            )
        }
    }
}
