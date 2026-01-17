package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.yandex_map_client.entity.GeoObject
import ru.foodbox.delivery.services.dto.CityDto
import ru.foodbox.delivery.services.dto.GeoAddressDto
import ru.foodbox.delivery.services.model.DeliveryInfo

@Component
class GeoAddressMapper {
    fun toDto(entity: GeoObject, cityDto: CityDto, deliveryInfo: DeliveryInfo, deliveryTime: Int, entrance: Int?): GeoAddressDto? {
        val components = entity.metaDataProperty.geocoderMetaData.address.components
        val (longitude, latitude) = entity.point.pos.split(" ")
        return GeoAddressDto(
            city = cityDto,
            street = components.firstOrNull { it.kind == "street" }?.name ?: return null,
            house = components.firstOrNull { it.kind == "house" }?.name ?: return null,
            entrance = entrance,
            deliveryInfo = deliveryInfo,
            deliveryTime = deliveryTime,
            latitude = latitude.toDoubleOrNull() ?: return null,
            longitude = longitude.toDoubleOrNull() ?: return null,
            uri = null
        )
    }
}