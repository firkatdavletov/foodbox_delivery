package ru.foodbox.delivery.data.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.data.entities.OrderEntity
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.entities.UserEntity
import ru.foodbox.delivery.services.dto.OrderPreviewDto

interface OrderRepository: JpaRepository<OrderEntity, Long> {
    fun findByUserAndStatusInOrderByCreatedDesc(userEntity: UserEntity, status: Set<OrderStatus>): List<OrderEntity>

    @Modifying
    @Query(
        value = "UPDATE orders SET status = :status where id = :id",
        nativeQuery = true
    )
    fun updateOrderStatus(@Param("id") orderId: Long, @Param("status") status: String): Int

    @Query(
        value = """
            select new ru.foodbox.delivery.services.dto.OrderPreviewDto(
                o.id,
                o.totalAmount,
                o.status,
                c.name,
                c.company,
                o.deliveryTime
            )
            from OrderEntity o
            join o.user c
        """,
        countQuery = """
            select count(o)
            from OrderEntity o
        """
    )
    fun findOrderPreviews(pageable: Pageable): Page<OrderPreviewDto>
}
