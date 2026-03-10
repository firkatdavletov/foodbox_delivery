package ru.foodbox.delivery.modules.cart.infrastructure.persistance.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.domain.repository.CartRepository
import ru.foodbox.delivery.modules.cart.infrastructure.mapper.CartMapper
import ru.foodbox.delivery.modules.cart.infrastructure.persistance.entity.CartEntity
import ru.foodbox.delivery.modules.cart.infrastructure.persistance.jpa.CartJpaRepository

@Repository
class CartRepositoryImpl(
    private val cartJpaRepository: CartJpaRepository,
    private val cartMapper: CartMapper,
) : CartRepository {
    override fun findActiveByOwner(owner: CartOwner): Cart? {
        val entity = cartJpaRepository.findByOwnerTypeAndOwnerValueAndStatus(
            ownerType = owner.type,
            ownerValue = owner.value,
            status = CartStatus.ACTIVE
        ) ?: return null

        return cartMapper.toDto(entity)
    }

    override fun save(cart: Cart): Cart {
        val existing = cartJpaRepository.findById(cart.id).orElse(null)

        val entity = existing ?: CartEntity(
            id = cart.id,
            ownerId = cart.owner.value,
            ownerType = cart.owner.type,
            status = cart.status,
            createdAt = cart.createdAt,
            updatedAt = cart.updatedAt,

            )

        val saved = cartJpaRepository.save(entity)
        return cartMapper.toDto(saved)
    }
}