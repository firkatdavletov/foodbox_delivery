package ru.foodbox.delivery.modules.delivery.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.delivery.api.dto.AdminPickupPointResponse
import ru.foodbox.delivery.modules.delivery.api.dto.CheckoutPaymentRuleResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryMethodSettingResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryTariffResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryZoneResponse
import ru.foodbox.delivery.modules.delivery.api.dto.ReplaceCheckoutPaymentRulesRequest
import ru.foodbox.delivery.modules.delivery.api.dto.UpsertDeliveryMethodSettingRequest
import ru.foodbox.delivery.modules.delivery.api.dto.UpsertDeliveryTariffRequest
import ru.foodbox.delivery.modules.delivery.api.dto.UpsertDeliveryZoneRequest
import ru.foodbox.delivery.modules.delivery.api.dto.UpsertPickupPointRequest
import ru.foodbox.delivery.modules.delivery.api.dto.toAdminResponse
import ru.foodbox.delivery.modules.delivery.api.dto.toDomain
import ru.foodbox.delivery.modules.delivery.api.dto.toResponse
import ru.foodbox.delivery.modules.delivery.application.DeliveryAdminService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone

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

    @PostMapping("/zones")
    fun upsertZone(
        @Valid @RequestBody request: UpsertDeliveryZoneRequest,
    ): DeliveryZoneResponse {
        return deliveryAdminService.upsertZone(request.toDomain()).toResponse()
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
                city = null,
                postalCode = null,
                active = true,
            )
        }

        return deliveryAdminService.upsertTariff(request.toDomain(zone)).toResponse()
    }

    @GetMapping("/pickup-points")
    fun getPickupPoints(
        @RequestParam(name = "isActive", required = false) isActive: Boolean?,
    ): List<AdminPickupPointResponse> {
        return deliveryAdminService.getPickupPoints(isActive).map { it.toAdminResponse() }
    }

    @PostMapping("/pickup-points")
    fun upsertPickupPoint(
        @Valid @RequestBody request: UpsertPickupPointRequest,
    ): AdminPickupPointResponse {
        return deliveryAdminService.upsertPickupPoint(request.toDomain()).toAdminResponse()
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
