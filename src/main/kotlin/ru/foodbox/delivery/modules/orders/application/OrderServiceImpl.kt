package ru.foodbox.delivery.modules.orders.application

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.catalog.application.ProductReadService
import ru.foodbox.delivery.modules.cart.application.CartService
import ru.foodbox.delivery.modules.orders.application.command.CheckoutCommand
import ru.foodbox.delivery.modules.orders.application.command.GuestCheckoutCommand
import ru.foodbox.delivery.modules.orders.application.event.OrderCreatedEvent
import ru.foodbox.delivery.modules.orders.application.event.OrderStatusChangedEvent
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliveryType
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderStatus
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.user.domain.repository.UserRepository
import java.time.Instant
import java.util.UUID

@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val cartService: CartService,
    private val productReadService: ProductReadService,
    private val userRepository: UserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : OrderService {

    private val phoneRegex = Regex("^\\+?[1-9]\\d{9,14}$")

    @Transactional
    override fun checkout(actor: CurrentActor, command: CheckoutCommand): Order {
        val cart = cartService.getOrCreateActiveCart(actor)
        if (cart.items.isEmpty()) {
            throw IllegalArgumentException("Cart is empty")
        }

        val now = Instant.now()

        val user = (actor as? CurrentActor.User)?.let { userRepository.findById(it.userId) }

        val customerName = command.customerName?.trim()?.takeIf { it.isNotBlank() }
            ?: user?.name

        val customerPhone = command.customerPhone?.trim()?.takeIf { it.isNotBlank() }
            ?: user?.phone

        val customerEmail = command.customerEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: user?.email

        val normalizedPhone = customerPhone?.let(::normalizePhone)

        val items = cart.items.map {
            OrderItem(
                id = UUID.randomUUID(),
                productId = it.productId,
                title = it.title,
                unit = it.unit,
                quantity = it.quantity,
                priceMinor = it.priceMinor,
                totalMinor = it.lineTotalMinor(),
            )
        }

        val subtotal = items.sumOf { it.totalMinor }
        val deliveryFee = deliveryFee(command.deliveryType)

        val order = Order(
            id = UUID.randomUUID(),
            orderNumber = generateOrderNumber(),
            customerType = if (actor is CurrentActor.User) OrderCustomerType.USER else OrderCustomerType.GUEST,
            userId = (actor as? CurrentActor.User)?.userId,
            guestInstallId = (actor as? CurrentActor.Guest)?.installId,
            customerName = customerName,
            customerPhone = normalizedPhone,
            customerEmail = customerEmail,
            status = OrderStatus.PENDING,
            deliveryType = command.deliveryType,
            deliveryAddress = command.deliveryAddress?.trim()?.takeIf { it.isNotBlank() },
            comment = command.comment?.trim()?.takeIf { it.isNotBlank() },
            items = items,
            subtotalMinor = subtotal,
            deliveryFeeMinor = deliveryFee,
            totalMinor = subtotal + deliveryFee,
            createdAt = now,
            updatedAt = now,
        )

        val saved = orderRepository.save(order)
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

        val orderItems = command.items.map { item ->
            val product = productReadService.getActiveProductSnapshot(item.productId)
                ?: throw NotFoundException("Product not found")

            require(item.quantity > 0) { "quantity must be greater than zero" }
            require(item.quantity % product.countStep == 0) { "quantity must match countStep" }

            OrderItem(
                id = UUID.randomUUID(),
                productId = product.id,
                title = product.title,
                unit = product.unit,
                quantity = item.quantity,
                priceMinor = product.priceMinor,
                totalMinor = product.priceMinor * item.quantity,
            )
        }

        val subtotal = orderItems.sumOf { it.totalMinor }
        val deliveryFee = deliveryFee(command.deliveryType)

        val order = Order(
            id = UUID.randomUUID(),
            orderNumber = generateOrderNumber(),
            customerType = OrderCustomerType.GUEST,
            userId = null,
            guestInstallId = installId,
            customerName = command.customerName.trim(),
            customerPhone = normalizedPhone,
            customerEmail = command.customerEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            status = OrderStatus.PENDING,
            deliveryType = command.deliveryType,
            deliveryAddress = command.deliveryAddress?.trim()?.takeIf { it.isNotBlank() },
            comment = command.comment?.trim()?.takeIf { it.isNotBlank() },
            items = orderItems,
            subtotalMinor = subtotal,
            deliveryFeeMinor = deliveryFee,
            totalMinor = subtotal + deliveryFee,
            createdAt = now,
            updatedAt = now,
        )

        val saved = orderRepository.save(order)
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

    override fun getAdminOrders(): List<Order> {
        return orderRepository.findAllByStatuses(
            statuses = setOf(OrderStatus.PENDING, OrderStatus.CONFIRMED),
        )
    }

    override fun getAdminOrderByNumber(orderNumber: String): Order {
        val normalizedOrderNumber = orderNumber.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("orderNumber must not be blank")

        return orderRepository.findByOrderNumber(normalizedOrderNumber)
            ?: throw NotFoundException("Order not found")
    }

    @Transactional
    override fun updateStatus(orderId: UUID, status: OrderStatus): Order {
        val order = orderRepository.findById(orderId)
            ?: throw NotFoundException("Order not found")

        val previousStatus = order.status
        order.updateStatus(status)
        val saved = orderRepository.save(order)
        if (previousStatus != status) {
            applicationEventPublisher.publishEvent(
                OrderStatusChangedEvent(
                    order = saved,
                    previousStatus = previousStatus,
                )
            )
        }
        return saved
    }

    private fun canAccessOrder(actor: CurrentActor, order: Order): Boolean {
        return when (actor) {
            is CurrentActor.User -> order.userId == actor.userId
            is CurrentActor.Guest -> order.guestInstallId == actor.installId
        }
    }

    private fun deliveryFee(deliveryType: OrderDeliveryType): Long {
        return when (deliveryType) {
            OrderDeliveryType.PICKUP -> 0
            OrderDeliveryType.DELIVERY -> 19_900
        }
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
}
