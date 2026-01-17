package ru.foodbox.delivery.utils

import org.springframework.stereotype.Service
import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.dto.CityDto
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.model.DeliveryInfo

@Service
class DeliveryPriceCalculator(
    private val distanceCalculator: DistanceCalculator,
) {
    fun calculateDeliveryPrice(
        deliveryType: DeliveryType,
        lat: Double?,
        lon: Double?,
        cityId: Long?,
        department: DepartmentDto
    ): DeliveryInfo? {
        if (deliveryType == DeliveryType.PICKUP) {
            return DeliveryInfo(0.0, 0.0)
        }
        val deliveryInfo = if (department.city.id == cityId && lat != null && lon != null) {
            val distanceInMetres = distanceCalculator.haversineDistance(
                lat1 = department.latitude,
                lon1 = department.longitude,
                lat2 = lat,
                lon2 = lon,
            )
            calculateDeliveryPrice(distanceInMetres)
        } else if (department.city.subCities.any { it.id == cityId }) {
            DeliveryInfo(250.0, null)
        } else {
            null
        }
        return deliveryInfo
    }

    fun calculateDeliveryPrice(distanceInMetres: Double): DeliveryInfo {
        val (deliveryPrice, freeDeliveryPrice) = when (distanceInMetres) {
            in 0.0..< 1340.0 -> 100.0 to 1200.0
            in 1340.0..2740.0 -> 100.0 to null
            else -> 200.0 to null
        }

        return DeliveryInfo(deliveryPrice, freeDeliveryPrice)
    }
}