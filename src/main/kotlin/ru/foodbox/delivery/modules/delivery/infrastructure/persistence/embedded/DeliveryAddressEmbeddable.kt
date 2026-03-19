package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.embedded

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress

@Embeddable
class DeliveryAddressEmbeddable(
    @Column(name = "country", length = 255)
    var country: String? = null,

    @Column(name = "region", length = 255)
    var region: String? = null,

    @Column(name = "city", length = 255)
    var city: String? = null,

    @Column(name = "street", length = 255)
    var street: String? = null,

    @Column(name = "house", length = 255)
    var house: String? = null,

    @Column(name = "apartment", length = 255)
    var apartment: String? = null,

    @Column(name = "postal_code", length = 64)
    var postalCode: String? = null,

    @Column(name = "entrance", length = 255)
    var entrance: String? = null,

    @Column(name = "floor", length = 255)
    var floor: String? = null,

    @Column(name = "intercom", length = 255)
    var intercom: String? = null,

    @Column(name = "address_comment", length = 500)
    var comment: String? = null,

    @Column(name = "latitude")
    var latitude: Double? = null,

    @Column(name = "longitude")
    var longitude: Double? = null,
) {
    fun toDomain(): DeliveryAddress {
        return DeliveryAddress(
            country = country,
            region = region,
            city = city,
            street = street,
            house = house,
            apartment = apartment,
            postalCode = postalCode,
            entrance = entrance,
            floor = floor,
            intercom = intercom,
            comment = comment,
            latitude = latitude,
            longitude = longitude,
        ).normalized()
    }

    companion object {
        fun fromDomain(address: DeliveryAddress?): DeliveryAddressEmbeddable? {
            if (address == null || address.isEmpty()) {
                return null
            }

            val normalized = address.normalized()
            return DeliveryAddressEmbeddable(
                country = normalized.country,
                region = normalized.region,
                city = normalized.city,
                street = normalized.street,
                house = normalized.house,
                apartment = normalized.apartment,
                postalCode = normalized.postalCode,
                entrance = normalized.entrance,
                floor = normalized.floor,
                intercom = normalized.intercom,
                comment = normalized.comment,
                latitude = normalized.latitude,
                longitude = normalized.longitude,
            )
        }
    }
}
