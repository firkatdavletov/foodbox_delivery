package ru.foodbox.delivery.controllers.web.checkout.body

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.services.dto.AddressDto

data class GuestCheckoutRequestBody(
    @field:NotEmpty(message = "Список товаров не должен быть пустым")
    @field:Valid
    val items: List<GuestCheckoutItemRequestBody> = emptyList(),

    @field:NotNull(message = "Данные покупателя обязательны")
    @field:Valid
    val customer: GuestCheckoutCustomerRequestBody? = null,

    @field:NotNull(message = "Данные доставки обязательны")
    @field:Valid
    val delivery: GuestCheckoutDeliveryRequestBody? = null,

    val comment: String? = null,
)

data class GuestCheckoutItemRequestBody(
    val productId: Long? = null,
    val sku: String? = null,

    @field:Min(value = 1, message = "Количество товара должно быть больше 0")
    val quantity: Int = 0,
)

data class GuestCheckoutCustomerRequestBody(
    @field:NotBlank(message = "Имя покупателя обязательно")
    val name: String? = null,

    @field:NotBlank(message = "Телефон обязателен")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{9,14}$",
        message = "Некорректный формат телефона"
    )
    val phone: String? = null,

    @field:Email(message = "Некорректный формат email")
    val email: String? = null,
)

data class GuestCheckoutDeliveryRequestBody(
    @field:NotNull(message = "Тип доставки обязателен")
    val type: DeliveryType? = null,
    @field:Valid
    val address: AddressDto? = null,
    val pickupPointId: Long? = null,
)
