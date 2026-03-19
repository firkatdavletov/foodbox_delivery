package ru.foodbox.delivery.modules.cart.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.catalog.application.ProductReadService
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemCommand
import ru.foodbox.delivery.modules.cart.application.command.ChangeCartItemQuantityCommand
import ru.foodbox.delivery.modules.cart.application.command.UpdateCartDeliveryCommand
import ru.foodbox.delivery.modules.cart.application.policy.CartMergePolicy
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryDraft
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryQuote
import ru.foodbox.delivery.modules.cart.domain.CartItem
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.domain.repository.CartRepository
import ru.foodbox.delivery.modules.delivery.application.DeliveryService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class CartServiceImpl(
    private val cartRepository: CartRepository,
    private val productReadService: ProductReadService,
    private val cartMergePolicy: CartMergePolicy,
    private val deliveryService: DeliveryService,
) : CartService {

    override fun getOrCreateActiveCart(actor: CurrentActor): Cart {
        val owner = toOwner(actor)
        return cartRepository.findActiveByOwner(owner) ?: createNewActiveCart(owner)
    }

    @Transactional
    override fun addItem(actor: CurrentActor, command: AddCartItemCommand): Cart {
        val product = productReadService.getActiveProductSnapshot(
            productId = command.productId,
            variantId = command.variantId,
        ) ?: throw NotFoundException("Product not found")

        val cart = getOrCreateActiveCart(actor)
        cart.addItem(
            CartItem(
                productId = product.id,
                variantId = product.variantId,
                title = product.title,
                unit = product.unit,
                countStep = product.countStep,
                quantity = command.quantity,
                priceMinor = product.priceMinor,
            )
        )
        return cartRepository.save(cart)
    }

    @Transactional
    override fun changeQuantity(actor: CurrentActor, command: ChangeCartItemQuantityCommand): Cart {
        val cart = getOrCreateActiveCart(actor)
        cart.changeQuantity(command.productId, command.variantId, command.quantity)
        return cartRepository.save(cart)
    }

    @Transactional
    override fun removeItem(actor: CurrentActor, productId: UUID, variantId: UUID?): Cart {
        val cart = getOrCreateActiveCart(actor)
        cart.removeItem(productId, variantId)
        return cartRepository.save(cart)
    }

    @Transactional
    override fun clear(actor: CurrentActor): Cart {
        val cart = getOrCreateActiveCart(actor)
        cart.clear()
        return cartRepository.save(cart)
    }

    override fun getDeliveryDraft(actor: CurrentActor): CartDeliveryDraft? {
        return getOrCreateActiveCart(actor).deliveryDraft
    }

    @Transactional
    override fun updateDeliveryDraft(actor: CurrentActor, command: UpdateCartDeliveryCommand): CartDeliveryDraft {
        val cart = getOrCreateActiveCart(actor)
        if (cart.items.isEmpty()) {
            throw IllegalArgumentException("Cannot select delivery for an empty cart")
        }

        val deliveryAddress = if (command.deliveryMethod.requiresAddress) {
            command.deliveryAddress?.normalized()
        } else {
            null
        }
        val pickupPointId = when (command.deliveryMethod) {
            DeliveryMethodType.PICKUP -> command.pickupPointId
            else -> null
        }
        val pickupPointExternalId = when (command.deliveryMethod) {
            DeliveryMethodType.YANDEX_PICKUP_POINT -> command.pickupPointExternalId?.trim()?.takeIf { it.isNotBlank() }
            else -> null
        }
        val now = Instant.now()
        val quote = deliveryService.calculateQuote(
            DeliveryQuoteContext(
                cartId = cart.id,
                subtotalMinor = cart.totalPriceMinor,
                itemCount = cart.items.sumOf { it.quantity },
                deliveryMethod = command.deliveryMethod,
                deliveryAddress = deliveryAddress,
                pickupPointId = pickupPointId,
                pickupPointExternalId = pickupPointExternalId,
            )
        )

        cart.upsertDeliveryDraft(
            CartDeliveryDraft(
                deliveryMethod = command.deliveryMethod,
                deliveryAddress = deliveryAddress,
                pickupPointId = quote.pickupPointId ?: pickupPointId,
                pickupPointExternalId = quote.pickupPointExternalId ?: pickupPointExternalId,
                pickupPointName = quote.pickupPointName,
                pickupPointAddress = quote.pickupPointAddress,
                quote = CartDeliveryQuote(
                    available = quote.available,
                    priceMinor = quote.priceMinor,
                    currency = quote.currency,
                    zoneCode = quote.zoneCode,
                    zoneName = quote.zoneName,
                    estimatedDays = quote.estimatedDays,
                    message = quote.message,
                    calculatedAt = now,
                    expiresAt = now.plus(QUOTE_TTL_MINUTES, ChronoUnit.MINUTES),
                ),
                createdAt = cart.deliveryDraft?.createdAt ?: now,
                updatedAt = now,
            )
        )

        return cartRepository.save(cart).deliveryDraft
            ?: throw IllegalStateException("Cart delivery draft was not saved")
    }

    @Transactional
    override fun mergeGuestCartIntoUser(userId: UUID, installId: String): Cart {
        val userOwner = CartOwner(type = CartOwnerType.USER, value = userId.toString())
        val guestOwner = CartOwner(type = CartOwnerType.INSTALLATION, value = installId)

        val guestCart = cartRepository.findActiveByOwner(guestOwner)
        val userCart = cartRepository.findActiveByOwner(userOwner)

        return when {
            guestCart == null && userCart == null -> createNewActiveCart(userOwner)
            guestCart == null && userCart != null -> userCart
            guestCart != null && userCart == null -> {
                guestCart.reassignOwner(userOwner)
                cartRepository.save(guestCart)
            }
            guestCart != null && userCart != null -> {
                val merged = cartMergePolicy.merge(source = guestCart, target = userCart)
                guestCart.markMerged()
                cartRepository.save(guestCart)
                cartRepository.save(merged)
            }
            else -> error("Unreachable state")
        }
    }

    @Transactional
    override fun markOrdered(cartId: UUID) {
        val cart = cartRepository.findById(cartId) ?: return
        if (cart.status == CartStatus.ACTIVE) {
            cart.markOrdered()
            cartRepository.save(cart)
        }
    }

    private fun createNewActiveCart(owner: CartOwner): Cart {
        val now = Instant.now()
        return cartRepository.save(
            Cart(
                id = UUID.randomUUID(),
                owner = owner,
                status = CartStatus.ACTIVE,
                items = mutableListOf(),
                deliveryDraft = null,
                totalPriceMinor = 0,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    private fun toOwner(actor: CurrentActor): CartOwner {
        return when (actor) {
            is CurrentActor.User -> CartOwner(CartOwnerType.USER, actor.userId.toString())
            is CurrentActor.Guest -> CartOwner(CartOwnerType.INSTALLATION, actor.installId)
        }
    }

    private companion object {
        private const val QUOTE_TTL_MINUTES = 15L
    }
}
