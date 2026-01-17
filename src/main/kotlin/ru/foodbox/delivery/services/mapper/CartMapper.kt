package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CartEntity
import ru.foodbox.delivery.services.dto.CartDto
import ru.foodbox.delivery.services.model.DeliveryInfo

@Component
class CartMapper(
    private val cartItemMapper: CartItemMapper,
    private val addressMapper: AddressMapper,
    private val departmentMapper: DepartmentMapper,
) {
    fun toDto(
        entity: CartEntity,
    ) = CartDto(
        items = cartItemMapper.toDto(entity.items),
        deliveryType = entity.deliveryType,
        deliveryInfo = DeliveryInfo(
            deliveryPrice = entity.deliveryPrice,
            freeDeliveryPrice = entity.freeDeliveryPrice,
        ),
        totalPrice = entity.totalPrice,
        deliveryAddress = entity.deliveryAddress?.let { addressMapper.toDto(it) },
        department = departmentMapper.toDto(entity.department),
        comment = entity.comment
    )
}