package ru.foodbox.delivery.utils

import ru.foodbox.delivery.services.dto.AddressDto

object AddressUtility {

    fun addressString(model: AddressDto): String {
        return buildString {
            append(model.city.name)
            append(", ")
            append(model.street)
            append(", ")
            append(model.house)
            if (model.entrance != null && model.flat != null) {
                append(", подъезд ")
                append(model.entrance)
            }
            if (model.flat != null) {
                append(", кв.")
                append(model.flat)
            }
        }
    }
}