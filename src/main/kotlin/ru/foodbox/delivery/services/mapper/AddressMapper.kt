package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.AddressEntity
import ru.foodbox.delivery.services.dto.AddressDto

@Component
class AddressMapper {
    fun toEntity(dto: AddressDto) = AddressEntity(
        lat = dto.latitude,
        long = dto.longitude,
        city = dto.city,
        street = dto.street,
        house = dto.house,
        flat = dto.flat,
        intercome = dto.intercome,
        comment = dto.comment
    )

    fun toDto(entity: AddressEntity) = AddressDto(
        latitude = entity.lat,
        longitude = entity.long,
        city = entity.city,
        street = entity.street,
        house = entity.house,
        flat = entity.flat,
        intercome = entity.intercome,
        comment = entity.comment
    )
}