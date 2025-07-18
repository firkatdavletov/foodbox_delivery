package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderItemEntity
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.repository.CartRepository
import ru.foodbox.delivery.data.repository.OrderRepository
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.services.dto.OrderDto
import ru.foodbox.delivery.services.mapper.OrderItemMapper
import ru.foodbox.delivery.services.mapper.OrderMapper

@Service
class OrderService(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val orderMapper: OrderMapper,
    private val orderItemMapper: OrderItemMapper,
) {
    fun createOrder(userId: Long): OrderDto {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден") }

        val pendingOrder = orderRepository.findFirstByUserAndStatus(userEntity, OrderStatus.PENDING)

        val cartEntity = cartRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Корзина не найдена")

        val orderItems = mutableListOf<OrderItemEntity>()
        var subtotal = 0f

        val order = pendingOrder ?: OrderEntity(
            user = userEntity,
            status = OrderStatus.PENDING,
        )

        cartEntity.items.forEach { cartItem ->
            val itemTotal = cartItem.quantity * cartItem.product.price.toFloat()
            subtotal += itemTotal
            val orderItem = OrderItemEntity(
                order = order,
                productId = cartItem.product.id,
                name = cartItem.product.title,
                quantity = cartItem.quantity,
                price = cartItem.product.price.toFloat()
            )
            orderItems.add(orderItem)
        }

        order.copy(
            items = orderItems,
            totalAmount = subtotal + cartEntity.deliveryPrice,
            deliveryPrice = cartEntity.deliveryPrice,
        )

        val savedOrderEntity = orderRepository.save(order)
        val items = orderItemMapper.toDto(savedOrderEntity.items)

        return orderMapper.toDto(savedOrderEntity, items)
    }
}