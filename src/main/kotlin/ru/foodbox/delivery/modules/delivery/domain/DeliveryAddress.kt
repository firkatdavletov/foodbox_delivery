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
}

private fun String?.normalizedText(): String? {
    val trimmed = this?.trim()?.replace(Regex("\\s+"), " ")
    return trimmed?.takeIf { it.isNotBlank() }
}
