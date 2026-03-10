package ru.foodbox.delivery.modules.orders.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.orders.domain.OrderDeliveryType
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
    val deliveryType: OrderDeliveryType,

    val deliveryAddress: String? = null,
    val comment: String? = null,
)

data class GuestCheckoutItemRequest(
    @field:NotNull
    val productId: UUID,

    @field:Min(1)
    val quantity: Int,
)
