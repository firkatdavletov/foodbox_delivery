package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CartEntity
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.CartDto
import ru.foodbox.delivery.services.dto.CartItemDto

@Component
class CartMapper {
    fun toDto(
        entity: CartEntity,
        items: List<CartItemDto>,
        addressDto: AddressDto?,
    ) = CartDto(
        items = items,
        deliveryType = entity.deliveryType,
        deliveryPrice = entity.deliveryPrice,
        totalPrice = entity.totalPrice,
        deliveryAddress = addressDto,
        departmentId = entity.department?.id,
    )
}