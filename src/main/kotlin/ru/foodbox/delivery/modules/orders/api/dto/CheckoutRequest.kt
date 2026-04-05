package ru.foodbox.delivery.modules.orders.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryAddressRequest
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode

data class CheckoutRequest(
    @field:NotNull
    val paymentMethodCode: PaymentMethodCode,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerEmail: String? = null,
    @field:Valid
    val address: DeliveryAddressRequest? = null,
    val comment: String? = null,
)
