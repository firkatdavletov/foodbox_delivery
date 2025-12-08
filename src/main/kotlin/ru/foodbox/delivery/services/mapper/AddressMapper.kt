package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.AddressEntity
import ru.foodbox.delivery.data.entities.CityEntity
import ru.foodbox.delivery.services.dto.AddressDto

@Component
class AddressMapper(
    private val cityMapper: CityMapper,
) {
    fun toDto(entity: AddressEntity) = AddressDto(
        street = entity.street,
        house = entity.house,
        entrance = entity.entrance,
        flat = entity.flat,
        intercome = entity.intercome,
        comment = entity.comment,
        city = cityMapper.toDto(entity.cityEntity),
        latitude = entity.latitude,
        longitude = entity.longitude
    )

    fun toEntity(dto: AddressDto, cityEntity: CityEntity) = AddressEntity(
        street = dto.street,
        house = dto.house,
        entrance = dto.entrance,
        flat = dto.flat,
        intercome = dto.intercome,
        comment = dto.comment,
        cityEntity = cityEntity,
        latitude = dto.latitude,
        longitude = dto.longitude
    )
}