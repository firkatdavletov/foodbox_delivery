package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.yandex_map_client.entity.GeoObject
import ru.foodbox.delivery.services.dto.GeoAddressDto

@Component
class GeoAddressMapper {
    fun toDto(entity: GeoObject, deliveryPrice: Double, deliveryTime: Int): GeoAddressDto {
        val components = entity.metaDataProperty.geocoderMetaData.address.components
        val (longitude, latitude) = entity.point.pos.split(" ")
        return GeoAddressDto(
            city = components.firstOrNull { it.kind == "locality" }?.name,
            street = components.firstOrNull { it.kind == "street" }?.name,
            house = components.firstOrNull { it.kind == "house" }?.name,
            deliveryPrice = deliveryPrice,
            deliveryTime = deliveryTime,
            latitude = latitude.toDoubleOrNull(),
            longitude = longitude.toDoubleOrNull(),
        )
    }
}