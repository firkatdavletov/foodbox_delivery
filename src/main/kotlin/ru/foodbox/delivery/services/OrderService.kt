package ru.foodbox.delivery.services

import jakarta.transaction.Transactional
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.order.body.CreateOrderResponse
import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.entities.DepartmentEntity
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderCustomerType
import ru.foodbox.delivery.data.entities.OrderItemEntity
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.repository.*
import ru.foodbox.delivery.data.telegram.MessageService
import ru.foodbox.delivery.data.telegram.model.ButtonClickEvent
import ru.foodbox.delivery.data.telegram.model.MarkupDataDto
import ru.foodbox.delivery.services.broadcast.OrderStatusBroadcaster
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.GuestCheckoutCustomerInputDto
import ru.foodbox.delivery.services.dto.GuestCheckoutDeliveryInputDto
import ru.foodbox.delivery.services.dto.GuestCheckoutItemInputDto
import ru.foodbox.delivery.services.dto.GuestCheckoutResultDto
import ru.foodbox.delivery.services.dto.OrderDto
import ru.foodbox.delivery.services.dto.OrderItemDto
import ru.foodbox.delivery.services.dto.OrderPreviewDto
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.mapper.DepartmentMapper
import ru.foodbox.delivery.services.mapper.OrderItemMapper
import ru.foodbox.delivery.services.mapper.OrderMapper
import ru.foodbox.delivery.common.utils.AddressUtility
import ru.foodbox.delivery.common.utils.DeliveryPriceCalculator
import ru.foodbox.delivery.common.utils.DistanceCalculator
import ru.foodbox.delivery.common.utils.OrderUtility
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val productRepository: ProductRepository,
    private val orderMapper: OrderMapper,
    private val orderItemMapper: OrderItemMapper,
    private val messageService: MessageService,
    private val orderStatusBroadcaster: OrderStatusBroadcaster,
    private val departmentMapper: DepartmentMapper,
    private val deliveryPriceCalculator: DeliveryPriceCalculator,
    private val distanceCalculator: DistanceCalculator,
) {
    private val phoneRegex = Regex("^\\+?[1-9]\\d{9,14}$")

    private data class ResolvedDepartment(
        val dto: DepartmentDto,
    )

    private data class ResolvedGuestItem(
        val productId: Long,
        val imageUrl: String?,
        val unit: ru.foodbox.delivery.services.model.UnitOfMeasure,
        val name: String,
        val quantity: Int,
        val price: BigDecimal,
        val totalPrice: BigDecimal,
    ) {
        fun toOrderItem(order: OrderEntity): OrderItemEntity {
            return OrderItemEntity(
                order = order,
                productId = productId,
                imageUrl = imageUrl,
                name = name,
                unit = unit,
                quantity = quantity,
                price = price,
                totalPrice = totalPrice,
            )
        }

        fun totalMinor(): Long = totalPrice
            .multiply(BigDecimal(100))
            .longValueExact()
    }

    @EventListener
    fun handleUpdate(event: ButtonClickEvent) {
        when (event.action) {
            MarkupDataDto.Action.CHANGE -> proceedOrderStatus(event.orderId)
            MarkupDataDto.Action.CANCEL -> cancelOrder(event.orderId)
        }
    }

    fun allOrders(): List<OrderDto> {
        val orders = orderRepository.findAll()
        return orderMapper.toDto(orders)
    }

    fun takeOrder(id: Long): OrderDto? {
        val order = orderRepository.findById(id).getOrNull() ?: return null

        order.take()

        val savedOrder = orderRepository.save(order)

        return orderMapper.toDto(savedOrder)
    }

    fun completeOrder(id: Long): OrderDto? {
        val order = orderRepository.findById(id).getOrNull() ?: return null

        order.complete()

        val savedOrder = orderRepository.save(order)

        return orderMapper.toDto(savedOrder)
    }

    fun cancelOrderTest(id: Long): OrderDto? {
        val order = orderRepository.findById(id).getOrNull() ?: return null

        order.cancel()

        val savedOrder = orderRepository.save(order)

        return orderMapper.toDto(savedOrder)
    }

    fun pendingOrder(id: Long): OrderDto? {
        val order = orderRepository.findById(id).getOrNull() ?: return null

        order.pending()

        val savedOrder = orderRepository.save(order)

        return orderMapper.toDto(savedOrder)
    }

    fun getOrderById(id: Long): OrderDto? {
        val entity = orderRepository.findById(id).getOrNull() ?: return null
        return orderMapper.toDto(entity)
    }

    fun getCurrentOrders(userId: Long): List<OrderDto> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден") }
        val orderEntities = orderRepository.findByUserAndStatusInOrderByCreatedDesc(
            user,
            setOf(
                OrderStatus.PROCESSING,
                OrderStatus.PENDING,
                OrderStatus.DELIVERY,
            )
        )

        return orderMapper.toDto(orderEntities)
    }

    fun createOrder(
        userId: Long,
        deliveryType: DeliveryType,
        deliveryAddress: AddressDto?,
        comment: String?,
        products: List<OrderItemDto>,
        departmentId: Long,
        amount: Long,
        deliveryPrice: Long,
    ): CreateOrderResponse {
        val departmentEntity = departmentRepository.findById(departmentId).getOrNull()
            ?: return CreateOrderResponse("Ошибка определения ресторана", 500)

        val departmentDto = departmentMapper.toDto(departmentEntity)

        if (!departmentDto.isWorkingNow) {
            return CreateOrderResponse("Ресторан закрыт", 404)
        }
        val user = userRepository.findById(userId).getOrNull()
            ?: return CreateOrderResponse("Ошибка определения пользователя", 500)

        val newOrder = OrderEntity(
            user = user,
            deliveryType = deliveryType,
            customerType = OrderCustomerType.AUTHORIZED,
            customerName = user.name.takeIf { it.isNotBlank() },
            customerPhone = user.phone.takeIf { it.isNotBlank() },
            customerEmail = user.email.takeIf { it.isNotBlank() },
            deliveryAddress = deliveryAddress?.let { AddressUtility.addressString(deliveryAddress) },
            comment = comment,
            deliveryTime = LocalDateTime.now() + Duration.ofMinutes(30),
        )
        val newOrderItems = orderItemMapper.toEntity(products, newOrder).toMutableSet()
        newOrder.setItems { newOrderItems }
        newOrder.deliveryPrice = toMoney(deliveryPrice)
        newOrder.totalAmount = toMoney(amount)

        return CreateOrderResponse(saveOrder(newOrder))
    }

    @Transactional
    fun createGuestOrder(
        items: List<GuestCheckoutItemInputDto>,
        customer: GuestCheckoutCustomerInputDto,
        delivery: GuestCheckoutDeliveryInputDto,
        comment: String?,
    ): GuestCheckoutResultDto {
        if (items.isEmpty()) {
            throw ResponseStatusException(HttpStatusCode.valueOf(400), "Список товаров не должен быть пустым")
        }

        val customerName = customer.name.trim().ifBlank {
            throw ResponseStatusException(HttpStatusCode.valueOf(400), "Имя покупателя обязательно")
        }
        val customerPhone = normalizePhone(customer.phone)
        val customerEmail = customer.email
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }

        val resolvedDepartment = resolveGuestDepartment(delivery)
        if (!resolvedDepartment.dto.isWorkingNow) {
            throw ResponseStatusException(HttpStatusCode.valueOf(400), "Ресторан закрыт")
        }

        val resolvedItems = resolveGuestItems(items)
        val itemsTotalMinor = resolvedItems.sumOf { it.totalMinor() }
        val deliveryPriceMinor = calculateGuestDeliveryPrice(
            delivery = delivery,
            department = resolvedDepartment.dto,
            itemsTotalMinor = itemsTotalMinor
        )

        val newOrder = OrderEntity(
            user = null,
            deliveryType = delivery.type,
            customerType = OrderCustomerType.GUEST,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            deliveryAddress = delivery.address?.let { AddressUtility.addressString(it) },
            comment = comment?.trim()?.takeIf { it.isNotBlank() },
            deliveryTime = LocalDateTime.now() + Duration.ofMinutes(30),
        )
        val newOrderItems = resolvedItems
            .map { it.toOrderItem(newOrder) }
            .toMutableSet()

        newOrder.setItems { newOrderItems }
        newOrder.deliveryPrice = toMoney(deliveryPriceMinor)
        newOrder.totalAmount = toMoney(itemsTotalMinor + deliveryPriceMinor)

        val order = saveOrder(newOrder)
        return GuestCheckoutResultDto(
            orderId = order.id,
            orderNumber = order.id.toString(),
            status = order.status,
            createdAt = order.created,
            totalAmount = order.totalAmount,
        )
    }

    private fun sendMessage(
        orderId: Long,
        orderInfo: String,
        orderStatus: OrderStatus,
        orderMessageId: Int?,
        deliveryType: DeliveryType,
    ): Int? {

        if (orderMessageId != null) {
            messageService.deleteBotMessage(orderMessageId)
        }

        return messageService.sendMessageToBot(
            message = orderInfo,
            orderId = orderId,
            orderStatus = orderStatus,
            deliveryType = deliveryType
        )
    }

    fun cancelOrder(orderId: Long): OrderDto? {
        val order = orderRepository.findById(orderId).getOrNull() ?: return null
        order.cancel()
        val savedOrder = orderRepository.save(order)
        val orderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(orderDto)
        val messageId = sendMessage(
            savedOrder.id!!,
            orderInfo,
            savedOrder.status,
            savedOrder.messageId,
            orderDto.deliveryType
        )

        if (messageId != null) {
            setOrderMessageId(savedOrder, messageId)
        }

        savedOrder.user?.id?.let { userId ->
            sendUpdatesToUser(userId, savedOrder.id!!, savedOrder.status)
        }

        return orderDto
    }

    fun proceedOrderStatus(orderId: Long): OrderDto? {
        val order = orderRepository.findById(orderId).getOrNull() ?: return null
        order.complete()
        val savedOrder = orderRepository.save(order)
        val orderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(orderDto)
        val messageId = sendMessage(
            savedOrder.id!!,
            orderInfo,
            savedOrder.status,
            savedOrder.messageId,
            orderDto.deliveryType
        )

        if (messageId != null) {
            setOrderMessageId(savedOrder, messageId)
        }

        savedOrder.user?.id?.let { userId ->
            sendUpdatesToUser(userId, savedOrder.id!!, savedOrder.status)
        }

        return orderMapper.toDto(savedOrder)
    }

    private fun setOrderMessageId(orderEntity: OrderEntity, messageId: Int) {
        orderEntity.messageId = messageId
        orderRepository.save(orderEntity)
    }

    private fun sendUpdatesToUser(userId: Long, orderId: Long, status: OrderStatus) {
        orderStatusBroadcaster.broadcastUpdate(
            userId,
            orderId,
            status
        )
    }

    fun getOrders(page: Int, size: Int): Page<OrderPreviewDto> {

        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "created")
        )

        return orderRepository.findOrderPreviews(pageable)
            .map { order ->
                OrderPreviewDto(
                    id = order.id,
                    totalAmount = order.totalAmount
                        .multiply(BigDecimal(100))
                        .longValueExact(),
                    status = order.status,
                    customerName = order.customerName,
                    companyName = order.companyName,
                    deliveryTime = order.deliveryTime
                )
            }
    }

    private fun saveOrder(orderEntity: OrderEntity): OrderDto {
        orderEntity.created = LocalDateTime.now()
        orderEntity.modified = LocalDateTime.now()

        val savedOrder = orderRepository.save(orderEntity)
        val savedOrderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(savedOrderDto)

        val messageId = sendMessage(
            savedOrder.id!!,
            orderInfo,
            savedOrder.status,
            savedOrder.messageId,
            savedOrder.deliveryType
        )

        if (messageId != null) {
            setOrderMessageId(savedOrder, messageId)
        }

        return savedOrderDto
    }

    private fun toMoney(value: Long): BigDecimal {
        return BigDecimal(value).divide(BigDecimal(100))
    }

    private fun normalizePhone(phone: String): String {
        val normalized = phone.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (!phoneRegex.matches(normalized)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(400), "Некорректный формат телефона")
        }

        return normalized.removePrefix("+")
    }

    private fun resolveGuestDepartment(
        delivery: GuestCheckoutDeliveryInputDto,
    ): ResolvedDepartment {
        return when (delivery.type) {
            DeliveryType.PICKUP -> {
                val pickupPointId = delivery.pickupPointId
                    ?: throw ResponseStatusException(HttpStatusCode.valueOf(400), "Для самовывоза нужен pickupPointId")

                val department = departmentRepository.findById(pickupPointId).getOrNull()
                    ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пункт самовывоза не найден")

                if (!department.isActive) {
                    throw ResponseStatusException(HttpStatusCode.valueOf(400), "Пункт самовывоза недоступен")
                }

                ResolvedDepartment(departmentMapper.toDto(department))
            }
            DeliveryType.DELIVERY -> {
                val address = delivery.address
                    ?: throw ResponseStatusException(HttpStatusCode.valueOf(400), "Для доставки нужно передать address")

                val department = if (delivery.pickupPointId != null) {
                    departmentRepository.findById(delivery.pickupPointId).getOrNull()
                        ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пункт выдачи не найден")
                } else {
                    val candidates = departmentRepository.findAll()
                        .asSequence()
                        .filter { it.isActive && it.canDeliver }
                        .filter { canDepartmentDeliverToCity(it, address.city.id) }
                        .toList()

                    candidates.minByOrNull { department ->
                        distanceCalculator.haversineDistance(
                            lat1 = department.latitude,
                            lon1 = department.longitude,
                            lat2 = address.latitude,
                            lon2 = address.longitude,
                        )
                    } ?: throw ResponseStatusException(HttpStatusCode.valueOf(400), "Некорректный способ доставки")
                }

                if (!department.isActive || !department.canDeliver) {
                    throw ResponseStatusException(HttpStatusCode.valueOf(400), "Некорректный способ доставки")
                }

                ResolvedDepartment(departmentMapper.toDto(department))
            }
        }
    }

    private fun canDepartmentDeliverToCity(department: DepartmentEntity, cityId: Long): Boolean {
        val departmentCityId = department.cityEntity.id
        if (departmentCityId == cityId) {
            return true
        }

        return department.cityEntity.subCities.any { it.id == cityId }
    }

    private fun calculateGuestDeliveryPrice(
        delivery: GuestCheckoutDeliveryInputDto,
        department: DepartmentDto,
        itemsTotalMinor: Long,
    ): Long {
        return when (delivery.type) {
            DeliveryType.PICKUP -> 0L
            DeliveryType.DELIVERY -> {
                val address = delivery.address
                    ?: throw ResponseStatusException(HttpStatusCode.valueOf(400), "Для доставки нужно передать address")

                val deliveryInfo = deliveryPriceCalculator.calculateDeliveryPrice(
                    deliveryType = delivery.type,
                    lat = address.latitude,
                    lon = address.longitude,
                    cityId = address.city.id,
                    department = department,
                ) ?: throw ResponseStatusException(HttpStatusCode.valueOf(400), "Некорректный способ доставки")

                val basePrice = deliveryInfo.deliveryPrice
                val freeDeliveryPrice = deliveryInfo.freeDeliveryPrice

                if (freeDeliveryPrice != null && itemsTotalMinor >= freeDeliveryPrice) {
                    0L
                } else {
                    basePrice
                }
            }
        }
    }

    private fun resolveGuestItems(
        items: List<GuestCheckoutItemInputDto>,
    ): List<ResolvedGuestItem> {
        val normalizedSkus = items.mapNotNull { it.sku?.trim()?.takeIf { sku -> sku.isNotBlank() } }
        val productIds = items.mapNotNull { it.productId }

        val productsById = if (productIds.isEmpty()) {
            emptyMap()
        } else {
            productRepository.findAllById(productIds).associateBy { it.id!! }
        }

        val productsBySku = if (normalizedSkus.isEmpty()) {
            emptyMap()
        } else {
            productRepository.findAllBySkuIn(normalizedSkus).associateBy { it.sku }
        }

        val itemByProduct = linkedMapOf<Long, Pair<ru.foodbox.delivery.data.entities.ProductEntity, Int>>()

        items.forEach { item ->
            if (item.quantity <= 0) {
                throw ResponseStatusException(HttpStatusCode.valueOf(400), "Количество товара должно быть больше 0")
            }

            val normalizedSku = item.sku?.trim()?.takeIf { it.isNotBlank() }

            val productById = item.productId?.let { productId ->
                productsById[productId]
                    ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Товар не найден")
            }

            val productBySku = normalizedSku?.let { sku ->
                productsBySku[sku]
                    ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "SKU не найден")
            }

            val product = when {
                productById != null && productBySku != null -> {
                    if (productById.id != productBySku.id) {
                        throw ResponseStatusException(
                            HttpStatusCode.valueOf(400),
                            "productId и sku указывают на разные товары"
                        )
                    }
                    productById
                }
                productById != null -> productById
                productBySku != null -> productBySku
                else -> throw ResponseStatusException(
                    HttpStatusCode.valueOf(400),
                    "Для каждого товара нужно передать productId или sku"
                )
            }

            if (!product.isActive || product.categories.none { it.isActive }) {
                throw ResponseStatusException(HttpStatusCode.valueOf(400), "Товар недоступен")
            }

            val productId = product.id
                ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Товар не найден")

            val existing = itemByProduct[productId]
            val totalQuantity = (existing?.second ?: 0) + item.quantity
            itemByProduct[productId] = product to totalQuantity
        }

        return itemByProduct.values.map { (product, quantity) ->
            if (product.countStep <= 0 || quantity % product.countStep != 0) {
                throw ResponseStatusException(HttpStatusCode.valueOf(400), "Некорректный шаг количества")
            }

            // TODO: добавить проверку остатков после появления модели складских остатков.
            val lineTotal = product.price.multiply(BigDecimal(quantity))
            ResolvedGuestItem(
                productId = product.id!!,
                imageUrl = product.imageUrl,
                unit = product.unit,
                name = product.title,
                quantity = quantity,
                price = product.price,
                totalPrice = lineTotal,
            )
        }
    }
}
