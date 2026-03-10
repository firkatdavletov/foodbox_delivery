package ru.foodbox.delivery.modules.cart.infrastructure.persistance.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.infrastructure.persistance.entity.CartEntity
import java.util.UUID

interface CartJpaRepository : JpaRepository<CartEntity, UUID> {
    fun findByOwnerTypeAndOwnerValueAndStatus(
        ownerType: CartOwnerType,
        ownerValue: String,
        status: CartStatus
    ): CartEntity?
}