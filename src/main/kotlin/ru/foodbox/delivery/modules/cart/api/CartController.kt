package ru.foodbox.delivery.modules.cart.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.common.web.CurrentActorParam
import ru.foodbox.delivery.modules.cart.api.mapper.CartResponseMapper
import ru.foodbox.delivery.modules.cart.api.request.AddCartItemRequest
import ru.foodbox.delivery.modules.cart.api.request.ChangeCartItemQuantityRequest
import ru.foodbox.delivery.modules.cart.api.response.CartResponse
import ru.foodbox.delivery.modules.cart.application.CartServiceImpl
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemCommand
import ru.foodbox.delivery.modules.cart.application.command.ChangeCartItemQuantityCommand

@RestController
@RequestMapping("api/v1/cart")
class CartController(
    private val cartService: CartServiceImpl,
    private val cartResponseMapper: CartResponseMapper,
) {
    @GetMapping
    fun getCart(
        @CurrentActorParam actor: CurrentActor,
    ): CartResponse {
        val cartDto = cartService.getOrCreateActiveCart(actor)
        return cartResponseMapper.toResponse(cartDto)
    }

    @PostMapping("/items")
    fun addItem(
        @CurrentActorParam actor: CurrentActor,
        @Valid @RequestBody request: AddCartItemRequest
    ): CartResponse {
        val cart = cartService.addItem(
            actor = actor,
            command = AddCartItemCommand(
                productId = request.productId,
                quantity = request.quantity
            )
        )
        return cartResponseMapper.toResponse(cart)
    }

    @PatchMapping("/items")
    fun changeQuantity(
        @CurrentActorParam actor: CurrentActor,
        @Valid @RequestBody request: ChangeCartItemQuantityRequest
    ): CartResponse {
        val cart = cartService.changeQuantity(
            actor = actor,
            command = ChangeCartItemQuantityCommand(
                productId = request.productId,
                quantity = request.quantity
            )
        )
        return cartResponseMapper.toResponse(cart)
    }

    @DeleteMapping("/items/{productId}")
    fun removeItem(
        @CurrentActorParam actor: CurrentActor,
        @PathVariable productId: Long
    ): CartResponse {
        val cart = cartService.removeItem(actor, productId)
        return cartResponseMapper.toResponse(cart)
    }

    @DeleteMapping
    fun clear(
        @CurrentActorParam actor: CurrentActor
    ): CartResponse {
        val cart = cartService.clear(actor)
        return cartResponseMapper.toResponse(cart)
    }
}