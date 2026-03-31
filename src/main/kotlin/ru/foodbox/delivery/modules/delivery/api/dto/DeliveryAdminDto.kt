package ru.foodbox.delivery.modules.delivery.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentMethodRule
import ru.foodbox.delivery.modules.delivery.application.AdminCheckoutPaymentRule
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.util.UUID

data class DeliveryMethodSettingResponse(
    val method: DeliveryMethodType,
    val name: String,
    val requiresAddress: Boolean,
    val requiresPickupPoint: Boolean,
    val isEnabled: Boolean,
    val sortOrder: Int,
)

data class UpsertDeliveryMethodSettingRequest(
    @field:NotNull
    val method: DeliveryMethodType,

    @field:NotNull
    val isEnabled: Boolean,

    @field:Min(0)
    val sortOrder: Int,
)

data class DeliveryZoneResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val city: String?,
    val postalCode: String?,
    val isActive: Boolean,
)

data class UpsertDeliveryZoneRequest(
    val id: UUID? = null,

    @field:NotBlank
    @field:Size(max = 64)
    val code: String,

    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:Size(max = 255)
    val city: String? = null,

    @field:Size(max = 64)
    val postalCode: String? = null,

    @field:NotNull
    val isActive: Boolean,
)

data class DeliveryTariffResponse(
    val id: UUID,
    val method: DeliveryMethodType,
    val zoneId: UUID?,
    val zoneCode: String?,
    val zoneName: String?,
    val isAvailable: Boolean,
    val fixedPriceMinor: Long,
    val freeFromAmountMinor: Long?,
    val currency: String,
    val estimatedDays: Int?,
)

data class UpsertDeliveryTariffRequest(
    val id: UUID? = null,

    @field:NotNull
    val method: DeliveryMethodType,

    val zoneId: UUID? = null,

    @field:NotNull
    val isAvailable: Boolean,

    @field:Min(0)
    val fixedPriceMinor: Long,

    @field:Min(0)
    val freeFromAmountMinor: Long? = null,

    @field:NotBlank
    @field:Size(min = 3, max = 3)
    val currency: String,

    @field:Min(0)
    val estimatedDays: Int? = null,
)

data class AdminPickupPointResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val address: DeliveryAddressResponse,
    val isActive: Boolean,
)

data class UpsertPickupPointRequest(
    val id: UUID? = null,

    @field:NotBlank
    @field:Size(max = 64)
    val code: String,

    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:Valid
    @field:NotNull
    val address: DeliveryAddressRequest,

    @field:NotNull
    val isActive: Boolean,
)

data class CheckoutPaymentRuleResponse(
    val deliveryMethod: DeliveryMethodType,
    val deliveryMethodName: String,
    val paymentMethods: List<PaymentMethodCode>,
    val isDynamic: Boolean,
)

data class ReplaceCheckoutPaymentRulesRequest(
    @field:Valid
    val rules: List<UpsertCheckoutPaymentRuleRequest>,
)

data class UpsertCheckoutPaymentRuleRequest(
    @field:NotNull
    val deliveryMethod: DeliveryMethodType,

    val paymentMethods: List<PaymentMethodCode> = emptyList(),
)

fun DeliveryMethodSetting.toResponse(): DeliveryMethodSettingResponse {
    return DeliveryMethodSettingResponse(
        method = method,
        name = method.displayName,
        requiresAddress = method.requiresAddress,
        requiresPickupPoint = method.requiresPickupPoint,
        isEnabled = enabled,
        sortOrder = sortOrder,
    )
}

fun UpsertDeliveryMethodSettingRequest.toDomain(): DeliveryMethodSetting {
    return DeliveryMethodSetting(
        method = method,
        enabled = isEnabled,
        sortOrder = sortOrder,
    )
}

fun DeliveryZone.toResponse(): DeliveryZoneResponse {
    return DeliveryZoneResponse(
        id = id,
        code = code,
        name = name,
        city = city,
        postalCode = postalCode,
        isActive = active,
    )
}

fun UpsertDeliveryZoneRequest.toDomain(): DeliveryZone {
    return DeliveryZone(
        id = id ?: UUID.randomUUID(),
        code = code,
        name = name,
        city = city,
        postalCode = postalCode,
        active = isActive,
    )
}

fun DeliveryTariff.toResponse(): DeliveryTariffResponse {
    return DeliveryTariffResponse(
        id = id,
        method = method,
        zoneId = zone?.id,
        zoneCode = zone?.code,
        zoneName = zone?.name,
        isAvailable = available,
        fixedPriceMinor = fixedPriceMinor,
        freeFromAmountMinor = freeFromAmountMinor,
        currency = currency,
        estimatedDays = estimatedDays,
    )
}

fun UpsertDeliveryTariffRequest.toDomain(zone: DeliveryZone?): DeliveryTariff {
    return DeliveryTariff(
        id = id ?: UUID.randomUUID(),
        method = method,
        zone = zone,
        available = isAvailable,
        fixedPriceMinor = fixedPriceMinor,
        freeFromAmountMinor = freeFromAmountMinor,
        currency = currency,
        estimatedDays = estimatedDays,
    )
}

fun PickupPoint.toAdminResponse(): AdminPickupPointResponse {
    return AdminPickupPointResponse(
        id = id,
        code = code,
        name = name,
        address = address.toResponse(),
        isActive = active,
    )
}

fun UpsertPickupPointRequest.toDomain(): PickupPoint {
    return PickupPoint(
        id = id ?: UUID.randomUUID(),
        code = code,
        name = name,
        address = address.toDomain(),
        active = isActive,
    )
}

fun ReplaceCheckoutPaymentRulesRequest.toDomain(): List<CheckoutPaymentMethodRule> {
    return rules.map { rule ->
        CheckoutPaymentMethodRule(
            deliveryMethod = rule.deliveryMethod,
            paymentMethods = rule.paymentMethods,
        )
    }
}

fun AdminCheckoutPaymentRule.toResponse(): CheckoutPaymentRuleResponse {
    return CheckoutPaymentRuleResponse(
        deliveryMethod = deliveryMethod,
        deliveryMethodName = deliveryMethod.displayName,
        paymentMethods = paymentMethods,
        isDynamic = dynamic,
    )
}
