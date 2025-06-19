package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.yandex_map_client.YandexMapClient
import ru.foodbox.delivery.services.mapper.GeoAddressMapper
import ru.foodbox.delivery.services.dto.GeoAddressDto
import ru.foodbox.delivery.utils.DeliveryPriceCalculator

@Service
class MapService(
    private val yandexMapClient: YandexMapClient,
    private val deliveryPriceCalculator: DeliveryPriceCalculator,
    private val geoAddressMapper: GeoAddressMapper,
) {
    fun findAddress(lat: Double, lon: Double): GeoAddressDto {
        val geoObject = yandexMapClient.findAddress(lat, lon)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Адрес не найден")

        val deliveryPrice = deliveryPriceCalculator.calculateDeliveryPrice(lat, lon)

        return geoAddressMapper.toDto(geoObject, deliveryPrice, 20)
    }
}