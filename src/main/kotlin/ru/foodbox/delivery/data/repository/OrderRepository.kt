package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.entities.UserEntity

interface OrderRepository: JpaRepository<OrderEntity, Long> {
    fun findFirstByUserAndStatus(userEntity: UserEntity, status: OrderStatus): OrderEntity?

    @Modifying
    @Query(
        value = "UPDATE orders SET status = :status where id = :id",
        nativeQuery = true
    )
    fun updateOrderStatus(@Param("id") orderId: Long, @Param("status") status: String): Int
}