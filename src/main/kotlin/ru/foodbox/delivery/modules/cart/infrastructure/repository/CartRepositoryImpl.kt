package ru.foodbox.delivery.modules.cart.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartItem
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.domain.repository.CartRepository
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity.CartEntity
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity.CartItemEntity
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.jpa.CartJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class CartRepositoryImpl(
    private val jpaRepository: CartJpaRepository,
) : CartRepository {

    override fun findById(cartId: UUID): Cart? {
        val entity = jpaRepository.findById(cartId).getOrNull() ?: return null
        return toDomain(entity)
    }

    override fun findActiveByOwner(owner: CartOwner): Cart? {
        val entity = jpaRepository.findByOwnerTypeAndOwnerIdAndStatus(
            ownerType = owner.type,
            ownerId = owner.value,
            status = CartStatus.ACTIVE,
        ) ?: return null

        return toDomain(entity)
    }

    override fun save(cart: Cart): Cart {
        val existing = jpaRepository.findById(cart.id).getOrNull()
        val entity = existing ?: CartEntity(
            id = cart.id,
            ownerType = cart.owner.type,
            ownerId = cart.owner.value,
            status = cart.status,
            totalPriceMinor = cart.totalPriceMinor,
            createdAt = cart.createdAt,
            updatedAt = cart.updatedAt,
        )

        entity.ownerType = cart.owner.type
        entity.ownerId = cart.owner.value
        entity.status = cart.status
        entity.totalPriceMinor = cart.totalPriceMinor
        entity.updatedAt = cart.updatedAt

        entity.items.clear()
        entity.items.addAll(
            cart.items.map { item ->
                CartItemEntity(
                    id = UUID.randomUUID(),
                    cart = entity,
                    productId = item.productId,
                    title = item.title,
                    unit = item.unit,
                    countStep = item.countStep,
                    quantity = item.quantity,
                    priceMinor = item.priceMinor,
                    createdAt = Instant.now(),
                )
            }
        )

        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    private fun toDomain(entity: CartEntity): Cart {
        return Cart(
            id = entity.id,
            owner = CartOwner(
                type = entity.ownerType,
                value = entity.ownerId,
            ),
            status = entity.status,
            items = entity.items.map { item ->
                CartItem(
                    productId = item.productId,
                    title = item.title,
                    unit = item.unit,
                    countStep = item.countStep,
                    quantity = item.quantity,
                    priceMinor = item.priceMinor,
                )
            }.toMutableList(),
            totalPriceMinor = entity.totalPriceMinor,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
