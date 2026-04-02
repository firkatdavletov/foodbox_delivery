package ru.foodbox.delivery.modules.cart.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryDraft
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryQuote
import ru.foodbox.delivery.modules.cart.domain.CartItem
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.modifier.domain.CartItemModifier
import ru.foodbox.delivery.modules.cart.domain.repository.CartRepository
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity.CartDeliveryDraftEntity
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity.CartEntity
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity.CartItemEntity
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity.CartItemModifierEntity
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.jpa.CartJpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.embedded.DeliveryAddressEmbeddable
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
                    id = item.id,
                    cart = entity,
                    productId = item.productId,
                    variantId = item.variantId,
                    title = item.title,
                    unit = item.unit,
                    countStep = item.countStep,
                    quantity = item.quantity,
                    priceMinor = item.priceMinor,
                    createdAt = item.createdAt,
                ).apply {
                    modifiers.addAll(
                        item.modifiers.map { modifier ->
                            CartItemModifierEntity(
                                id = UUID.randomUUID(),
                                cartItem = this,
                                modifierGroupId = modifier.modifierGroupId,
                                modifierOptionId = modifier.modifierOptionId,
                                groupCodeSnapshot = modifier.groupCodeSnapshot,
                                groupNameSnapshot = modifier.groupNameSnapshot,
                                optionCodeSnapshot = modifier.optionCodeSnapshot,
                                optionNameSnapshot = modifier.optionNameSnapshot,
                                applicationScopeSnapshot = modifier.applicationScopeSnapshot,
                                priceSnapshot = modifier.priceSnapshot,
                                quantity = modifier.quantity,
                            )
                        }
                    )
                }
            }
        )

        entity.deliveryDraft = cart.deliveryDraft?.let { draft ->
            val draftEntity = entity.deliveryDraft ?: CartDeliveryDraftEntity(
                id = UUID.randomUUID(),
                cart = entity,
                deliveryMethod = draft.deliveryMethod,
                createdAt = draft.createdAt,
                updatedAt = draft.updatedAt,
            )
            draftEntity.deliveryMethod = draft.deliveryMethod
            draftEntity.deliveryAddress = DeliveryAddressEmbeddable.fromDomain(draft.deliveryAddress)
            draftEntity.pickupPointId = draft.pickupPointId
            draftEntity.pickupPointExternalId = draft.pickupPointExternalId
            draftEntity.pickupPointName = draft.pickupPointName
            draftEntity.pickupPointAddress = draft.pickupPointAddress
            draftEntity.quoteAvailable = draft.quote?.available
            draftEntity.quotePriceMinor = draft.quote?.priceMinor
            draftEntity.quoteCurrency = draft.quote?.currency
            draftEntity.quoteZoneCode = draft.quote?.zoneCode
            draftEntity.quoteZoneName = draft.quote?.zoneName
            draftEntity.quoteEstimatedDays = draft.quote?.estimatedDays
            draftEntity.quoteEstimatesMinutes = draft.quote?.estimatesMinutes
            draftEntity.quoteMessage = draft.quote?.message
            draftEntity.quoteCalculatedAt = draft.quote?.calculatedAt
            draftEntity.quoteExpiresAt = draft.quote?.expiresAt
            draftEntity.updatedAt = draft.updatedAt
            draftEntity
        }

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
                    id = item.id,
                    productId = item.productId,
                    variantId = item.variantId,
                    title = item.title,
                    unit = item.unit,
                    countStep = item.countStep,
                    quantity = item.quantity,
                    priceMinor = item.priceMinor,
                    modifiers = item.modifiers.map { modifier ->
                        CartItemModifier(
                            modifierGroupId = modifier.modifierGroupId,
                            modifierOptionId = modifier.modifierOptionId,
                            groupCodeSnapshot = modifier.groupCodeSnapshot,
                            groupNameSnapshot = modifier.groupNameSnapshot,
                            optionCodeSnapshot = modifier.optionCodeSnapshot,
                            optionNameSnapshot = modifier.optionNameSnapshot,
                            applicationScopeSnapshot = modifier.applicationScopeSnapshot,
                            priceSnapshot = modifier.priceSnapshot,
                            quantity = modifier.quantity,
                        )
                    },
                    createdAt = item.createdAt,
                )
            }.toMutableList(),
            deliveryDraft = entity.deliveryDraft?.toDomain(),
            totalPriceMinor = entity.totalPriceMinor,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        ).apply {
            recalculateTotalPrice()
        }
    }

    private fun CartDeliveryDraftEntity.toDomain(): CartDeliveryDraft {
        return CartDeliveryDraft(
            deliveryMethod = deliveryMethod,
            deliveryAddress = deliveryAddress?.toDomain(),
            pickupPointId = pickupPointId,
            pickupPointExternalId = pickupPointExternalId,
            pickupPointName = pickupPointName,
            pickupPointAddress = pickupPointAddress,
            quote = if (quoteCalculatedAt != null && quoteExpiresAt != null && quoteCurrency != null) {
                CartDeliveryQuote(
                    available = quoteAvailable ?: false,
                    priceMinor = quotePriceMinor,
                    currency = quoteCurrency!!,
                    zoneCode = quoteZoneCode,
                    zoneName = quoteZoneName,
                    estimatedDays = quoteEstimatedDays,
                    estimatesMinutes = quoteEstimatesMinutes,
                    message = quoteMessage,
                    calculatedAt = quoteCalculatedAt!!,
                    expiresAt = quoteExpiresAt!!,
                )
            } else {
                null
            },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
