package ru.foodbox.delivery.modules.delivery.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentMethodRule
import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentRuleDefaults
import ru.foodbox.delivery.modules.checkout.domain.repository.CheckoutPaymentMethodRuleRepository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryMethodSettingRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryTariffRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryZoneRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository
import java.util.Locale

@Service
class DeliveryAdminService(
    private val deliveryMethodSettingRepository: DeliveryMethodSettingRepository,
    private val deliveryZoneRepository: DeliveryZoneRepository,
    private val deliveryTariffRepository: DeliveryTariffRepository,
    private val pickupPointRepository: PickupPointRepository,
    private val checkoutPaymentMethodRuleRepository: CheckoutPaymentMethodRuleRepository,
    private val deliveryAddressGeocoder: DeliveryAddressGeocoder,
) {

    fun getMethodSettings(): List<DeliveryMethodSetting> {
        val settingsByMethod = deliveryMethodSettingRepository.findAll().associateBy(DeliveryMethodSetting::method)
        return DeliveryMethodType.entries
            .map { method ->
                settingsByMethod[method] ?: DeliveryMethodSetting(
                    method = method,
                    enabled = method.isActive,
                    sortOrder = method.ordinal,
                )
            }
            .sortedWith(compareBy<DeliveryMethodSetting> { it.sortOrder }.thenBy { it.method.ordinal })
    }

    @Transactional
    fun upsertMethodSetting(setting: DeliveryMethodSetting): DeliveryMethodSetting {
        require(setting.sortOrder >= 0) { "sortOrder must be non-negative" }
        return deliveryMethodSettingRepository.save(setting)
    }

    fun getZones(isActive: Boolean?): List<DeliveryZone> {
        return when (isActive) {
            null -> deliveryZoneRepository.findAll()
            else -> deliveryZoneRepository.findAllByIsActive(isActive)
        }
    }

    fun getZone(zoneId: java.util.UUID): DeliveryZone {
        return deliveryZoneRepository.findById(zoneId)
            ?: throw NotFoundException("Delivery zone not found: $zoneId")
    }

    @Transactional
    fun deleteZone(zoneId: java.util.UUID) {
        deliveryZoneRepository.findById(zoneId)
            ?: throw NotFoundException("Delivery zone not found: $zoneId")
        require(!deliveryTariffRepository.existsByZoneId(zoneId)) {
            "Cannot delete delivery zone while tariffs are linked to it"
        }
        deliveryZoneRepository.deleteById(zoneId)
    }

    @Transactional
    fun upsertZone(zone: DeliveryZone): DeliveryZone {
        val code = zone.code.trim().takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
            ?: throw IllegalArgumentException("Delivery zone code is required")
        val name = zone.name.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Delivery zone name is required")
        val city = zone.city?.trim()?.takeIf { it.isNotBlank() }
        val postalCode = zone.postalCode?.trim()?.takeIf { it.isNotBlank() }
        require(zone.priority >= 0) { "priority must be non-negative" }

        when (zone.type) {
            DeliveryZoneType.CITY -> require(city != null) {
                "city is required for CITY delivery zone"
            }
            DeliveryZoneType.POSTAL_CODE -> require(postalCode != null) {
                "postalCode is required for POSTAL_CODE delivery zone"
            }
            DeliveryZoneType.POLYGON -> require(zone.geometry != null) {
                "geometry is required for POLYGON delivery zone"
            }
        }

        require(zone.type == DeliveryZoneType.POLYGON || zone.geometry == null) {
            "geometry is supported only for POLYGON delivery zones"
        }

        deliveryZoneRepository.findByCode(code)?.let { duplicate ->
            require(duplicate.id == zone.id) { "Delivery zone code '$code' is already used" }
        }

        return deliveryZoneRepository.save(
            DeliveryZone(
                id = zone.id,
                code = code,
                name = name,
                type = zone.type,
                city = city,
                normalizedCity = null,
                postalCode = postalCode,
                geometry = zone.geometry,
                priority = zone.priority,
                active = zone.active,
            )
        )
    }

    fun getTariffs(): List<DeliveryTariff> = deliveryTariffRepository.findAll()

    @Transactional
    fun deleteTariff(tariffId: java.util.UUID) {
        deliveryTariffRepository.findById(tariffId)
            ?: throw NotFoundException("Delivery tariff not found: $tariffId")
        deliveryTariffRepository.deleteById(tariffId)
    }

    @Transactional
    fun upsertTariff(tariff: DeliveryTariff): DeliveryTariff {
        require(tariff.fixedPriceMinor >= 0L) { "fixedPriceMinor must be non-negative" }
        require(tariff.freeFromAmountMinor == null || tariff.freeFromAmountMinor >= 0L) {
            "freeFromAmountMinor must be non-negative"
        }
        require(tariff.estimatedDays == null || tariff.estimatedDays >= 0) {
            "estimatedDays must be non-negative"
        }

        val currency = tariff.currency.trim().uppercase(Locale.ROOT)
        require(currency.length == 3) { "currency must contain 3 letters" }

        when (tariff.method) {
            DeliveryMethodType.COURIER -> require(tariff.zone != null) {
                "Courier tariff must be linked to a delivery zone"
            }
            DeliveryMethodType.PICKUP -> require(tariff.zone == null) {
                "Pickup tariff must not be linked to a delivery zone"
            }
            DeliveryMethodType.YANDEX_PICKUP_POINT -> throw IllegalArgumentException(
                "Tariffs are not supported for Yandex pickup point delivery"
            )
        }

        val zone = tariff.zone?.let { zoneRef ->
            deliveryZoneRepository.findById(zoneRef.id)
                ?: throw IllegalArgumentException("Delivery zone not found: ${zoneRef.id}")
        }

        val duplicate = when (tariff.method) {
            DeliveryMethodType.COURIER -> deliveryTariffRepository.findByMethodAndZone(
                method = tariff.method,
                zoneId = zone?.id,
            )
            DeliveryMethodType.PICKUP -> deliveryTariffRepository.findDefaultByMethod(tariff.method)
            DeliveryMethodType.YANDEX_PICKUP_POINT -> null
        }
        duplicate?.let { existing ->
            require(existing.id == tariff.id) {
                "Tariff for ${tariff.method.name} and selected zone is already configured"
            }
        }

        return deliveryTariffRepository.save(
            DeliveryTariff(
                id = tariff.id,
                method = tariff.method,
                zone = zone,
                available = tariff.available,
                fixedPriceMinor = tariff.fixedPriceMinor,
                freeFromAmountMinor = tariff.freeFromAmountMinor,
                currency = currency,
                estimatedDays = tariff.estimatedDays,
            )
        )
    }

    fun getPickupPoints(isActive: Boolean?): List<PickupPoint> {
        return when (isActive) {
            null -> pickupPointRepository.findAll()
            else -> pickupPointRepository.findAllByIsActive(isActive)
        }
    }

    @Transactional
    fun deletePickupPoint(pickupPointId: java.util.UUID) {
        pickupPointRepository.findById(pickupPointId)
            ?: throw NotFoundException("Pickup point not found: $pickupPointId")
        pickupPointRepository.deleteById(pickupPointId)
    }

    fun detectPickupPointAddress(latitude: Double, longitude: Double): DeliveryAddress? {
        return deliveryAddressGeocoder.reverseGeocode(
            latitude = latitude,
            longitude = longitude,
        )
    }

    @Transactional
    fun upsertPickupPoint(point: PickupPoint): PickupPoint {
        val code = point.code.trim().takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
            ?: throw IllegalArgumentException("Pickup point code is required")
        val name = point.name.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Pickup point name is required")
        require(!point.address.isEmpty()) { "Pickup point address must not be empty" }

        pickupPointRepository.findByCode(code)?.let { duplicate ->
            require(duplicate.id == point.id) { "Pickup point code '$code' is already used" }
        }

        return pickupPointRepository.save(
            PickupPoint(
                id = point.id,
                code = code,
                name = name,
                address = point.address.normalized(),
                active = point.active,
            )
        )
    }

    fun getCheckoutPaymentRules(): List<AdminCheckoutPaymentRule> {
        val rulesByDeliveryMethod = checkoutPaymentMethodRuleRepository.findAll().associateBy(CheckoutPaymentMethodRule::deliveryMethod)

        return DeliveryMethodType.entries
            .sortedBy { it.ordinal }
            .map { method ->
                AdminCheckoutPaymentRule(
                    deliveryMethod = method,
                    paymentMethods = if (method in CheckoutPaymentRuleDefaults.dynamicDeliveryMethods) {
                        emptyList()
                    } else {
                        rulesByDeliveryMethod[method]?.paymentMethods.orEmpty()
                    },
                    dynamic = method in CheckoutPaymentRuleDefaults.dynamicDeliveryMethods,
                )
            }
    }

    @Transactional
    fun replaceCheckoutPaymentRules(rules: List<CheckoutPaymentMethodRule>): List<AdminCheckoutPaymentRule> {
        val duplicateMethods = rules
            .groupBy(CheckoutPaymentMethodRule::deliveryMethod)
            .filterValues { it.size > 1 }
            .keys
        require(duplicateMethods.isEmpty()) {
            "Duplicate delivery methods in checkout payment rules: ${duplicateMethods.joinToString()}"
        }

        rules.forEach { rule ->
            require(rule.deliveryMethod !in CheckoutPaymentRuleDefaults.dynamicDeliveryMethods) {
                "Checkout payment rules are dynamic for ${rule.deliveryMethod.name}"
            }
            val duplicatePaymentMethods = rule.paymentMethods
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
            require(duplicatePaymentMethods.isEmpty()) {
                "Duplicate payment methods for ${rule.deliveryMethod.name}: ${duplicatePaymentMethods.joinToString()}"
            }
        }

        checkoutPaymentMethodRuleRepository.replaceAll(
            rules.sortedBy { it.deliveryMethod.ordinal }
        )
        return getCheckoutPaymentRules()
    }
}

data class AdminCheckoutPaymentRule(
    val deliveryMethod: DeliveryMethodType,
    val paymentMethods: List<ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode>,
    val dynamic: Boolean,
)
