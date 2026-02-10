package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CartEntity
import ru.foodbox.delivery.services.dto.CartDto
import ru.foodbox.delivery.services.model.DeliveryInfo
import java.math.BigDecimal

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
            deliveryPrice = entity.deliveryPrice
                .multiply(BigDecimal(100))
                .longValueExact(),
            freeDeliveryPrice = entity.freeDeliveryPrice
                ?.multiply(BigDecimal(100))
                ?.longValueExact(),
        ),
        totalPrice = entity.totalPrice
            .multiply(BigDecimal(100))
            .longValueExact(),
        deliveryAddress = entity.deliveryAddress?.let { addressMapper.toDto(it) },
        department = departmentMapper.toDto(entity.department),
        comment = entity.comment
    )
}