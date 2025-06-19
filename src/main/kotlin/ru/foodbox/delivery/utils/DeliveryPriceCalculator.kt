package ru.foodbox.delivery.utils

import org.springframework.stereotype.Service

@Service
class DeliveryPriceCalculator(
    private val distanceCalculator: DistanceCalculator,
) {
    fun calculateDeliveryPrice(lat: Double, lon: Double): Double {
        val distance = distanceCalculator.haversineDistance(
            lat1 = 53.970216,
            lon1 = 58.407499,
            lat2 = lat,
            lon2 = lon,
        )

        val deliveryPrice = when (distance) {
            in 0.0..< 100.0 -> 0.0
            in 100.0..1000.0 -> 300.0
            else -> 500.0
        }

        return deliveryPrice
    }
}