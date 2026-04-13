package ru.foodbox.delivery.modules.cart.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity.CartEntity
import java.util.UUID

interface CartJpaRepository : JpaRepository<CartEntity, UUID> {
    fun countByStatus(status: CartStatus): Long
    fun findByOwnerTypeAndOwnerIdAndStatus(
        ownerType: CartOwnerType,
        ownerId: String,
        status: CartStatus,
    ): CartEntity?
}
