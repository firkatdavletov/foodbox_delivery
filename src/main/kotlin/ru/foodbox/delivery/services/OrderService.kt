package ru.foodbox.delivery.services

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderItemEntity
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.repository.CartRepository
import ru.foodbox.delivery.data.repository.OrderItemRepository
import ru.foodbox.delivery.data.repository.OrderRepository
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.services.dto.OrderDto
import ru.foodbox.delivery.services.mapper.OrderItemMapper
import ru.foodbox.delivery.services.mapper.OrderMapper
import kotlin.jvm.optionals.getOrNull

@Service
class OrderService(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val userRepository: UserRepository,
    private val orderMapper: OrderMapper,
    private val orderItemMapper: OrderItemMapper,
) {
    fun getOrder(id: Long): OrderDto? {
        val orderEntity = orderRepository.findById(id).getOrNull() ?: return null
        return orderMapper.toDto(orderEntity, orderItemMapper.toDto(orderEntity.items))
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, status: OrderStatus): OrderDto? {
        val order = orderRepository.findById(orderId).getOrNull()

        if (order == null) return null

        order.status = status
        val updatedOrder = orderRepository.save(order)
        return orderMapper.toDto(updatedOrder, orderItemMapper.toDto(updatedOrder.items))
    }

    fun createOrder(userId: Long): OrderDto {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден") }

        val pendingOrder = orderRepository.findFirstByUserAndStatus(userEntity, OrderStatus.PENDING)

        val cartEntity = cartRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Корзина не найдена")

        val newItems = mutableListOf<OrderItemEntity>()
        var subtotal = 0f

        val order = pendingOrder ?: OrderEntity(
            user = userEntity,
            status = OrderStatus.PENDING,
        )

        cartEntity.items.forEach { cartItem ->
            val itemTotal = cartItem.quantity * cartItem.product.price.toFloat()
            subtotal += itemTotal
            val orderItem = OrderItemEntity(
                order = order, // OK — ссылается на будущий order
                productId = cartItem.product.id,
                name = cartItem.product.title,
                quantity = cartItem.quantity,
                price = cartItem.product.price.toFloat()
            )
            newItems.add(orderItem)
        }

        order.items.clear() // Hibernate удалит старые как "orphan"
        order.items.addAll(newItems)

        order.deliveryPrice = cartEntity.deliveryPrice
        order.totalAmount = subtotal + cartEntity.deliveryPrice

        val savedOrderEntity = orderRepository.save(order)

        return orderMapper.toDto(savedOrderEntity, orderItemMapper.toDto(savedOrderEntity.items))
    }
}