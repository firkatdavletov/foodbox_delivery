package ru.foodbox.delivery.modules.cart.api.mapper

import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.utils.PriceFormat
import ru.foodbox.delivery.modules.cart.api.response.CartItemResponse
import ru.foodbox.delivery.modules.cart.api.response.CartResponse
import ru.foodbox.delivery.modules.cart.domain.Cart

@Service
class CartResponseMapper {
    fun toResponse(dto: Cart): CartResponse {
        return CartResponse(
            items = dto.items.map {
                CartItemResponse(
                    productId = it.productId,
                    quantity = it.quantity,
                    priceSnapshot = PriceFormat.transformForApi(it.price)
                )
            },
            totalPrice = PriceFormat.transformForApi(dto.totalPrice),
        )
    }
}