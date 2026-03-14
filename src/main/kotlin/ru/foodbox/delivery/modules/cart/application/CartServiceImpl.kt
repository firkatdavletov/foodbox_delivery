package ru.foodbox.delivery.modules.cart.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.catalog.application.ProductReadService
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemCommand
import ru.foodbox.delivery.modules.cart.application.command.ChangeCartItemQuantityCommand
import ru.foodbox.delivery.modules.cart.application.policy.CartMergePolicy
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartItem
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.domain.repository.CartRepository
import java.time.Instant
import java.util.UUID

@Service
class CartServiceImpl(
    private val cartRepository: CartRepository,
    private val productReadService: ProductReadService,
    private val cartMergePolicy: CartMergePolicy,
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
}
