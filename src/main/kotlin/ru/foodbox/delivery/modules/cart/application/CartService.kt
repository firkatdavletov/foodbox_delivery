package ru.foodbox.delivery.modules.cart.application

import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemCommand
import ru.foodbox.delivery.modules.cart.application.command.ChangeCartItemQuantityCommand
import ru.foodbox.delivery.modules.cart.domain.Cart

interface CartService {
    fun getOrCreateActiveCart(actor: CurrentActor): Cart
    fun addItem(actor: CurrentActor, command: AddCartItemCommand): Cart
    fun changeQuantity(actor: CurrentActor, command: ChangeCartItemQuantityCommand): Cart
    fun removeItem(actor: CurrentActor, productId: Long): Cart
    fun clear(actor: CurrentActor): Cart
    fun mergeGuestCartIntoUser(userActor: CurrentActor.User, installId: String): Cart
}