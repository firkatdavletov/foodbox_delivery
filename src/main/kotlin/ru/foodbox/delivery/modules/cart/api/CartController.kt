package ru.foodbox.delivery.modules.cart.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.common.web.CurrentActorParam
import ru.foodbox.delivery.modules.cart.api.dto.AddCartItemRequest
import ru.foodbox.delivery.modules.cart.api.dto.CartDeliveryDraftResponse
import ru.foodbox.delivery.modules.cart.api.dto.CartResponse
import ru.foodbox.delivery.modules.cart.api.dto.ChangeCartItemQuantityRequest
import ru.foodbox.delivery.modules.cart.api.dto.PutCartDeliveryRequest
import ru.foodbox.delivery.modules.cart.application.CartService
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemCommand
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemModifierCommand
import ru.foodbox.delivery.modules.cart.application.command.ChangeCartItemQuantityCommand
import ru.foodbox.delivery.modules.cart.application.command.UpdateCartDeliveryCommand
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.catalog.application.CatalogImageService
import ru.foodbox.delivery.modules.delivery.api.dto.toDomain
import java.util.UUID

@RestController
@RequestMapping("/api/v1/cart")
class CartController(
    private val cartService: CartService,
    private val catalogImageService: CatalogImageService,
) {

    @GetMapping
    fun getCart(
        @CurrentActorParam actor: CurrentActor,
    ): CartResponse {
        return cartService.getOrCreateActiveCart(actor).toResponseWithImages()
    }

    @GetMapping("/delivery")
    fun getDeliveryDraft(
        @CurrentActorParam actor: CurrentActor,
    ): CartDeliveryDraftResponse? {
        return cartService.getDeliveryDraft(actor).toResponse()
    }

    @PutMapping("/delivery")
    fun putDeliveryDraft(
        @CurrentActorParam actor: CurrentActor,
        @Valid @RequestBody request: PutCartDeliveryRequest,
    ): CartDeliveryDraftResponse {
        return cartService.updateDeliveryDraft(
            actor = actor,
            command = UpdateCartDeliveryCommand(
                deliveryMethod = request.deliveryMethod,
                deliveryAddress = request.address?.toDomain(),
                pickupPointId = request.pickupPointId,
                pickupPointExternalId = request.pickupPointExternalId,
            ),
        ).toResponse()!!
    }

    @PostMapping("/items")
    fun addItem(
        @CurrentActorParam actor: CurrentActor,
        @Valid @RequestBody request: AddCartItemRequest,
    ): CartResponse {
        return cartService.addItem(
            actor = actor,
            command = AddCartItemCommand(
                productId = request.productId,
                variantId = request.variantId,
                quantity = request.quantity,
                modifiers = request.modifiers.map { modifier ->
                    AddCartItemModifierCommand(
                        modifierGroupId = modifier.modifierGroupId,
                        modifierOptionId = modifier.modifierOptionId,
                        quantity = modifier.quantity,
                    )
                },
            ),
        ).toResponseWithImages()
    }

    @PatchMapping("/items/{itemId}")
    fun changeQuantity(
        @CurrentActorParam actor: CurrentActor,
        @PathVariable itemId: UUID,
        @Valid @RequestBody request: ChangeCartItemQuantityRequest,
    ): CartResponse {
        return cartService.changeQuantity(
            actor = actor,
            command = ChangeCartItemQuantityCommand(
                itemId = itemId,
                quantity = request.quantity,
            ),
        ).toResponseWithImages()
    }

    @DeleteMapping("/items/{itemId}")
    fun removeItem(
        @CurrentActorParam actor: CurrentActor,
        @PathVariable itemId: UUID,
    ): CartResponse {
        return cartService.removeItem(actor = actor, itemId = itemId).toResponseWithImages()
    }

    @DeleteMapping
    fun clear(
        @CurrentActorParam actor: CurrentActor,
    ): CartResponse {
        return cartService.clear(actor).toResponseWithImages()
    }

    private fun Cart.toResponseWithImages(): CartResponse {
        val productIds = items.map { it.productId }.distinct()
        val thumbUrls = if (productIds.isNotEmpty()) {
            catalogImageService.getFirstProductThumbUrl(productIds)
        } else {
            emptyMap()
        }
        return toResponse(thumbUrls)
    }
}
