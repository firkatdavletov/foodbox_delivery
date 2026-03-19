package ru.foodbox.delivery.modules.delivery.application

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.embedded.DeliveryAddressEmbeddable
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryTariffEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryZoneEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.PickupPointEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryTariffJpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryZoneJpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.PickupPointJpaRepository
import java.time.Instant
import java.util.UUID

@Component
class DeliveryBootstrapInitializer(
    private val deliveryZoneJpaRepository: DeliveryZoneJpaRepository,
    private val deliveryTariffJpaRepository: DeliveryTariffJpaRepository,
    private val pickupPointJpaRepository: PickupPointJpaRepository,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        if (
            deliveryZoneJpaRepository.count() > 0L ||
            deliveryTariffJpaRepository.count() > 0L ||
            pickupPointJpaRepository.count() > 0L
        ) {
            return
        }

        val now = Instant.now()
        val ekbZone = DeliveryZoneEntity(
            id = UUID.randomUUID(),
            code = "EKB",
            name = "Yekaterinburg",
            city = "Yekaterinburg",
            normalizedCity = "yekaterinburg",
            postalCode = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        val mskZone = DeliveryZoneEntity(
            id = UUID.randomUUID(),
            code = "MSK",
            name = "Moscow",
            city = "Moscow",
            normalizedCity = "moscow",
            postalCode = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        deliveryZoneJpaRepository.saveAll(listOf(ekbZone, mskZone))

        deliveryTariffJpaRepository.saveAll(
            listOf(
                DeliveryTariffEntity(
                    id = UUID.randomUUID(),
                    method = DeliveryMethodType.COURIER,
                    zone = ekbZone,
                    isAvailable = true,
                    fixedPriceMinor = 29_900,
                    freeFromAmountMinor = 300_000,
                    currency = DEFAULT_CURRENCY,
                    estimatedDays = 1,
                    createdAt = now,
                    updatedAt = now,
                ),
                DeliveryTariffEntity(
                    id = UUID.randomUUID(),
                    method = DeliveryMethodType.COURIER,
                    zone = mskZone,
                    isAvailable = true,
                    fixedPriceMinor = 49_900,
                    freeFromAmountMinor = 500_000,
                    currency = DEFAULT_CURRENCY,
                    estimatedDays = 2,
                    createdAt = now,
                    updatedAt = now,
                ),
                DeliveryTariffEntity(
                    id = UUID.randomUUID(),
                    method = DeliveryMethodType.PICKUP,
                    zone = null,
                    isAvailable = true,
                    fixedPriceMinor = 0,
                    freeFromAmountMinor = null,
                    currency = DEFAULT_CURRENCY,
                    estimatedDays = 0,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        )

        pickupPointJpaRepository.save(
            PickupPointEntity(
                id = UUID.randomUUID(),
                code = "main-showroom",
                name = "Main Showroom",
                address = DeliveryAddressEmbeddable(
                    country = "Russia",
                    region = "Sverdlovsk Oblast",
                    city = "Yekaterinburg",
                    street = "Malysheva",
                    house = "51",
                    comment = "Pickup desk on the first floor",
                    latitude = 56.8389,
                    longitude = 60.6057,
                ),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    private companion object {
        private const val DEFAULT_CURRENCY = "RUB"
    }
}
