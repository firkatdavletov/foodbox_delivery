package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Service
import ru.foodbox.delivery.data.entities.CityEntity
import ru.foodbox.delivery.services.dto.CityDto

@Service
class CityMapper {
    fun toDto(entity: CityEntity): CityDto = CityDto(
        id = entity.id ?: -1,
        name = entity.name,
        latitude = entity.latitude,
        longitude = entity.longitude,
        subCities = toDto(entity.subCities)
    )

    fun toDto(entities: List<CityEntity>): List<CityDto> = entities.map { toDto(it) }
}