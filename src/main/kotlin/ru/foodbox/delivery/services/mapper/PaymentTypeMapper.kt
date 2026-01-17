package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.PaymentTypeEntity
import ru.foodbox.delivery.services.dto.PaymentTypeDto

@Component
class PaymentTypeMapper {
    fun toDto(entity: PaymentTypeEntity) = PaymentTypeDto(entity.key, entity.title)

    fun toDto(entities: List<PaymentTypeEntity>) = entities.map { toDto(it) }
}