package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.entities.UserEntity

interface OrderRepository: JpaRepository<OrderEntity, Long> {
    fun findFirstByUserAndStatus(userEntity: UserEntity, status: OrderStatus): OrderEntity?
}