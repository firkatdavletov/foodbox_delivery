package ru.foodbox.delivery.modules.orders.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryAddressRequest
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.util.UUID

data class GuestCheckoutRequest(
    @field:NotEmpty
    @field:Valid
    val items: List<GuestCheckoutItemRequest>,

    @field:NotBlank
    val customerName: String,

    @field:NotBlank
    val customerPhone: String,

    val customerEmail: String? = null,

    @field:NotNull
    val paymentMethodCode: PaymentMethodCode,

    @field:NotNull
    val deliveryMethod: DeliveryMethodType,

    @field:Valid
    val address: DeliveryAddressRequest? = null,

    val pickupPointId: UUID? = null,
    val pickupPointExternalId: String? = null,
    val comment: String? = null,
    val promoCode: String? = null,
    val giftCertificateCode: String? = null,
)

data class GuestCheckoutItemRequest(
    @field:NotNull
    val productId: UUID,

    val variantId: UUID? = null,

    @field:Min(1)
    val quantity: Int,
)
