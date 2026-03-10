package ru.foodbox.delivery.modules.cart.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemCommand
import ru.foodbox.delivery.modules.cart.application.command.ChangeCartItemQuantityCommand
import ru.foodbox.delivery.modules.cart.application.policy.CartMergePolicy
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartOwner
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.infrastructure.mapper.CartMapper
import ru.foodbox.delivery.modules.cart.domain.repository.CartRepository
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
class CartServiceImpl(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val cartMapper: CartMapper,
    private val cartMergePolicy: CartMergePolicy,
) : CartService {

    override fun getOrCreateActiveCart(actor: CurrentActor): Cart {
        val owner = cartMapper.toOwner(actor)
        return cartRepository.findActiveByOwner(owner) ?: createNewActiveCart(owner)
    }

    @Transactional
    override fun addItem(actor: CurrentActor, command: AddCartItemCommand): Cart {
        val product = productRepository.findById(command.productId).getOrNull()
            ?: throw NotFoundException("Product not found")

        require(product.isActive) { "Product is not available" }

        val cart = getOrCreateActiveCart(actor)
        cart.addItem(
            productId = product.id!!,
            quantity = command.quantity,
            priceSnapshot = product.price,
            title = product.title,
            unit = product.unit,
            countStep = product.countStep,
        )
        return cartRepository.save(cart)
    }

    @Transactional
    override fun changeQuantity(
        actor: CurrentActor,
        command: ChangeCartItemQuantityCommand
    ): Cart {
        val cart = getOrCreateActiveCart(actor)
        cart.changeQuantity(
            productId = command.productId,
            quantity = command.quantity
        )
        return cartRepository.save(cart)
    }

    @Transactional
    override fun removeItem(
        actor: CurrentActor,
        productId: Long
    ): Cart {
        val cart = getOrCreateActiveCart(actor)
        cart.removeItem(productId)
        return cartRepository.save(cart)
    }

    @Transactional
    override fun clear(actor: CurrentActor): Cart {
        val cart = getOrCreateActiveCart(actor)
        cart.clear()
        return cartRepository.save(cart)
    }

    @Transactional
    override fun mergeGuestCartIntoUser(
        userActor: CurrentActor.User,
        installId: String
    ): Cart {
        val userOwner = cartMapper.toOwner(userActor)
        val guestOwner = CartOwner(
            type = CartOwnerType.INSTALLATION,
            value = installId
        )

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

    private fun createNewActiveCart(owner: CartOwner): Cart {
        val instant = LocalDateTime.now()
        return cartRepository.save(
            Cart(
                id = UUID.randomUUID(),
                owner = owner,
                status = CartStatus.ACTIVE,
                createdAt = instant,
                updatedAt = instant,
            )
        )
    }
}