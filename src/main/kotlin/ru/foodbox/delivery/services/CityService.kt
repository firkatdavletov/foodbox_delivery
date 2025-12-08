package ru.foodbox.delivery.services

import org.springframework.stereotype.Service
import ru.foodbox.delivery.data.repository.CityRepository
import ru.foodbox.delivery.services.dto.CityDto
import ru.foodbox.delivery.services.mapper.CityMapper

@Service
class CityService(
    private val cityRepository: CityRepository,
    private val cityMapper: CityMapper,
) {
    fun findCityByName(name: String): CityDto? {
        val cityEntity = cityRepository.findByName(name)
        return cityEntity?.let { cityMapper.toDto(it) }
    }
}