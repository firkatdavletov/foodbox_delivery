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
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.entities.PaymentType
import ru.foodbox.delivery.data.repository.*
import ru.foodbox.delivery.data.telegram.MessageService
import ru.foodbox.delivery.data.telegram.model.ButtonClickEvent
import ru.foodbox.delivery.data.telegram.model.MarkupDataDto
import ru.foodbox.delivery.services.broadcast.OrderStatusBroadcaster
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.OrderDto
import ru.foodbox.delivery.services.dto.OrderItemDto
import ru.foodbox.delivery.services.dto.OrderPreviewDto
import ru.foodbox.delivery.services.mapper.AddressMapper
import ru.foodbox.delivery.services.mapper.DepartmentMapper
import ru.foodbox.delivery.services.mapper.OrderItemMapper
import ru.foodbox.delivery.services.mapper.OrderMapper
import ru.foodbox.delivery.services.mapper.UserMapper
import ru.foodbox.delivery.utils.AddressUtility
import ru.foodbox.delivery.utils.OrderUtility
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val orderMapper: OrderMapper,
    private val orderItemMapper: OrderItemMapper,
    private val addressMapper: AddressMapper,
    private val messageService: MessageService,
    private val orderStatusBroadcaster: OrderStatusBroadcaster,
    private val userMapper: UserMapper,
    private val departmentMapper: DepartmentMapper,
) {
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
            deliveryAddress = deliveryAddress?.let { AddressUtility.addressString(deliveryAddress) },
            comment = comment,
            deliveryTime = LocalDateTime.now() + Duration.ofMinutes(30),
        )
        val newOrderItems = orderItemMapper.toEntity(products, newOrder).toMutableSet()
        newOrder.setItems { newOrderItems }
        newOrder.deliveryPrice = BigDecimal(deliveryPrice)
            .divide(BigDecimal(100))
        newOrder.totalAmount = BigDecimal(amount)
            .divide(BigDecimal(100))
        newOrder.created = LocalDateTime.now()
        newOrder.modified = LocalDateTime.now()
        val savedOrder = orderRepository.save(newOrder)
        val savedOrderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(savedOrderDto, userMapper.toDto(user))

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

        return CreateOrderResponse(savedOrderDto)
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
        val user = order.user
        val orderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(orderDto, userMapper.toDto(user))
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

        sendUpdatesToUser(savedOrder.user.id!!, savedOrder.id!!, savedOrder.status)

        return orderDto
    }

    fun proceedOrderStatus(orderId: Long): OrderDto? {
        val order = orderRepository.findById(orderId).getOrNull() ?: return null
        order.complete()
        val savedOrder = orderRepository.save(order)
        val user = savedOrder.user
        val orderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(orderDto, userMapper.toDto(user))
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

        sendUpdatesToUser(savedOrder.user.id!!, savedOrder.id!!, savedOrder.status)

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
    }
}