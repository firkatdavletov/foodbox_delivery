package ru.foodbox.delivery.modules.delivery.domain

data class DeliveryAddress(
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val street: String? = null,
    val house: String? = null,
    val apartment: String? = null,
    val postalCode: String? = null,
    val entrance: String? = null,
    val floor: String? = null,
    val intercom: String? = null,
    val comment: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    fun normalized(): DeliveryAddress {
        return copy(
            country = country.normalizedText(),
            region = region.normalizedText(),
            city = city.normalizedText(),
            street = street.normalizedText(),
            house = house.normalizedText(),
            apartment = apartment.normalizedText(),
            postalCode = postalCode.normalizedText(),
            entrance = entrance.normalizedText(),
            floor = floor.normalizedText(),
            intercom = intercom.normalizedText(),
            comment = comment.normalizedText(),
        )
    }

    fun patchWith(update: DeliveryAddress): DeliveryAddress {
        return copy(
            country = update.country ?: country,
            region = update.region ?: region,
            city = update.city ?: city,
            street = update.street ?: street,
            house = update.house ?: house,
            apartment = update.apartment ?: apartment,
            postalCode = update.postalCode ?: postalCode,
            entrance = update.entrance ?: entrance,
            floor = update.floor ?: floor,
            intercom = update.intercom ?: intercom,
            comment = update.comment ?: comment,
            latitude = update.latitude ?: latitude,
            longitude = update.longitude ?: longitude,
        )
    }

    fun clearCheckoutDetails(): DeliveryAddress {
        return copy(
            entrance = null,
            floor = null,
            intercom = null,
            comment = null,
        )
    }

    fun hasSameLocationAs(other: DeliveryAddress?): Boolean {
        return other != null && locationIdentity() == other.locationIdentity()
    }

    fun isEmpty(): Boolean {
        return listOf(
            country,
            region,
            city,
            street,
            house,
            apartment,
            postalCode,
            entrance,
            floor,
            intercom,
            comment,
        ).all { it.isNullOrBlank() } && latitude == null && longitude == null
    }

    fun toSingleLine(): String {
        return listOfNotNull(
            country,
            region,
            city,
            buildStreetLine(),
            apartment?.let { "apt. $it" },
            entrance?.let { "entrance $it" },
            floor?.let { "floor $it" },
            intercom?.let { "intercom $it" },
            postalCode,
            comment,
        ).joinToString(", ")
    }

    private fun buildStreetLine(): String? {
        val normalizedStreet = street?.takeIf { it.isNotBlank() }
        val normalizedHouse = house?.takeIf { it.isNotBlank() }
        return when {
            normalizedStreet == null && normalizedHouse == null -> null
            normalizedStreet == null -> "house $normalizedHouse"
            normalizedHouse == null -> normalizedStreet
            else -> "$normalizedStreet, house $normalizedHouse"
        }
    }

    private fun locationIdentity(): DeliveryAddress {
        return copy(
            entrance = null,
            floor = null,
            intercom = null,
            comment = null,
        )
    }
}

private fun String?.normalizedText(): String? {
    val trimmed = this?.trim()?.replace(Regex("\\s+"), " ")
    return trimmed?.takeIf { it.isNotBlank() }
}
