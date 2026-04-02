package ru.foodbox.delivery.modules.delivery.application

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentRuleDefaults
import ru.foodbox.delivery.modules.checkout.domain.repository.CheckoutPaymentMethodRuleRepository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryMethodSettingRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryTariffRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryZoneRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository
import java.util.UUID

@Component
class DeliveryBootstrapInitializer(
    private val deliveryMethodSettingRepository: DeliveryMethodSettingRepository,
    private val deliveryZoneRepository: DeliveryZoneRepository,
    private val deliveryTariffRepository: DeliveryTariffRepository,
    private val pickupPointRepository: PickupPointRepository,
    private val checkoutPaymentMethodRuleRepository: CheckoutPaymentMethodRuleRepository,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        seedMethodSettings()
        val zonesByCode = seedZones()
        seedTariffs(zonesByCode)
        seedPickupPoints()
        seedCheckoutPaymentRules()
    }

    private fun seedMethodSettings() {
        if (deliveryMethodSettingRepository.findAll().isNotEmpty()) {
            return
        }

        DeliveryMethodType.entries.forEach { method ->
            deliveryMethodSettingRepository.save(
                DeliveryMethodSetting(
                    method = method,
                    enabled = method.isActive,
                    sortOrder = method.ordinal,
                )
            )
        }
    }

    private fun seedZones(): Map<String, DeliveryZone> {
        val existing = deliveryZoneRepository.findAll()
        if (existing.isNotEmpty()) {
            return existing.associateBy(DeliveryZone::code)
        }

        val ekbZone = deliveryZoneRepository.save(
            DeliveryZone(
                id = UUID.randomUUID(),
                code = "EKB",
                name = "Yekaterinburg",
                type = DeliveryZoneType.CITY,
                city = "Yekaterinburg",
                normalizedCity = null,
                postalCode = null,
                geometry = null,
                priority = 0,
                active = true,
            )
        )
        val mskZone = deliveryZoneRepository.save(
            DeliveryZone(
                id = UUID.randomUUID(),
                code = "MSK",
                name = "Moscow",
                type = DeliveryZoneType.CITY,
                city = "Moscow",
                normalizedCity = null,
                postalCode = null,
                geometry = null,
                priority = 0,
                active = true,
            )
        )
        return listOf(ekbZone, mskZone).associateBy(DeliveryZone::code)
    }

    private fun seedTariffs(zonesByCode: Map<String, DeliveryZone>) {
        if (deliveryTariffRepository.findAll().isNotEmpty()) {
            return
        }

        deliveryTariffRepository.save(
            DeliveryTariff(
                id = UUID.randomUUID(),
                method = DeliveryMethodType.COURIER,
                zone = zonesByCode.getValue("EKB"),
                available = true,
                fixedPriceMinor = 29_900,
                freeFromAmountMinor = 300_000,
                currency = DEFAULT_CURRENCY,
                estimatedDays = 1,
                deliveryMinutes = 24 * 60,
            )
        )
        deliveryTariffRepository.save(
            DeliveryTariff(
                id = UUID.randomUUID(),
                method = DeliveryMethodType.COURIER,
                zone = zonesByCode.getValue("MSK"),
                available = true,
                fixedPriceMinor = 49_900,
                freeFromAmountMinor = 500_000,
                currency = DEFAULT_CURRENCY,
                estimatedDays = 2,
                deliveryMinutes = 48 * 60,
            )
        )
        deliveryTariffRepository.save(
            DeliveryTariff(
                id = UUID.randomUUID(),
                method = DeliveryMethodType.PICKUP,
                zone = null,
                available = true,
                fixedPriceMinor = 0,
                freeFromAmountMinor = null,
                currency = DEFAULT_CURRENCY,
                estimatedDays = 0,
            )
        )
    }

    private fun seedPickupPoints() {
        if (pickupPointRepository.findAll().isNotEmpty()) {
            return
        }

        pickupPointRepository.save(
            PickupPoint(
                id = UUID.randomUUID(),
                code = "main-showroom",
                name = "Main Showroom",
                address = DeliveryAddress(
                    country = "Russia",
                    region = "Sverdlovsk Oblast",
                    city = "Yekaterinburg",
                    street = "Malysheva",
                    house = "51",
                    comment = "Pickup desk on the first floor",
                    latitude = 56.8389,
                    longitude = 60.6057,
                ),
                active = true,
            )
        )
    }

    private fun seedCheckoutPaymentRules() {
        if (checkoutPaymentMethodRuleRepository.findAll().isNotEmpty()) {
            return
        }

        checkoutPaymentMethodRuleRepository.replaceAll(CheckoutPaymentRuleDefaults.defaultRules())
    }

    private companion object {
        private const val DEFAULT_CURRENCY = "RUB"
    }
}
