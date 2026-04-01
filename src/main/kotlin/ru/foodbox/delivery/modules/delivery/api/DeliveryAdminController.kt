package ru.foodbox.delivery.modules.delivery.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.ResponseStatus
import ru.foodbox.delivery.modules.delivery.api.dto.AdminPickupPointResponse
import ru.foodbox.delivery.modules.delivery.api.dto.CheckoutPaymentRuleResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DetectPickupPointAddressRequest
import ru.foodbox.delivery.modules.delivery.api.dto.DetectPickupPointAddressResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryMethodSettingResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryTariffResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryZoneResponse
import ru.foodbox.delivery.modules.delivery.api.dto.ReplaceCheckoutPaymentRulesRequest
import ru.foodbox.delivery.modules.delivery.api.dto.UpsertDeliveryMethodSettingRequest
import ru.foodbox.delivery.modules.delivery.api.dto.UpsertDeliveryTariffRequest
import ru.foodbox.delivery.modules.delivery.api.dto.UpsertDeliveryZoneRequest
import ru.foodbox.delivery.modules.delivery.api.dto.UpsertPickupPointRequest
import ru.foodbox.delivery.modules.delivery.api.dto.toAdminResponse
import ru.foodbox.delivery.modules.delivery.api.dto.toDetectPickupPointAddressResponse
import ru.foodbox.delivery.modules.delivery.api.dto.toDomain
import ru.foodbox.delivery.modules.delivery.api.dto.toResponse
import ru.foodbox.delivery.modules.delivery.api.dto.withId
import ru.foodbox.delivery.modules.delivery.application.DeliveryAdminService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/delivery")
class DeliveryAdminController(
    private val deliveryAdminService: DeliveryAdminService,
) {

    @GetMapping("/methods")
    fun getMethodSettings(): List<DeliveryMethodSettingResponse> {
        return deliveryAdminService.getMethodSettings().map { it.toResponse() }
    }

    @PostMapping("/methods")
    fun upsertMethodSetting(
        @Valid @RequestBody request: UpsertDeliveryMethodSettingRequest,
    ): DeliveryMethodSettingResponse {
        return deliveryAdminService.upsertMethodSetting(request.toDomain()).toResponse()
    }

    @GetMapping("/zones")
    fun getZones(
        @RequestParam(name = "isActive", required = false) isActive: Boolean?,
    ): List<DeliveryZoneResponse> {
        return deliveryAdminService.getZones(isActive).map { it.toResponse() }
    }

    @GetMapping("/zones/{zoneId}")
    fun getZone(
        @PathVariable zoneId: UUID,
    ): DeliveryZoneResponse {
        return deliveryAdminService.getZone(zoneId).toResponse()
    }

    @PostMapping("/zones")
    fun upsertZone(
        @Valid @RequestBody request: UpsertDeliveryZoneRequest,
    ): DeliveryZoneResponse {
        return deliveryAdminService.upsertZone(request.toDomain()).toResponse()
    }

    @PutMapping("/zones/{zoneId}")
    fun updateZone(
        @PathVariable zoneId: UUID,
        @Valid @RequestBody request: UpsertDeliveryZoneRequest,
    ): DeliveryZoneResponse {
        require(request.id == null || request.id == zoneId) {
            "Delivery zone id in path and payload must match"
        }
        return deliveryAdminService.upsertZone(request.withId(zoneId).toDomain()).toResponse()
    }

    @DeleteMapping("/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteZone(
        @PathVariable zoneId: UUID,
    ) {
        deliveryAdminService.deleteZone(zoneId)
    }

    @GetMapping("/tariffs")
    fun getTariffs(): List<DeliveryTariffResponse> {
        return deliveryAdminService.getTariffs().map { it.toResponse() }
    }

    @PostMapping("/tariffs")
    fun upsertTariff(
        @Valid @RequestBody request: UpsertDeliveryTariffRequest,
    ): DeliveryTariffResponse {
        val zone = request.zoneId?.let { zoneId ->
            DeliveryZone(
                id = zoneId,
                code = "",
                name = "",
                type = DeliveryZoneType.CITY,
                city = null,
                normalizedCity = null,
                postalCode = null,
                geometry = null,
                priority = 0,
                active = true,
            )
        }

        return deliveryAdminService.upsertTariff(request.toDomain(zone)).toResponse()
    }

    @DeleteMapping("/tariffs/{tariffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTariff(
        @PathVariable tariffId: UUID,
    ) {
        deliveryAdminService.deleteTariff(tariffId)
    }

    @GetMapping("/pickup-points")
    fun getPickupPoints(
        @RequestParam(name = "isActive", required = false) isActive: Boolean?,
    ): List<AdminPickupPointResponse> {
        return deliveryAdminService.getPickupPoints(isActive).map { it.toAdminResponse() }
    }

    @PostMapping("/pickup-points/address-detect")
    fun detectPickupPointAddress(
        @Valid @RequestBody request: DetectPickupPointAddressRequest,
    ): DetectPickupPointAddressResponse {
        return deliveryAdminService.detectPickupPointAddress(
            latitude = request.latitude,
            longitude = request.longitude,
        ).toDetectPickupPointAddressResponse()
    }

    @PostMapping("/pickup-points")
    fun upsertPickupPoint(
        @Valid @RequestBody request: UpsertPickupPointRequest,
    ): AdminPickupPointResponse {
        return deliveryAdminService.upsertPickupPoint(request.toDomain()).toAdminResponse()
    }

    @DeleteMapping("/pickup-points/{pickupPointId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePickupPoint(
        @PathVariable pickupPointId: UUID,
    ) {
        deliveryAdminService.deletePickupPoint(pickupPointId)
    }

    @GetMapping("/payment-rules")
    fun getCheckoutPaymentRules(): List<CheckoutPaymentRuleResponse> {
        return deliveryAdminService.getCheckoutPaymentRules().map { it.toResponse() }
    }

    @PostMapping("/payment-rules/bulk")
    fun replaceCheckoutPaymentRules(
        @Valid @RequestBody request: ReplaceCheckoutPaymentRulesRequest,
    ): List<CheckoutPaymentRuleResponse> {
        return deliveryAdminService.replaceCheckoutPaymentRules(request.toDomain()).map { it.toResponse() }
    }
}
