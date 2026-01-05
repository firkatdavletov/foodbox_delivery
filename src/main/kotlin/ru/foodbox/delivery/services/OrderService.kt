package ru.foodbox.delivery.services

import jakarta.transaction.Transactional
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.netty.transport.AddressUtils
import ru.foodbox.delivery.controllers.websockets.model.UserOrdersStatusUpdate
import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.entities.UserEntity
import ru.foodbox.delivery.data.repository.*
import ru.foodbox.delivery.data.telegram.MessageService
import ru.foodbox.delivery.data.telegram.model.ButtonClickEvent
import ru.foodbox.delivery.data.telegram.model.MarkupDataDto
import ru.foodbox.delivery.services.broadcast.OrderStatusBroadcaster
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.OrderDto
import ru.foodbox.delivery.services.dto.OrderItemDto
import ru.foodbox.delivery.services.mapper.AddressMapper
import ru.foodbox.delivery.services.mapper.OrderItemMapper
import ru.foodbox.delivery.services.mapper.OrderMapper
import ru.foodbox.delivery.services.mapper.UserMapper
import ru.foodbox.delivery.utils.AddressUtility
import ru.foodbox.delivery.utils.OrderUtility
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
) {
    @EventListener
    fun handleUpdate(event: ButtonClickEvent) {
        when (event.action) {
            MarkupDataDto.Action.CHANGE -> proceedOrderStatus(event.orderId)
            MarkupDataDto.Action.CANCEL -> cancelOrder(event.orderId)
        }
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
                OrderStatus.PAID,
            )
        )

        return orderMapper.toDto(orderEntities)
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, status: OrderStatus): OrderDto? {
        val order = orderRepository.findById(orderId).getOrNull() ?: return null

        order.status = status
        val updatedOrder = orderRepository.save(order)
        return orderMapper.toDto(updatedOrder)
    }

    fun createOrder(
        userId: Long,
        deliveryType: DeliveryType,
        deliveryAddress: AddressDto?,
        comment: String?,
        products: List<OrderItemDto>,
        departmentId: Long,
        amount: Double,
        deliveryPrice: Double,
    ): OrderDto? {
        val user = userRepository.findById(userId).getOrNull() ?: return null
        val newOrder = OrderEntity(
            user = user,
            deliveryType = deliveryType,
            deliveryAddress = deliveryAddress?.let { AddressUtility.addressString(deliveryAddress) },
            comment = comment
        )
        val newOrderItems = orderItemMapper.toEntity(products, newOrder).toMutableSet()
        newOrder.setItems { newOrderItems }
        newOrder.deliveryPrice = deliveryPrice
        newOrder.totalAmount = amount
        newOrder.created = LocalDateTime.now()
        newOrder.modified = LocalDateTime.now()
        val savedOrder = orderRepository.save(newOrder)
        val savedOrderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(savedOrderDto, userMapper.toDto(user))

        val messageId = sendMessage(
            savedOrder.id!!,
            orderInfo,
            savedOrder.status,
            savedOrder.messageId
        )

        if (messageId != null) {
            setOrderMessageId(savedOrder, messageId)
        }

        return savedOrderDto
    }

    private fun sendMessage(
        orderId: Long,
        orderInfo: String,
        orderStatus: OrderStatus,
        orderMessageId: Int?
    ): Int? {

        if (orderMessageId != null) {
            messageService.deleteBotMessage(orderMessageId)
        }

        return messageService.sendMessageToBot(
            message = orderInfo,
            orderId = orderId,
            orderStatus = orderStatus
        )
    }

    fun cancelOrder(orderId: Long): OrderDto? {
        val order = orderRepository.findById(orderId).getOrNull() ?: return null
        order.status = OrderStatus.CANCELLED
        val savedOrder = orderRepository.save(order)
        val user = order.user
        val orderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(orderDto, userMapper.toDto(user))
        val messageId = sendMessage(
            savedOrder.id!!,
            orderInfo,
            savedOrder.status,
            savedOrder.messageId
        )

        if (messageId != null) {
            setOrderMessageId(savedOrder, messageId)
        }

        sendUpdatesToUser(savedOrder.user.id!!, savedOrder.id!!, savedOrder.status)

        return orderDto
    }

    fun proceedOrderStatus(orderId: Long): OrderDto? {
        val order = orderRepository.findById(orderId).getOrNull() ?: return null
        order.status = OrderStatus.nextStatus(order.status)
        val savedOrder = orderRepository.save(order)
        val user = savedOrder.user
        val orderDto = orderMapper.toDto(savedOrder)
        val orderInfo = OrderUtility.createOrderInfo(orderDto, userMapper.toDto(user))
        val messageId = sendMessage(
            savedOrder.id!!,
            orderInfo,
            savedOrder.status,
            savedOrder.messageId
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
}