package ru.foodbox.delivery.modules.cart.application

import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemCommand
import ru.foodbox.delivery.modules.cart.application.command.ChangeCartItemQuantityCommand
import ru.foodbox.delivery.modules.cart.application.command.UpdateCartDeliveryCommand
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryDraft
import java.util.UUID

interface CartService {
    fun getOrCreateActiveCart(actor: CurrentActor): Cart
    fun addItem(actor: CurrentActor, command: AddCartItemCommand): Cart
    fun changeQuantity(actor: CurrentActor, command: ChangeCartItemQuantityCommand): Cart
    fun removeItem(actor: CurrentActor, productId: UUID, variantId: UUID?): Cart
    fun clear(actor: CurrentActor): Cart
    fun getDeliveryDraft(actor: CurrentActor): CartDeliveryDraft?
    fun updateDeliveryDraft(actor: CurrentActor, command: UpdateCartDeliveryCommand): CartDeliveryDraft
    fun mergeGuestCartIntoUser(userId: UUID, installId: String): Cart
    fun markOrdered(cartId: UUID)
}
